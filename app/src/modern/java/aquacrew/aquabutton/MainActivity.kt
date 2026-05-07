package aquacrew.aquabutton

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val AQUA_VOICE_BASE_URL =
    "https://raw.githubusercontent.com/zyzsdy/aqua-button/master/public/voices/"
private const val MEA_VOICE_BASE_URL =
    "https://raw.githubusercontent.com/zyzsdy/meamea-button/master/public/voices/"

data class ButtonPack(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val logoResId: Int,
    val categories: List<ButtonCategory>,
    val isBuiltIn: Boolean = false
) {
    val itemCount: Int
        get() = categories.sumOf { it.items.size }
}

data class ButtonCategory(
    val id: String,
    val title: String,
    val items: List<ButtonItem>
)

data class ButtonItem(
    val id: String,
    val packId: String,
    val categoryId: String,
    val title: String,
    val mediaType: String,
    val assetPath: String?,
    val remoteUrl: String?,
    val localPath: String?
)

@Entity(tableName = "packs")
data class PackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val description: String,
    val logoResName: String,
    val isBuiltIn: Boolean,
    val sortOrder: Int
)

@Entity(
    tableName = "categories",
    indices = [Index("packId")]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val title: String,
    val sortOrder: Int
)

@Entity(
    tableName = "button_items",
    indices = [Index("packId"), Index("categoryId")]
)
data class ButtonItemEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val categoryId: String,
    val title: String,
    val mediaType: String,
    val assetPath: String?,
    val remoteUrl: String?,
    val localPath: String?,
    val sortOrder: Int
)

@Entity(
    tableName = "trigger_phrases",
    primaryKeys = ["itemId", "phrase"],
    indices = [Index("itemId")]
)
data class TriggerPhraseEntity(
    val itemId: String,
    val phrase: String
)

@Dao
interface ButtonPackDao {
    @Query("SELECT * FROM packs ORDER BY sortOrder ASC, name ASC")
    suspend fun getPacks(): List<PackEntity>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, title ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM button_items ORDER BY sortOrder ASC, title ASC")
    suspend fun getItems(): List<ButtonItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacks(packs: List<PackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ButtonItemEntity>)

    @Query("DELETE FROM trigger_phrases WHERE itemId IN (SELECT id FROM button_items WHERE packId IN (SELECT id FROM packs WHERE isBuiltIn = 1))")
    suspend fun deleteBuiltInTriggerPhrases()

    @Query("DELETE FROM button_items WHERE packId IN (SELECT id FROM packs WHERE isBuiltIn = 1)")
    suspend fun deleteBuiltInItems()

    @Query("DELETE FROM categories WHERE packId IN (SELECT id FROM packs WHERE isBuiltIn = 1)")
    suspend fun deleteBuiltInCategories()

    @Query("DELETE FROM packs WHERE isBuiltIn = 1")
    suspend fun deleteBuiltInPacks()
}

@Database(
    entities = [
        PackEntity::class,
        CategoryEntity::class,
        ButtonItemEntity::class,
        TriggerPhraseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AquaDatabase : RoomDatabase() {
    abstract fun buttonPackDao(): ButtonPackDao

    companion object {
        @Volatile
        private var instance: AquaDatabase? = null

        fun get(context: Context): AquaDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AquaDatabase::class.java,
                    "aqua_button.db"
                ).build().also { instance = it }
            }
        }
    }
}

class ButtonPackRepository(
    private val context: Context,
    private val database: AquaDatabase = AquaDatabase.get(context)
) {
    suspend fun loadPacks(): List<ButtonPack> {
        val builtInPacks = loadBuiltInPacks()
        database.withTransaction {
            val dao = database.buttonPackDao()
            dao.deleteBuiltInTriggerPhrases()
            dao.deleteBuiltInItems()
            dao.deleteBuiltInCategories()
            dao.deleteBuiltInPacks()
            dao.insertPacks(builtInPacks.mapIndexed { index, pack -> pack.toEntity(index) })
            dao.insertCategories(
                builtInPacks.flatMap { pack ->
                    pack.categories.mapIndexed { index, category ->
                        category.toEntity(pack.id, index)
                    }
                }
            )
            dao.insertItems(
                builtInPacks.flatMap { pack ->
                    pack.categories.flatMap { category ->
                        category.items.mapIndexed { index, item ->
                            item.toEntity(index)
                        }
                    }
                }
            )
        }
        return readPacksFromDatabase()
    }

    private fun loadBuiltInPacks(): List<ButtonPack> {
        val assets = context.assets
        val aquaJson = assets.open("voices.json").bufferedReader().use { it.readText() }
        val meaJson = assets.open("mea_voices.json").bufferedReader().use { it.readText() }
        return listOf(
            aquaJson.parseButtonPack(
                packId = "aqua",
                packName = "AquaButton",
                author = "MinatoAquaCrew",
                description = "Bundled Minato Aqua voice buttons.",
                logoResId = R.drawable.main_logo,
                assetPrefix = "voices",
                remoteBaseUrl = AQUA_VOICE_BASE_URL,
                isBuiltIn = true
            ),
            meaJson.parseButtonPack(
                packId = "mea",
                packName = "MeaButton",
                author = "zyzsdy/meamea-button",
                description = "Bundled Kagura Mea voice buttons.",
                logoResId = R.drawable.main_logo,
                assetPrefix = "mea_voices",
                remoteBaseUrl = MEA_VOICE_BASE_URL,
                isBuiltIn = true
            )
        )
    }

    private suspend fun readPacksFromDatabase(): List<ButtonPack> {
        val dao = database.buttonPackDao()
        val packs = dao.getPacks()
        val categoriesByPack = dao.getCategories().groupBy { it.packId }
        val itemsByCategory = dao.getItems().groupBy { it.categoryId }
        return packs.map { pack ->
            ButtonPack(
                id = pack.id,
                name = pack.name,
                author = pack.author,
                description = pack.description,
                logoResId = resolveDrawable(pack.logoResName),
                isBuiltIn = pack.isBuiltIn,
                categories = categoriesByPack[pack.id].orEmpty().map { category ->
                    ButtonCategory(
                        id = category.id,
                        title = category.title,
                        items = itemsByCategory[category.id].orEmpty().map { it.toButtonItem() }
                    )
                }
            )
        }
    }

    private fun resolveDrawable(name: String): Int {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return if (id == 0) R.drawable.main_logo else id
    }
}

data class AquaUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val packs: List<ButtonPack> = emptyList(),
    val selectedPackId: String? = null,
    val selectedCategoryId: String? = null,
    val query: String = "",
    val playing: ButtonItem? = null
) {
    val selectedPack: ButtonPack?
        get() = packs.firstOrNull { it.id == selectedPackId } ?: packs.firstOrNull()

    val selectedCategory: ButtonCategory?
        get() = selectedPack?.categories?.firstOrNull { it.id == selectedCategoryId }
            ?: selectedPack?.categories?.firstOrNull()

    val shownItems: List<ButtonItem>
        get() {
            val pack = selectedPack ?: return emptyList()
            val source = if (query.isBlank()) {
                selectedCategory?.items.orEmpty()
            } else {
                pack.categories.flatMap { it.items }
            }
            val keyword = query.trim()
            return if (keyword.isEmpty()) {
                source
            } else {
                source.filter {
                    it.title.contains(keyword, ignoreCase = true) ||
                        it.id.contains(keyword, ignoreCase = true)
                }
            }
        }
}

class AquaViewModel : ViewModel() {
    var state by mutableStateOf(AquaUiState())
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var assetManager: AssetManager? = null

    fun load(activity: ComponentActivity) {
        assetManager = activity.assets
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ButtonPackRepository(activity.applicationContext).loadPacks()
                }
            }.onSuccess { packs ->
                state = state.copy(
                    loading = false,
                    packs = packs,
                    selectedPackId = packs.firstOrNull()?.id,
                    selectedCategoryId = packs.firstOrNull()?.categories?.firstOrNull()?.id,
                    error = null
                )
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    error = error.message ?: "Failed to load button packs"
                )
            }
        }
    }

    fun selectPack(pack: ButtonPack) {
        if (pack.id == state.selectedPackId) return
        stop()
        state = state.copy(
            selectedPackId = pack.id,
            selectedCategoryId = pack.categories.firstOrNull()?.id,
            query = ""
        )
    }

    fun selectCategory(category: ButtonCategory) {
        state = state.copy(selectedCategoryId = category.id, query = "")
    }

    fun updateQuery(query: String) {
        state = state.copy(query = query)
    }

    fun play(item: ButtonItem) {
        stop()
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                stop()
            }
            setOnErrorListener { _, _, _ ->
                stop()
                true
            }
        }
        mediaPlayer = player
        state = state.copy(playing = item)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val assetFile = item.assetPath?.let { assetPath ->
                    assetManager?.runCatching { openFd(assetPath) }?.getOrNull()
                }
                if (assetFile != null) {
                    assetFile.use {
                        player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                } else if (item.localPath != null) {
                    player.setDataSource(item.localPath)
                } else {
                    player.setDataSource(requireNotNull(item.remoteUrl))
                }
                player.prepare()
                player.start()
            }.onFailure {
                stop()
            }
        }
    }

    fun playRandom() {
        val items = state.selectedPack?.categories?.flatMap { it.items }.orEmpty()
        if (items.isNotEmpty()) play(items.random())
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        state = state.copy(playing = null)
    }

    override fun onCleared() {
        stop()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AquaButtonApp(this)
        }
    }
}

@Composable
fun AquaButtonApp(activity: ComponentActivity, viewModel: AquaViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.load(activity)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF6C2BFF),
            secondary = Color(0xFF00B8C8),
            surface = Color(0xFFFBFAFF),
            background = Color(0xFFF7F4FF)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AquaHome(
                state = viewModel.state,
                onPackClick = viewModel::selectPack,
                onCategoryClick = viewModel::selectCategory,
                onQueryChange = viewModel::updateQuery,
                onItemClick = viewModel::play,
                onRandomClick = viewModel::playRandom,
                onStopClick = viewModel::stop,
                onRetryClick = { viewModel.load(activity) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AquaHome(
    state: AquaUiState,
    onPackClick: (ButtonPack) -> Unit,
    onCategoryClick: (ButtonCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onItemClick: (ButtonItem) -> Unit,
    onRandomClick: () -> Unit,
    onStopClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(state.selectedPack?.logoResId ?: R.drawable.main_logo),
                        contentDescription = state.selectedPack?.name ?: "AquaButton",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(30.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (state.playing == null) onRandomClick else onStopClick,
                containerColor = if (state.playing == null) Color(0xFF00D6C9) else Color(0xFFFF5F6D),
                contentColor = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(
                    imageVector = if (state.playing == null) Icons.Filled.Casino else Icons.Filled.Stop,
                    contentDescription = if (state.playing == null) "Shuffle" else "Stop"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF0E9FF), Color(0xFFFFFFFF))
                    )
                )
                .padding(padding)
        ) {
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorView(state.error, onRetryClick)
                else -> ButtonPackBrowser(
                    state = state,
                    onPackClick = onPackClick,
                    onCategoryClick = onCategoryClick,
                    onQueryChange = onQueryChange,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun ButtonPackBrowser(
    state: AquaUiState,
    onPackClick: (ButtonPack) -> Unit,
    onCategoryClick: (ButtonCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onItemClick: (ButtonItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        PackSwitcher(
            packs = state.packs,
            selectedPack = state.selectedPack,
            onPackClick = onPackClick
        )
        SearchBox(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        CategorySwitcher(
            categories = state.selectedPack?.categories.orEmpty(),
            selectedCategory = state.selectedCategory,
            onCategoryClick = onCategoryClick
        )
        PackSummary(state)
        if (state.shownItems.isEmpty()) {
            EmptyPackView(state.selectedPack, state.query)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.shownItems, key = { "${it.packId}:${it.id}" }) { item ->
                    ButtonItemCard(
                        item = item,
                        isPlaying = state.playing?.packId == item.packId && state.playing.id == item.id,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackSwitcher(
    packs: List<ButtonPack>,
    selectedPack: ButtonPack?,
    onPackClick: (ButtonPack) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(packs, key = { it.id }) { pack ->
            AssistChip(
                onClick = { onPackClick(pack) },
                label = { Text(pack.name) },
                leadingIcon = if (pack.id == selectedPack?.id) {
                    { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6C2BFF))) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun CategorySwitcher(
    categories: List<ButtonCategory>,
    selectedCategory: ButtonCategory?,
    onCategoryClick: (ButtonCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            AssistChip(
                onClick = { onCategoryClick(category) },
                label = { Text(category.title) },
                leadingIcon = if (category.id == selectedCategory?.id) {
                    { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00B8C8))) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun PackSummary(state: AquaUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (state.query.isBlank()) {
                    state.selectedCategory?.title ?: state.selectedPack?.name.orEmpty()
                } else {
                    "Search results"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${state.selectedPack?.itemCount ?: 0} total / ${state.shownItems.size} shown",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF625B71)
            )
        }
        state.playing?.let {
            Text(
                text = "Playing: ${it.title}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF6C2BFF),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmptyPackView(pack: ButtonPack?, query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (query.isBlank()) "No buttons yet" else "No matches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = pack?.description ?: "This pack is ready for future import and editing work.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A7286)
            )
        }
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        placeholder = { Text("Search buttons") },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ButtonItemCard(item: ButtonItem, isPlaying: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFFE9F9F8) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color(0xFF00B8C8) else Color(0xFFEDE7F6))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = if (isPlaying) Color.White else Color(0xFF6C2BFF)
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A7286)
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading button packs")
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetryClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Failed to load packs", style = MaterialTheme.typography.titleMedium)
            Text(message, color = Color(0xFF7A7286))
            Button(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

private fun String.parseButtonPack(
    packId: String,
    packName: String,
    author: String,
    description: String,
    logoResId: Int,
    assetPrefix: String,
    remoteBaseUrl: String,
    isBuiltIn: Boolean
): ButtonPack {
    val categories = JsonParser.parseString(this).asJsonObject
        .getAsJsonArray("voices")
        .map { categoryJson ->
            val category = categoryJson.asJsonObject
            val categoryId = category.get("categoryName").asString
            ButtonCategory(
                id = categoryId,
                title = category.getAsJsonObject("categoryDescription").bestText(),
                items = category.getAsJsonArray("voiceList").map { itemJson ->
                    val item = itemJson.asJsonObject
                    val itemId = item.get("name").asString
                    val path = item.get("path").asString
                    ButtonItem(
                        id = itemId,
                        packId = packId,
                        categoryId = categoryId,
                        title = item.getAsJsonObject("description").bestText(),
                        mediaType = "audio",
                        assetPath = "$assetPrefix/$path",
                        remoteUrl = remoteBaseUrl + path,
                        localPath = null
                    )
                }
            )
        }
    return ButtonPack(
        id = packId,
        name = packName,
        author = author,
        description = description,
        logoResId = logoResId,
        categories = categories,
        isBuiltIn = isBuiltIn
    )
}

private fun ButtonPack.toEntity(sortOrder: Int): PackEntity {
    return PackEntity(
        id = id,
        name = name,
        author = author,
        description = description,
        logoResName = "main_logo",
        isBuiltIn = isBuiltIn,
        sortOrder = sortOrder
    )
}

private fun ButtonCategory.toEntity(packId: String, sortOrder: Int): CategoryEntity {
    return CategoryEntity(
        id = "$packId:$id",
        packId = packId,
        title = title,
        sortOrder = sortOrder
    )
}

private fun ButtonItem.toEntity(sortOrder: Int): ButtonItemEntity {
    return ButtonItemEntity(
        id = "$packId:$id",
        packId = packId,
        categoryId = "$packId:$categoryId",
        title = title,
        mediaType = mediaType,
        assetPath = assetPath,
        remoteUrl = remoteUrl,
        localPath = localPath,
        sortOrder = sortOrder
    )
}

private fun ButtonItemEntity.toButtonItem(): ButtonItem {
    return ButtonItem(
        id = id.substringAfter(':', id),
        packId = packId,
        categoryId = categoryId.substringAfter(':', categoryId),
        title = title,
        mediaType = mediaType,
        assetPath = assetPath,
        remoteUrl = remoteUrl,
        localPath = localPath
    )
}

private fun JsonObject.bestText(): String {
    val languageTag = Locale.getDefault().toLanguageTag()
    val language = Locale.getDefault().language
    return get(languageTag)?.asString
        ?: entrySet().firstOrNull { it.key.equals(language, ignoreCase = true) }?.value?.asString
        ?: get("zh-CN")?.asString
        ?: get("ja-JP")?.asString
        ?: get("en")?.asString
        ?: entrySet().firstOrNull()?.value?.asString
        ?: ""
}
