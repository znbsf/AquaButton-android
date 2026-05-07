package aquacrew.aquabutton

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val VOICE_BASE_URL =
    "https://raw.githubusercontent.com/zyzsdy/aqua-button/master/public/voices/"

data class VoiceCategory(
    val name: String,
    val description: String,
    val voices: List<VoiceItem>
)

data class VoiceItem(
    val name: String,
    val path: String,
    val description: String
)

data class AquaUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<VoiceCategory> = emptyList(),
    val selectedCategory: String? = null,
    val query: String = "",
    val playing: VoiceItem? = null
) {
    val selected: VoiceCategory?
        get() = categories.firstOrNull { it.name == selectedCategory } ?: categories.firstOrNull()

    val shownVoices: List<VoiceItem>
        get() {
            val source = if (query.isBlank()) {
                selected?.voices.orEmpty()
            } else {
                categories.flatMap { it.voices }
            }
            val keyword = query.trim()
            return if (keyword.isEmpty()) {
                source
            } else {
                source.filter {
                    it.description.contains(keyword, ignoreCase = true) ||
                        it.name.contains(keyword, ignoreCase = true)
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
                    activity.assets.open("voices.json").bufferedReader().use { it.readText() }
                }.parseVoiceCategories()
            }.onSuccess { categories ->
                state = state.copy(
                    loading = false,
                    categories = categories,
                    selectedCategory = categories.firstOrNull()?.name,
                    error = null
                )
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    error = error.message ?: "Failed to load voices"
                )
            }
        }
    }

    fun selectCategory(category: VoiceCategory) {
        state = state.copy(selectedCategory = category.name, query = "")
    }

    fun updateQuery(query: String) {
        state = state.copy(query = query)
    }

    fun play(voice: VoiceItem) {
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
        state = state.copy(playing = voice)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val assetFile = assetManager?.runCatching {
                    openFd("voices/${voice.path}")
                }?.getOrNull()
                if (assetFile != null) {
                    assetFile.use {
                        player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                } else {
                    player.setDataSource(VOICE_BASE_URL + voice.path)
                }
                player.prepare()
                player.start()
            }.onFailure {
                stop()
            }
        }
    }

    fun playRandom() {
        val voices = state.categories.flatMap { it.voices }
        if (voices.isNotEmpty()) play(voices.random())
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
                onCategoryClick = viewModel::selectCategory,
                onQueryChange = viewModel::updateQuery,
                onVoiceClick = viewModel::play,
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
    onCategoryClick: (VoiceCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onVoiceClick: (VoiceItem) -> Unit,
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
                        painter = painterResource(R.drawable.main_logo),
                        contentDescription = "AquaButton",
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
                else -> VoiceBrowser(
                    state = state,
                    onCategoryClick = onCategoryClick,
                    onQueryChange = onQueryChange,
                    onVoiceClick = onVoiceClick
                )
            }
        }
    }
}

@Composable
private fun VoiceBrowser(
    state: AquaUiState,
    onCategoryClick: (VoiceCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onVoiceClick: (VoiceItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBox(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(state.categories, key = { it.name }) { category ->
                AssistChip(
                    onClick = { onCategoryClick(category) },
                    label = { Text(category.description) },
                    leadingIcon = if (category.name == state.selected?.name) {
                        { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00B8C8))) }
                    } else {
                        null
                    }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.query.isBlank()) state.selected?.description.orEmpty() else "Search results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.shownVoices.size} voices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF625B71)
                )
            }
            state.playing?.let {
                Text(
                    text = "Playing: ${it.description}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF6C2BFF),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.shownVoices, key = { it.name }) { voice ->
                VoiceCard(
                    voice = voice,
                    isPlaying = state.playing?.name == voice.name,
                    onClick = { onVoiceClick(voice) }
                )
            }
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
        placeholder = { Text("Search voices") },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun VoiceCard(voice: VoiceItem, isPlaying: Boolean, onClick: () -> Unit) {
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
                    text = voice.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = voice.name,
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
            Text("Loading voices")
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
            Text("Failed to load voices", style = MaterialTheme.typography.titleMedium)
            Text(message, color = Color(0xFF7A7286))
            Button(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

private fun String.parseVoiceCategories(): List<VoiceCategory> {
    return JsonParser.parseString(this).asJsonObject
        .getAsJsonArray("voices")
        .map { categoryJson ->
            val category = categoryJson.asJsonObject
            VoiceCategory(
                name = category.get("categoryName").asString,
                description = category.getAsJsonObject("categoryDescription").bestText(),
                voices = category.getAsJsonArray("voiceList").map { voiceJson ->
                    val voice = voiceJson.asJsonObject
                    VoiceItem(
                        name = voice.get("name").asString,
                        path = voice.get("path").asString,
                        description = voice.getAsJsonObject("description").bestText()
                    )
                }
            )
        }
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
