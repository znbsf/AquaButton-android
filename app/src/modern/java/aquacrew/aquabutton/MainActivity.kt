package aquacrew.aquabutton

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
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
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val AQUA_VOICE_BASE_URL =
    "https://raw.githubusercontent.com/zyzsdy/aqua-button/master/public/voices/"
private const val MEA_VOICE_BASE_URL =
    "https://raw.githubusercontent.com/zyzsdy/meamea-button/master/public/voices/"
private const val PACK_SCHEMA_VERSION = 1
private const val PREFS_NAME = "buttonbox_prefs"
private const val PREF_HIDDEN_BUILT_IN_PACKS = "hidden_built_in_packs"
private const val PREF_HIDDEN_BUILT_IN_CATEGORIES = "hidden_built_in_categories"
private const val PREF_HIDDEN_BUILT_IN_ITEMS = "hidden_built_in_items"

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

data class ImportPackPreview(
    val uri: Uri,
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val schemaVersion: Int,
    val categoryCount: Int,
    val itemCount: Int,
    val audioCount: Int,
    val videoCount: Int,
    val existingPackName: String?,
    val existingPackIsBuiltIn: Boolean
) {
    val hasConflict: Boolean
        get() = existingPackName != null
}

enum class ImportPackMode {
    Replace,
    Copy
}

enum class SortDirection {
    Up,
    Down
}

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

    @Query("SELECT * FROM packs WHERE id = :packId LIMIT 1")
    suspend fun getPack(packId: String): PackEntity?

    @Query("SELECT * FROM button_items WHERE id = :itemId LIMIT 1")
    suspend fun getItem(itemId: String): ButtonItemEntity?

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategory(categoryId: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacks(packs: List<PackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ButtonItemEntity>)

    @Query("DELETE FROM trigger_phrases WHERE itemId IN (SELECT id FROM button_items WHERE packId IN (SELECT id FROM packs WHERE isBuiltIn = 1) AND assetPath IS NOT NULL)")
    suspend fun deleteBuiltInTriggerPhrases()

    @Query("DELETE FROM button_items WHERE packId IN (SELECT id FROM packs WHERE isBuiltIn = 1) AND assetPath IS NOT NULL")
    suspend fun deleteBuiltInItems()

    @Query("DELETE FROM categories WHERE packId IN (SELECT id FROM packs WHERE isBuiltIn = 1)")
    suspend fun deleteBuiltInCategories()

    @Query("DELETE FROM packs WHERE isBuiltIn = 1")
    suspend fun deleteBuiltInPacks()

    @Query("DELETE FROM trigger_phrases WHERE itemId IN (SELECT id FROM button_items WHERE packId = :packId)")
    suspend fun deleteTriggerPhrasesForPack(packId: String)

    @Query("DELETE FROM button_items WHERE packId = :packId")
    suspend fun deleteItemsForPack(packId: String)

    @Query("DELETE FROM categories WHERE packId = :packId")
    suspend fun deleteCategoriesForPack(packId: String)

    @Query("DELETE FROM packs WHERE id = :packId")
    suspend fun deletePack(packId: String)

    @Query("DELETE FROM trigger_phrases WHERE itemId = :itemId")
    suspend fun deleteTriggerPhrasesForItem(itemId: String)

    @Query("DELETE FROM button_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM packs")
    suspend fun getMaxPackSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories WHERE packId = :packId")
    suspend fun getMaxCategorySortOrder(packId: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM button_items WHERE categoryId = :categoryId")
    suspend fun getMaxItemSortOrder(categoryId: String): Int

    @Query("UPDATE packs SET name = :name WHERE id = :packId")
    suspend fun renamePack(packId: String, name: String)

    @Query("UPDATE categories SET title = :title WHERE id = :categoryId")
    suspend fun renameCategory(categoryId: String, title: String)

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE id = :categoryId")
    suspend fun updateCategorySortOrder(categoryId: String, sortOrder: Int)

    @Query("DELETE FROM trigger_phrases WHERE itemId IN (SELECT id FROM button_items WHERE categoryId = :categoryId)")
    suspend fun deleteTriggerPhrasesForCategory(categoryId: String)

    @Query("DELETE FROM button_items WHERE categoryId = :categoryId")
    suspend fun deleteItemsForCategory(categoryId: String)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("UPDATE button_items SET title = :title, categoryId = :categoryId, sortOrder = :sortOrder WHERE id = :itemId")
    suspend fun updateButton(itemId: String, title: String, categoryId: String, sortOrder: Int)

    @Query("UPDATE button_items SET sortOrder = :sortOrder WHERE id = :itemId")
    suspend fun updateButtonSortOrder(itemId: String, sortOrder: Int)
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
        val builtInPacks = loadBuiltInPacks().visibleBuiltInPacks()
        database.withTransaction {
            val dao = database.buttonPackDao()
            builtInPacks.forEach { pack ->
                val existing = dao.getPack(pack.id)
                if (existing != null && !existing.isBuiltIn) {
                    dao.deleteTriggerPhrasesForPack(pack.id)
                    dao.deleteItemsForPack(pack.id)
                    dao.deleteCategoriesForPack(pack.id)
                    dao.deletePack(pack.id)
                }
            }
            dao.deleteBuiltInTriggerPhrases()
            dao.deleteBuiltInItems()
            dao.deleteBuiltInPacks()
            hiddenBuiltInCategories().forEach { categoryId ->
                dao.deleteTriggerPhrasesForCategory(categoryId)
                dao.deleteItemsForCategory(categoryId)
                dao.deleteCategory(categoryId)
            }
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

    suspend fun previewImportPack(uri: Uri): ImportPackPreview {
        return withExtractedPack(uri) { extractRoot, manifest ->
            val schemaVersion = manifest.getOptionalInt("schemaVersion") ?: PACK_SCHEMA_VERSION
            require(schemaVersion <= PACK_SCHEMA_VERSION) {
                "Pack schema v$schemaVersion is newer than this app supports"
            }
            val packId = manifest.getRequiredString("id").safeId()
            val packName = manifest.getRequiredString("name")
            val author = manifest.getOptionalString("author").ifBlank { "Imported" }
            val description = manifest.getOptionalString("description")
            val categories = manifest.getAsJsonArray("categories") ?: JsonArray()
            require(packId.isNotBlank()) { "Pack id is empty" }
            require(packName.isNotBlank()) { "Pack name is empty" }
            require(categories.size() > 0) { "Pack has no categories" }

            var itemCount = 0
            var audioCount = 0
            var videoCount = 0
            categories.forEach { categoryElement ->
                val category = categoryElement.asJsonObject
                val categoryId = category.getRequiredString("id").safeId()
                require(categoryId.isNotBlank()) { "Category id is empty" }
                val items = category.getAsJsonArray("items") ?: JsonArray()
                items.forEach { itemElement ->
                    val item = itemElement.asJsonObject
                    val itemId = item.getRequiredString("id").safeId()
                    require(itemId.isNotBlank()) { "Button id is empty" }
                    val mediaType = item.getOptionalString("mediaType").ifBlank { "audio" }
                    require(mediaType == "audio" || mediaType == "video") {
                        "Unsupported media type: $mediaType"
                    }
                    val mediaPath = item.getRequiredString("mediaPath")
                    val mediaFile = safeRelativeFile(extractRoot, mediaPath)
                    require(mediaFile.isFile) { "Missing media file: $mediaPath" }
                    itemCount += 1
                    if (mediaType == "video") videoCount += 1 else audioCount += 1
                }
            }
            require(itemCount > 0) { "Pack has no buttons" }

            val existing = database.buttonPackDao().getPack(packId)
            ImportPackPreview(
                uri = uri,
                id = packId,
                name = packName,
                author = author,
                description = description,
                schemaVersion = schemaVersion,
                categoryCount = categories.size(),
                itemCount = itemCount,
                audioCount = audioCount,
                videoCount = videoCount,
                existingPackName = existing?.name,
                existingPackIsBuiltIn = existing?.isBuiltIn == true
            )
        }
    }

    suspend fun importPack(uri: Uri, mode: ImportPackMode): String {
        return withExtractedPack(uri) { extractRoot, manifest ->
            importExtractedPack(extractRoot, manifest, mode)
        }
    }

    private suspend fun <T> withExtractedPack(uri: Uri, block: suspend (File, JsonObject) -> T): T {
        val importRoot = File(context.cacheDir, "pack_import_${System.currentTimeMillis()}")
        val extractRoot = File(importRoot, "unzipped")
        try {
            extractRoot.mkdirs()
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open selected pack" }
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val outFile = safeZipFile(extractRoot, entry)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { output -> zip.copyTo(output) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            val manifestFile = File(extractRoot, "pack.json")
            require(manifestFile.isFile) { "pack.json is missing" }
            val manifest = JsonParser.parseString(manifestFile.readText()).asJsonObject
            return block(extractRoot, manifest)
        } finally {
            importRoot.deleteRecursively()
        }
    }

    private suspend fun importExtractedPack(
        extractRoot: File,
        manifest: JsonObject,
        mode: ImportPackMode
    ): String {
        val schemaVersion = manifest.getOptionalInt("schemaVersion") ?: PACK_SCHEMA_VERSION
        require(schemaVersion <= PACK_SCHEMA_VERSION) {
            "Pack schema v$schemaVersion is newer than this app supports"
        }
        val originalPackId = manifest.getRequiredString("id").safeId()
        val packName = manifest.getRequiredString("name")
        val author = manifest.getOptionalString("author").ifBlank { "Imported" }
        val description = manifest.getOptionalString("description")
        val categories = manifest.getAsJsonArray("categories") ?: JsonArray()
        require(originalPackId.isNotBlank()) { "Pack id is empty" }
        require(packName.isNotBlank()) { "Pack name is empty" }
        require(categories.size() > 0) { "Pack has no categories" }

        val dao = database.buttonPackDao()
        val packId = if (mode == ImportPackMode.Copy && dao.getPack(originalPackId) != null) {
            uniqueImportedPackId(originalPackId)
        } else {
            originalPackId
        }
        val importedName = if (packId == originalPackId) packName else "$packName Copy"
        val packDir = File(context.filesDir, "button_packs/$packId")
        if (packDir.exists()) packDir.deleteRecursively()
        packDir.mkdirs()

        val categoryEntities = mutableListOf<CategoryEntity>()
        val itemEntities = mutableListOf<ButtonItemEntity>()
        categories.forEachIndexed { categoryIndex, categoryElement ->
            val category = categoryElement.asJsonObject
            val categoryId = category.getRequiredString("id").safeId()
            val categoryDbId = "$packId:$categoryId"
            categoryEntities += CategoryEntity(
                id = categoryDbId,
                packId = packId,
                title = category.getOptionalString("title").ifBlank { categoryId },
                sortOrder = categoryIndex
            )

            val items = category.getAsJsonArray("items") ?: JsonArray()
            items.forEachIndexed { itemIndex, itemElement ->
                val item = itemElement.asJsonObject
                val itemId = item.getRequiredString("id").safeId()
                val mediaType = item.getOptionalString("mediaType").ifBlank { "audio" }
                require(mediaType == "audio" || mediaType == "video") {
                    "Unsupported media type: $mediaType"
                }
                val mediaPath = item.getRequiredString("mediaPath")
                val mediaFile = safeRelativeFile(extractRoot, mediaPath)
                require(mediaFile.isFile) { "Missing media file: $mediaPath" }
                val importedMedia = safeRelativeFile(packDir, mediaPath)
                importedMedia.parentFile?.mkdirs()
                mediaFile.copyTo(importedMedia, overwrite = true)

                itemEntities += ButtonItemEntity(
                    id = "$packId:$itemId",
                    packId = packId,
                    categoryId = categoryDbId,
                    title = item.getOptionalString("title").ifBlank { itemId },
                    mediaType = mediaType,
                    assetPath = null,
                    remoteUrl = null,
                    localPath = importedMedia.absolutePath,
                    sortOrder = itemIndex
                )
            }
        }
        require(itemEntities.isNotEmpty()) { "Pack has no buttons" }

        database.withTransaction {
            if (mode == ImportPackMode.Replace) {
                dao.deleteTriggerPhrasesForPack(packId)
                dao.deleteItemsForPack(packId)
                dao.deleteCategoriesForPack(packId)
                dao.deletePack(packId)
            }
            dao.insertPacks(
                listOf(
                    PackEntity(
                        id = packId,
                        name = importedName,
                        author = author,
                        description = description,
                        logoResName = "main_logo",
                        isBuiltIn = false,
                        sortOrder = dao.getMaxPackSortOrder() + 1
                    )
                )
            )
            dao.insertCategories(categoryEntities)
            dao.insertItems(itemEntities)
        }
        return packId
    }

    private suspend fun uniqueImportedPackId(baseId: String): String {
        val dao = database.buttonPackDao()
        var suffix = 2
        var candidate = "$baseId-copy"
        while (dao.getPack(candidate) != null) {
            candidate = "$baseId-copy-$suffix"
            suffix += 1
        }
        return candidate
    }

    suspend fun exportPack(pack: ButtonPack, uri: Uri) {
        context.contentResolver.openOutputStream(uri).use { output ->
            requireNotNull(output) { "Cannot open export destination" }
            ZipOutputStream(output.buffered()).use { zip ->
                val manifest = pack.toPackManifest()
                zip.putNextEntry(ZipEntry("pack.json"))
                zip.write(GsonBuilder().setPrettyPrinting().create().toJson(manifest).toByteArray())
                zip.closeEntry()

                pack.categories.forEach { category ->
                    category.items.forEach { item ->
                        val mediaPath = item.exportMediaPath(category.id)
                        zip.putNextEntry(ZipEntry(mediaPath))
                        when {
                            item.assetPath != null -> {
                                context.assets.open(item.assetPath).use { it.copyTo(zip) }
                            }
                            item.localPath != null -> {
                                FileInputStream(File(item.localPath)).use { it.copyTo(zip) }
                            }
                            else -> Unit
                        }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    suspend fun createUserPack(name: String, categoryTitle: String): String {
        val cleanName = name.trim()
        val cleanCategory = categoryTitle.trim().ifBlank { "General" }
        require(cleanName.isNotBlank()) { "Pack name is required" }
        val packId = "user-${cleanName.safeId()}-${System.currentTimeMillis()}"
        val categoryId = "$packId:${cleanCategory.safeId()}"
        database.withTransaction {
            val dao = database.buttonPackDao()
            dao.insertPacks(
                listOf(
                    PackEntity(
                        id = packId,
                        name = cleanName,
                        author = "User",
                        description = "Custom button pack.",
                        logoResName = "main_logo",
                        isBuiltIn = false,
                        sortOrder = dao.getMaxPackSortOrder() + 1
                    )
                )
            )
            dao.insertCategories(
                listOf(
                    CategoryEntity(
                        id = categoryId,
                        packId = packId,
                        title = cleanCategory,
                        sortOrder = 0
                    )
                )
            )
        }
        File(context.filesDir, "button_packs/$packId/assets/audio").mkdirs()
        return packId
    }

    suspend fun createCategory(packId: String, title: String): String {
        val dao = database.buttonPackDao()
        val pack = dao.getPack(packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Category name is required" }
        val categoryId = "$packId:${cleanTitle.safeId()}-${UUID.randomUUID().toString().take(8)}"
        database.withTransaction {
            dao.insertCategories(
                listOf(
                    CategoryEntity(
                        id = categoryId,
                        packId = packId,
                        title = cleanTitle,
                        sortOrder = dao.getMaxCategorySortOrder(packId) + 1
                    )
                )
            )
        }
        return categoryId
    }

    suspend fun importAudioButton(packId: String, categoryId: String, uri: Uri, title: String): String {
        val dao = database.buttonPackDao()
        val pack = dao.getPack(packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        val category = dao.getCategory(categoryId)
        requireNotNull(category) { "Selected category no longer exists" }

        val displayName = context.displayName(uri)
        val extension = displayName.substringAfterLast('.', "mp3").safeExtension()
        val itemTitle = title.trim().ifBlank {
            displayName.substringBeforeLast('.').ifBlank { "Audio Button" }
        }
        val itemId = "${itemTitle.safeId()}-${UUID.randomUUID().toString().take(8)}"
        val mediaFile = File(
            context.filesDir,
            "button_packs/$packId/assets/audio/${itemId.safeId()}.$extension"
        )
        mediaFile.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open selected audio" }
            mediaFile.outputStream().use { output -> input.copyTo(output) }
        }

        database.withTransaction {
            dao.insertItems(
                listOf(
                    ButtonItemEntity(
                        id = "$packId:$itemId",
                        packId = packId,
                        categoryId = categoryId,
                        title = itemTitle,
                        mediaType = "audio",
                        assetPath = null,
                        remoteUrl = null,
                        localPath = mediaFile.absolutePath,
                        sortOrder = dao.getMaxItemSortOrder(categoryId) + 1
                    )
                )
            )
        }
        return "$packId:$itemId"
    }

    suspend fun importRecordedAudioButton(
        packId: String,
        categoryId: String,
        sourceFile: File,
        title: String
    ): String {
        val dao = database.buttonPackDao()
        val pack = dao.getPack(packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        val category = dao.getCategory(categoryId)
        requireNotNull(category) { "Selected category no longer exists" }
        require(sourceFile.isFile && sourceFile.length() > 0L) { "Recording is empty" }

        val itemTitle = title.trim().ifBlank { "Recorded Button" }
        val itemId = "${itemTitle.safeId()}-${UUID.randomUUID().toString().take(8)}"
        val mediaFile = File(
            context.filesDir,
            "button_packs/$packId/assets/audio/${itemId.safeId()}.m4a"
        )
        mediaFile.parentFile?.mkdirs()
        sourceFile.copyTo(mediaFile, overwrite = true)

        database.withTransaction {
            dao.insertItems(
                listOf(
                    ButtonItemEntity(
                        id = "$packId:$itemId",
                        packId = packId,
                        categoryId = categoryId,
                        title = itemTitle,
                        mediaType = "audio",
                        assetPath = null,
                        remoteUrl = null,
                        localPath = mediaFile.absolutePath,
                        sortOrder = dao.getMaxItemSortOrder(categoryId) + 1
                    )
                )
            )
        }
        return "$packId:$itemId"
    }

    suspend fun renamePack(packId: String, name: String) {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Pack name is required" }
        val dao = database.buttonPackDao()
        requireNotNull(dao.getPack(packId)) { "Selected pack no longer exists" }
        dao.renamePack(packId, cleanName)
    }

    suspend fun renameCategory(categoryId: String, title: String) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Category name is required" }
        val dao = database.buttonPackDao()
        requireNotNull(dao.getCategory(categoryId)) { "Selected category no longer exists" }
        dao.renameCategory(categoryId, cleanTitle)
    }

    suspend fun deleteCategory(categoryId: String) {
        val dao = database.buttonPackDao()
        val category = dao.getCategory(categoryId)
        requireNotNull(category) { "Selected category no longer exists" }
        val pack = dao.getPack(category.packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        val items = dao.getItems().filter { it.categoryId == categoryId }
        if (pack.isBuiltIn) {
            rememberHiddenBuiltInCategory(categoryId)
        }
        database.withTransaction {
            dao.deleteTriggerPhrasesForCategory(categoryId)
            dao.deleteItemsForCategory(categoryId)
            dao.deleteCategory(categoryId)
        }
        items.forEach { entity ->
            entity.localPath?.let { File(it).delete() }
            if (pack.isBuiltIn && entity.assetPath != null) {
                rememberHiddenBuiltInItem(entity.id)
            }
        }
    }

    suspend fun moveCategory(categoryId: String, direction: SortDirection) {
        val dao = database.buttonPackDao()
        val category = dao.getCategory(categoryId)
        requireNotNull(category) { "Selected category no longer exists" }
        val pack = dao.getPack(category.packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        require(!pack.isBuiltIn) { "Built-in pack order cannot be changed" }
        val categories = dao.getCategories().filter { it.packId == category.packId }
        val index = categories.indexOfFirst { it.id == categoryId }
        require(index >= 0) { "Selected category no longer exists" }
        val targetIndex = when (direction) {
            SortDirection.Up -> index - 1
            SortDirection.Down -> index + 1
        }
        if (targetIndex !in categories.indices) return
        val reordered = categories.toMutableList().apply {
            add(targetIndex, removeAt(index))
        }
        database.withTransaction {
            reordered.forEachIndexed { sortOrder, entity ->
                dao.updateCategorySortOrder(entity.id, sortOrder)
            }
        }
    }

    suspend fun updateButton(item: ButtonItem, title: String, categoryId: String) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Button title is required" }
        val dao = database.buttonPackDao()
        val itemDbId = "${item.packId}:${item.id}"
        val entity = dao.getItem(itemDbId)
        requireNotNull(entity) { "Button no longer exists" }
        val targetCategory = dao.getCategory(categoryId)
        requireNotNull(targetCategory) { "Target category no longer exists" }
        require(targetCategory.packId == item.packId) { "Target category belongs to a different pack" }
        val sortOrder = if (entity.categoryId == categoryId) {
            entity.sortOrder
        } else {
            dao.getMaxItemSortOrder(categoryId) + 1
        }
        dao.updateButton(itemDbId, cleanTitle, categoryId, sortOrder)
    }

    suspend fun moveButton(item: ButtonItem, direction: SortDirection) {
        val dao = database.buttonPackDao()
        val pack = dao.getPack(item.packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        require(!pack.isBuiltIn) { "Built-in pack order cannot be changed" }
        val itemDbId = "${item.packId}:${item.id}"
        val entity = dao.getItem(itemDbId)
        requireNotNull(entity) { "Button no longer exists" }
        val items = dao.getItems().filter { it.categoryId == entity.categoryId }
        val index = items.indexOfFirst { it.id == itemDbId }
        require(index >= 0) { "Button no longer exists" }
        val targetIndex = when (direction) {
            SortDirection.Up -> index - 1
            SortDirection.Down -> index + 1
        }
        if (targetIndex !in items.indices) return
        val reordered = items.toMutableList().apply {
            add(targetIndex, removeAt(index))
        }
        database.withTransaction {
            reordered.forEachIndexed { sortOrder, itemEntity ->
                dao.updateButtonSortOrder(itemEntity.id, sortOrder)
            }
        }
    }

    suspend fun deletePack(packId: String) {
        val dao = database.buttonPackDao()
        val pack = dao.getPack(packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        if (pack.isBuiltIn) {
            rememberHiddenBuiltInPack(packId)
        }
        database.withTransaction {
            dao.deleteTriggerPhrasesForPack(packId)
            dao.deleteItemsForPack(packId)
            dao.deleteCategoriesForPack(packId)
            dao.deletePack(packId)
        }
        File(context.filesDir, "button_packs/$packId").deleteRecursively()
    }

    suspend fun deleteButton(item: ButtonItem) {
        val dao = database.buttonPackDao()
        val pack = dao.getPack(item.packId)
        requireNotNull(pack) { "Selected pack no longer exists" }
        val itemDbId = "${item.packId}:${item.id}"
        val entity = dao.getItem(itemDbId)
        requireNotNull(entity) { "Button no longer exists" }
        if (pack.isBuiltIn) {
            rememberHiddenBuiltInItem(itemDbId)
        }
        database.withTransaction {
            dao.deleteTriggerPhrasesForItem(itemDbId)
            dao.deleteItem(itemDbId)
        }
        entity.localPath?.let { File(it).delete() }
    }

    private fun List<ButtonPack>.visibleBuiltInPacks(): List<ButtonPack> {
        val hiddenPacks = hiddenBuiltInPacks()
        val hiddenCategories = hiddenBuiltInCategories()
        val hiddenItems = hiddenBuiltInItems()
        return filterNot { it.id in hiddenPacks }.map { pack ->
            pack.copy(
                categories = pack.categories
                    .filterNot { category -> "${pack.id}:${category.id}" in hiddenCategories }
                    .map { category ->
                        category.copy(
                            items = category.items.filterNot { item ->
                                "${item.packId}:${item.id}" in hiddenItems
                            }
                        )
                    }
            )
        }
    }

    private fun hiddenBuiltInPacks(): Set<String> =
        prefs().getStringSet(PREF_HIDDEN_BUILT_IN_PACKS, emptySet()).orEmpty()

    private fun hiddenBuiltInCategories(): Set<String> =
        prefs().getStringSet(PREF_HIDDEN_BUILT_IN_CATEGORIES, emptySet()).orEmpty()

    private fun hiddenBuiltInItems(): Set<String> =
        prefs().getStringSet(PREF_HIDDEN_BUILT_IN_ITEMS, emptySet()).orEmpty()

    private fun rememberHiddenBuiltInPack(packId: String) {
        prefs().edit()
            .putStringSet(PREF_HIDDEN_BUILT_IN_PACKS, hiddenBuiltInPacks() + packId)
            .apply()
    }

    private fun rememberHiddenBuiltInCategory(categoryId: String) {
        prefs().edit()
            .putStringSet(PREF_HIDDEN_BUILT_IN_CATEGORIES, hiddenBuiltInCategories() + categoryId)
            .apply()
    }

    private fun rememberHiddenBuiltInItem(itemDbId: String) {
        prefs().edit()
            .putStringSet(PREF_HIDDEN_BUILT_IN_ITEMS, hiddenBuiltInItems() + itemDbId)
            .apply()
    }

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadBuiltInPacks(): List<ButtonPack> {
        if (!BuildConfig.INCLUDE_BUILT_IN_PACKS) {
            return emptyList()
        }
        val assets = context.assets
        val aquaJson = assets.open("voices.json").bufferedReader().use { it.readText() }
        val meaJson = assets.open("mea_voices.json").bufferedReader().use { it.readText() }
        val memeJson = assets.open("meme_voices.json").bufferedReader().use { it.readText() }
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
            ),
            memeJson.parseButtonPack(
                packId = "myinstants-meme-pack",
                packName = "MyInstants Meme Pack",
                author = "MyInstants user uploads, collected locally",
                description = "Bundled local meme sound pack. Verify rights before redistribution.",
                logoResId = R.drawable.main_logo,
                assetPrefix = "meme_voices",
                remoteBaseUrl = "",
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
    val notice: String? = null,
    val packs: List<ButtonPack> = emptyList(),
    val selectedPackId: String? = null,
    val selectedCategoryId: String? = null,
    val query: String = "",
    val playing: ButtonItem? = null,
    val pendingImportPreview: ImportPackPreview? = null
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
    private var repository: ButtonPackRepository? = null

    fun load(activity: ComponentActivity) {
        assetManager = activity.assets
        repository = ButtonPackRepository(activity.applicationContext)
        state = state.copy(loading = true, error = null, notice = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    requireNotNull(repository).loadPacks()
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

    fun previewImportPack(activity: ComponentActivity, uri: Uri) {
        state = state.copy(loading = true, error = null, notice = "Checking pack")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.previewImportPack(uri)
                }
            }.onSuccess { preview ->
                state = state.copy(
                    loading = false,
                    pendingImportPreview = preview,
                    error = null,
                    notice = null
                )
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    error = "Import check failed: ${error.message ?: "Unknown pack error"}",
                    notice = null
                )
            }
        }
    }

    fun cancelImportPreview() {
        state = state.copy(pendingImportPreview = null)
    }

    fun confirmImportPack(activity: ComponentActivity, mode: ImportPackMode) {
        val preview = state.pendingImportPreview ?: return
        state = state.copy(loading = true, error = null, notice = "Importing ${preview.name}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    val importedPackId = repo.importPack(preview.uri, mode)
                    importedPackId to repo.loadPacks()
                }
            }.onSuccess { (importedPackId, packs) ->
                val importedPack = packs.firstOrNull { it.id == importedPackId }
                state = state.copy(
                    loading = false,
                    packs = packs,
                    selectedPackId = importedPackId,
                    selectedCategoryId = importedPack?.categories?.firstOrNull()?.id,
                    query = "",
                    pendingImportPreview = null,
                    error = null,
                    notice = "Imported ${importedPack?.name ?: importedPackId} (${importedPack?.itemCount ?: 0} buttons)"
                )
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    pendingImportPreview = null,
                    error = error.message ?: "Failed to import pack",
                    notice = null
                )
            }
        }
    }

    fun exportSelectedPack(activity: ComponentActivity, uri: Uri) {
        val pack = state.selectedPack ?: return
        state = state.copy(error = null, notice = "Exporting ${pack.name}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.exportPack(pack, uri)
                }
            }.onSuccess {
                state = state.copy(notice = "Exported ${pack.name} (${pack.itemCount} buttons)", error = null)
            }.onFailure { error ->
                state = state.copy(
                    error = error.message ?: "Failed to export pack",
                    notice = null
                )
            }
        }
    }

    fun createUserPack(activity: ComponentActivity, name: String, categoryTitle: String) {
        state = state.copy(loading = true, error = null, notice = "Creating pack")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    val packId = repo.createUserPack(name, categoryTitle)
                    packId to repo.loadPacks()
                }
            }.onSuccess { (packId, packs) ->
                val pack = packs.firstOrNull { it.id == packId }
                state = state.copy(
                    loading = false,
                    packs = packs,
                    selectedPackId = packId,
                    selectedCategoryId = pack?.categories?.firstOrNull()?.id,
                    query = "",
                    error = null,
                    notice = "Created ${pack?.name ?: "pack"}"
                )
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    error = error.message ?: "Failed to create pack",
                    notice = null
                )
            }
        }
    }

    fun createCategory(activity: ComponentActivity, name: String) {
        val pack = state.selectedPack ?: return
        state = state.copy(error = null, notice = "Creating category")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    val categoryId = repo.createCategory(pack.id, name)
                    categoryId to repo.loadPacks()
                }
            }.onSuccess { (categoryId, packs) ->
                val updatedPack = packs.firstOrNull { it.id == pack.id }
                val category = updatedPack?.categories?.firstOrNull { it.id == categoryId }
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack.id,
                    selectedCategoryId = categoryId,
                    query = "",
                    error = null,
                    notice = "Created ${category?.title ?: "category"}"
                )
            }.onFailure { error ->
                state = state.copy(
                    error = error.message ?: "Failed to create category",
                    notice = null
                )
            }
        }
    }

    fun importAudio(activity: ComponentActivity, uri: Uri, title: String, category: ButtonCategory) {
        val pack = state.selectedPack ?: return
        state = state.copy(error = null, notice = "Adding audio")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    val itemDbId = repo.importAudioButton(pack.id, category.id, uri, title)
                    itemDbId to repo.loadPacks()
                }
            }.onSuccess { (itemDbId, packs) ->
                val importedPack = packs.firstOrNull { it.id == pack.id }
                val importedItem = importedPack?.categories
                    ?.flatMap { it.items }
                    ?.firstOrNull { "${it.packId}:${it.id}" == itemDbId }
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack.id,
                    selectedCategoryId = category.id,
                    query = "",
                    error = null,
                    notice = "Added ${importedItem?.title ?: "audio"}"
                )
            }.onFailure { error ->
                state = state.copy(
                    error = error.message ?: "Failed to add audio",
                    notice = null
                )
            }
        }
    }

    fun importRecordedAudio(
        activity: ComponentActivity,
        sourceFile: File,
        title: String,
        category: ButtonCategory
    ) {
        val pack = state.selectedPack ?: return
        state = state.copy(error = null, notice = "Saving recording")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    val itemDbId = repo.importRecordedAudioButton(pack.id, category.id, sourceFile, title)
                    sourceFile.delete()
                    itemDbId to repo.loadPacks()
                }
            }.onSuccess { (itemDbId, packs) ->
                val importedPack = packs.firstOrNull { it.id == pack.id }
                val importedItem = importedPack?.categories
                    ?.flatMap { it.items }
                    ?.firstOrNull { "${it.packId}:${it.id}" == itemDbId }
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack.id,
                    selectedCategoryId = category.id,
                    query = "",
                    error = null,
                    notice = "Recorded ${importedItem?.title ?: "audio"}"
                )
            }.onFailure { error ->
                state = state.copy(
                    error = error.message ?: "Failed to save recording",
                    notice = null
                )
            }
        }
    }

    fun showNotice(message: String) {
        state = state.copy(error = null, notice = message)
    }

    fun renameSelectedPack(activity: ComponentActivity, name: String) {
        val pack = state.selectedPack ?: return
        state = state.copy(error = null, notice = "Renaming ${pack.name}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.renamePack(pack.id, name)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                val renamedPack = packs.firstOrNull { it.id == pack.id }
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack.id,
                    selectedCategoryId = renamedPack?.categories?.firstOrNull { it.id == state.selectedCategoryId }?.id
                        ?: renamedPack?.categories?.firstOrNull()?.id,
                    error = null,
                    notice = "Renamed ${renamedPack?.name ?: "pack"}"
                )
            }.onFailure { error ->
                state = state.copy(error = error.message ?: "Failed to rename pack", notice = null)
            }
        }
    }

    fun renameSelectedCategory(activity: ComponentActivity, title: String) {
        val pack = state.selectedPack ?: return
        val category = state.selectedCategory ?: return
        state = state.copy(error = null, notice = "Renaming ${category.title}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.renameCategory(category.id, title)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                val updatedPack = packs.firstOrNull { it.id == pack.id }
                val updatedCategory = updatedPack?.categories?.firstOrNull { it.id == category.id }
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack.id,
                    selectedCategoryId = updatedCategory?.id ?: updatedPack?.categories?.firstOrNull()?.id,
                    query = "",
                    error = null,
                    notice = "Renamed ${updatedCategory?.title ?: "category"}"
                )
            }.onFailure { error ->
                state = state.copy(error = error.message ?: "Failed to rename category", notice = null)
            }
        }
    }

    fun moveSelectedCategory(activity: ComponentActivity, direction: SortDirection) {
        val pack = state.selectedPack ?: return
        val category = state.selectedCategory ?: return
        state = state.copy(error = null, notice = "Moving ${category.title}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.moveCategory(category.id, direction)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack.id,
                    selectedCategoryId = category.id,
                    query = "",
                    error = null,
                    notice = "Moved ${category.title}"
                )
            }.onFailure { error ->
                state = state.copy(error = error.message ?: "Failed to move category", notice = null)
            }
        }
    }

    fun deleteSelectedCategory(activity: ComponentActivity) {
        val pack = state.selectedPack ?: return
        val category = state.selectedCategory ?: return
        if (category.items.any { state.playing?.packId == it.packId && state.playing?.id == it.id }) stop()
        state = state.copy(error = null, notice = "Deleting ${category.title}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.deleteCategory(category.id)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                val updatedPack = packs.firstOrNull { it.id == pack.id }
                state = state.copy(
                    packs = packs,
                    selectedPackId = updatedPack?.id ?: packs.firstOrNull()?.id,
                    selectedCategoryId = updatedPack?.categories?.firstOrNull()?.id
                        ?: packs.firstOrNull()?.categories?.firstOrNull()?.id,
                    query = "",
                    error = null,
                    notice = "Deleted ${category.title}"
                )
            }.onFailure { error ->
                state = state.copy(error = error.message ?: "Failed to delete category", notice = null)
            }
        }
    }

    fun updateButton(activity: ComponentActivity, item: ButtonItem, title: String, category: ButtonCategory) {
        state = state.copy(error = null, notice = "Updating ${item.title}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.updateButton(item, title, category.id)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                val pack = packs.firstOrNull { it.id == item.packId }
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack?.id ?: state.selectedPackId,
                    selectedCategoryId = category.id,
                    query = "",
                    error = null,
                    notice = "Updated ${title.trim()}"
                )
            }.onFailure { error ->
                state = state.copy(error = error.message ?: "Failed to update button", notice = null)
            }
        }
    }

    fun moveButton(activity: ComponentActivity, item: ButtonItem, direction: SortDirection) {
        val pack = state.selectedPack ?: return
        state = state.copy(error = null, notice = "Moving ${item.title}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.moveButton(item, direction)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                val updatedPack = packs.firstOrNull { it.id == pack.id }
                state = state.copy(
                    packs = packs,
                    selectedPackId = updatedPack?.id ?: state.selectedPackId,
                    selectedCategoryId = item.categoryId,
                    query = "",
                    error = null,
                    notice = "Moved ${item.title}"
                )
            }.onFailure { error ->
                state = state.copy(error = error.message ?: "Failed to move button", notice = null)
            }
        }
    }

    fun deleteSelectedPack(activity: ComponentActivity) {
        val pack = state.selectedPack ?: return
        stop()
        state = state.copy(loading = true, error = null, notice = "Deleting ${pack.name}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.deletePack(pack.id)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                state = state.copy(
                    loading = false,
                    packs = packs,
                    selectedPackId = packs.firstOrNull()?.id,
                    selectedCategoryId = packs.firstOrNull()?.categories?.firstOrNull()?.id,
                    query = "",
                    error = null,
                    notice = "Deleted ${pack.name}"
                )
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    error = error.message ?: "Failed to delete pack",
                    notice = null
                )
            }
        }
    }

    fun deleteButton(activity: ComponentActivity, item: ButtonItem) {
        val currentPack = state.selectedPack
        if (currentPack == null) return
        if (state.playing?.packId == item.packId && state.playing?.id == item.id) stop()
        state = state.copy(error = null, notice = "Deleting ${item.title}")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val repo = repository ?: ButtonPackRepository(activity.applicationContext).also {
                        repository = it
                    }
                    repo.deleteButton(item)
                    repo.loadPacks()
                }
            }.onSuccess { packs ->
                val pack = packs.firstOrNull { it.id == item.packId }
                state = state.copy(
                    packs = packs,
                    selectedPackId = pack?.id ?: state.selectedPackId,
                    selectedCategoryId = pack?.categories?.firstOrNull { it.id == item.categoryId }?.id
                        ?: pack?.categories?.firstOrNull()?.id
                        ?: state.selectedCategoryId,
                    query = "",
                    error = null,
                    notice = "Deleted ${item.title}"
                )
            }.onFailure { error ->
                state = state.copy(
                    error = error.message ?: "Failed to delete button",
                    notice = null
                )
            }
        }
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
    var showNewPackDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var showRenamePackDialog by remember { mutableStateOf(false) }
    var showRenameCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var showDeletePackDialog by remember { mutableStateOf(false) }
    var showRecordAudioDialog by remember { mutableStateOf(false) }
    var pendingAudioUri by remember { mutableStateOf<Uri?>(null) }
    var pendingEditItem by remember { mutableStateOf<ButtonItem?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<ButtonItem?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.previewImportPack(activity, uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.exportSelectedPack(activity, uri)
    }
    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingAudioUri = uri
    }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showRecordAudioDialog = true
        } else {
            viewModel.showNotice("Microphone permission is required to record audio")
        }
    }
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
                onNewPackClick = { showNewPackDialog = true },
                onNewCategoryClick = { showNewCategoryDialog = true },
                onImportAudioClick = { audioLauncher.launch(arrayOf("audio/*")) },
                onRecordAudioClick = {
                    if (
                        ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        showRecordAudioDialog = true
                    } else {
                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onRenamePackClick = { showRenamePackDialog = true },
                onRenameCategoryClick = { showRenameCategoryDialog = true },
                onMoveCategoryUpClick = { viewModel.moveSelectedCategory(activity, SortDirection.Up) },
                onMoveCategoryDownClick = { viewModel.moveSelectedCategory(activity, SortDirection.Down) },
                onDeleteCategoryClick = { showDeleteCategoryDialog = true },
                onDeletePackClick = { showDeletePackDialog = true },
                onEditItemClick = { pendingEditItem = it },
                onMoveItemUpClick = { viewModel.moveButton(activity, it, SortDirection.Up) },
                onMoveItemDownClick = { viewModel.moveButton(activity, it, SortDirection.Down) },
                onDeleteItemClick = { pendingDeleteItem = it },
                onImportClick = {
                    importLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/octet-stream",
                            "application/x-zip-compressed"
                        )
                    )
                },
                onExportClick = {
                    val packId = viewModel.state.selectedPack?.id ?: "buttonpack"
                    exportLauncher.launch("${packId.safeId()}.buttonpack.zip")
                },
                onRetryClick = { viewModel.load(activity) }
            )
            if (showNewPackDialog) {
                NewPackDialog(
                    onDismiss = { showNewPackDialog = false },
                    onCreate = { name, category ->
                        showNewPackDialog = false
                        viewModel.createUserPack(activity, name, category)
                    }
                )
            }
            if (showNewCategoryDialog) {
                NewCategoryDialog(
                    onDismiss = { showNewCategoryDialog = false },
                    onCreate = { name ->
                        showNewCategoryDialog = false
                        viewModel.createCategory(activity, name)
                    }
                )
            }
            if (showRenamePackDialog) {
                viewModel.state.selectedPack?.let { pack ->
                    TextEditDialog(
                        title = "Rename Pack",
                        label = "Pack name",
                        initialValue = pack.name,
                        confirmText = "Rename",
                        onDismiss = { showRenamePackDialog = false },
                        onConfirm = { name ->
                            showRenamePackDialog = false
                            viewModel.renameSelectedPack(activity, name)
                        }
                    )
                } ?: run {
                    showRenamePackDialog = false
                }
            }
            if (showRenameCategoryDialog) {
                viewModel.state.selectedCategory?.let { category ->
                    TextEditDialog(
                        title = "Rename Category",
                        label = "Category name",
                        initialValue = category.title,
                        confirmText = "Rename",
                        onDismiss = { showRenameCategoryDialog = false },
                        onConfirm = { title ->
                            showRenameCategoryDialog = false
                            viewModel.renameSelectedCategory(activity, title)
                        }
                    )
                } ?: run {
                    showRenameCategoryDialog = false
                }
            }
            if (showDeleteCategoryDialog) {
                viewModel.state.selectedCategory?.let { category ->
                    ConfirmDeleteDialog(
                        title = "Delete Category",
                        message = "Delete ${category.title} and ${category.items.size} buttons?",
                        onDismiss = { showDeleteCategoryDialog = false },
                        onConfirm = {
                            showDeleteCategoryDialog = false
                            viewModel.deleteSelectedCategory(activity)
                        }
                    )
                } ?: run {
                    showDeleteCategoryDialog = false
                }
            }
            if (showDeletePackDialog) {
                viewModel.state.selectedPack?.let { pack ->
                    ConfirmDeleteDialog(
                        title = "Delete Pack",
                        message = "Delete ${pack.name} and all of its buttons?",
                        onDismiss = { showDeletePackDialog = false },
                        onConfirm = {
                            showDeletePackDialog = false
                            viewModel.deleteSelectedPack(activity)
                        }
                    )
                } ?: run {
                    showDeletePackDialog = false
                }
            }
            pendingDeleteItem?.let { item ->
                ConfirmDeleteDialog(
                    title = "Delete Button",
                    message = "Delete ${item.title}?",
                    onDismiss = { pendingDeleteItem = null },
                    onConfirm = {
                        pendingDeleteItem = null
                        viewModel.deleteButton(activity, item)
                    }
                )
            }
            viewModel.state.pendingImportPreview?.let { preview ->
                ImportPreviewDialog(
                    preview = preview,
                    onDismiss = viewModel::cancelImportPreview,
                    onImport = { mode -> viewModel.confirmImportPack(activity, mode) }
                )
            }
            pendingAudioUri?.let { uri ->
                val selectedPack = viewModel.state.selectedPack
                val categories = selectedPack?.categories.orEmpty()
                val defaultTitle = activity.displayName(uri).substringBeforeLast('.').ifBlank { "Audio Button" }
                AudioButtonDialog(
                    title = "Add Audio",
                    initialTitle = defaultTitle,
                    categories = categories,
                    initialCategory = viewModel.state.selectedCategory,
                    confirmText = "Add",
                    onDismiss = { pendingAudioUri = null },
                    onConfirm = { buttonTitle, category ->
                        pendingAudioUri = null
                        viewModel.importAudio(activity, uri, buttonTitle, category)
                    }
                )
            }
            if (showRecordAudioDialog) {
                val selectedPack = viewModel.state.selectedPack
                val categories = selectedPack?.categories.orEmpty()
                RecordAudioDialog(
                    activity = activity,
                    categories = categories,
                    initialCategory = viewModel.state.selectedCategory,
                    onDismiss = { showRecordAudioDialog = false },
                    onSave = { buttonTitle, category, file ->
                        showRecordAudioDialog = false
                        viewModel.importRecordedAudio(activity, file, buttonTitle, category)
                    }
                )
            }
            pendingEditItem?.let { item ->
                val pack = viewModel.state.packs.firstOrNull { it.id == item.packId }
                AudioButtonDialog(
                    title = "Edit Button",
                    initialTitle = item.title,
                    categories = pack?.categories.orEmpty(),
                    initialCategory = pack?.categories?.firstOrNull { it.id == item.categoryId },
                    confirmText = "Save",
                    onDismiss = { pendingEditItem = null },
                    onConfirm = { buttonTitle, category ->
                        pendingEditItem = null
                        viewModel.updateButton(activity, item, buttonTitle, category)
                    }
                )
            }
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
    onNewPackClick: () -> Unit,
    onNewCategoryClick: () -> Unit,
    onImportAudioClick: () -> Unit,
    onRecordAudioClick: () -> Unit,
    onRenamePackClick: () -> Unit,
    onRenameCategoryClick: () -> Unit,
    onMoveCategoryUpClick: () -> Unit,
    onMoveCategoryDownClick: () -> Unit,
    onDeleteCategoryClick: () -> Unit,
    onDeletePackClick: () -> Unit,
    onEditItemClick: (ButtonItem) -> Unit,
    onMoveItemUpClick: (ButtonItem) -> Unit,
    onMoveItemDownClick: (ButtonItem) -> Unit,
    onDeleteItemClick: (ButtonItem) -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    var showPackMenu by remember { mutableStateOf(false) }
    val selectedPack = state.selectedPack
    val selectedCategory = state.selectedCategory
    val selectedCategoryIndex = selectedPack?.categories.orEmpty()
        .indexOfFirst { it.id == selectedCategory?.id }
    val canSortSelectedCategory = selectedPack != null &&
        !selectedPack.isBuiltIn &&
        selectedCategory != null &&
        state.query.isBlank()
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
                actions = {
                    IconButton(onClick = { showPackMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Pack actions")
                    }
                    DropdownMenu(
                        expanded = showPackMenu,
                        onDismissRequest = { showPackMenu = false }
                    ) {
                        DropdownMenuHeader("Pack")
                        DropdownMenuItem(
                            text = { Text("Rename Pack") },
                            enabled = state.selectedPack != null,
                            onClick = {
                                showPackMenu = false
                                onRenamePackClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import Pack") },
                            onClick = {
                                showPackMenu = false
                                onImportClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Pack") },
                            enabled = selectedPack != null,
                            onClick = {
                                showPackMenu = false
                                onExportClick()
                            }
                        )
                        DropdownMenuHeader("Category")
                        DropdownMenuItem(
                            text = { Text("Rename Category") },
                            enabled = selectedCategory != null,
                            onClick = {
                                showPackMenu = false
                                onRenameCategoryClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Category Up") },
                            enabled = canSortSelectedCategory && selectedCategoryIndex > 0,
                            onClick = {
                                showPackMenu = false
                                onMoveCategoryUpClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Category Down") },
                            enabled = canSortSelectedCategory &&
                                selectedCategoryIndex >= 0 &&
                                selectedCategoryIndex < selectedPack.categories.lastIndex,
                            onClick = {
                                showPackMenu = false
                                onMoveCategoryDownClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Category") },
                            enabled = selectedCategory != null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFB3261E)
                                )
                            },
                            onClick = {
                                showPackMenu = false
                                onDeleteCategoryClick()
                            }
                        )
                        DropdownMenuHeader("Danger")
                        DropdownMenuItem(
                            text = { Text("Delete Pack") },
                            enabled = selectedPack != null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFB3261E)
                                )
                            },
                            onClick = {
                                showPackMenu = false
                                onDeletePackClick()
                            }
                        )
                    }
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
                    onItemClick = onItemClick,
                    onNewPackClick = onNewPackClick,
                    onNewCategoryClick = onNewCategoryClick,
                    onImportAudioClick = onImportAudioClick,
                    onRecordAudioClick = onRecordAudioClick,
                    onEditItemClick = onEditItemClick,
                    onMoveItemUpClick = onMoveItemUpClick,
                    onMoveItemDownClick = onMoveItemDownClick,
                    onDeleteItemClick = onDeleteItemClick
                )
            }
        }
    }
}

@Composable
private fun DropdownMenuHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF7A7286),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ButtonPackBrowser(
    state: AquaUiState,
    onPackClick: (ButtonPack) -> Unit,
    onCategoryClick: (ButtonCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onItemClick: (ButtonItem) -> Unit,
    onNewPackClick: () -> Unit,
    onNewCategoryClick: () -> Unit,
    onImportAudioClick: () -> Unit,
    onRecordAudioClick: () -> Unit,
    onEditItemClick: (ButtonItem) -> Unit,
    onMoveItemUpClick: (ButtonItem) -> Unit,
    onMoveItemDownClick: (ButtonItem) -> Unit,
    onDeleteItemClick: (ButtonItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        PackSwitcher(
            packs = state.packs,
            selectedPack = state.selectedPack,
            onPackClick = onPackClick,
            onNewPackClick = onNewPackClick
        )
        StatusNotice(state.notice)
        SearchBox(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        CategorySwitcher(
            categories = state.selectedPack?.categories.orEmpty(),
            selectedCategory = state.selectedCategory,
            canAddCategory = state.selectedPack != null,
            onCategoryClick = onCategoryClick,
            onNewCategoryClick = onNewCategoryClick
        )
        PackSummary(state)
        val canAddAudio = state.query.isBlank() &&
            state.selectedPack != null &&
            state.selectedCategory != null
        if (state.shownItems.isEmpty() && !canAddAudio) {
            EmptyPackView(state.selectedPack, state.query)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.shownItems, key = { "${it.packId}:${it.id}" }) { item ->
                    val itemIndex = state.shownItems.indexOfFirst {
                        it.packId == item.packId && it.id == item.id
                    }
                    val canSortItem = state.query.isBlank() &&
                        state.selectedPack?.isBuiltIn == false &&
                        item.categoryId == state.selectedCategory?.id
                    ButtonItemCard(
                        item = item,
                        canDelete = state.selectedPack != null,
                        canMoveUp = canSortItem && itemIndex > 0,
                        canMoveDown = canSortItem &&
                            itemIndex >= 0 &&
                            itemIndex < state.shownItems.lastIndex,
                        isPlaying = state.playing?.packId == item.packId && state.playing.id == item.id,
                        onClick = { onItemClick(item) },
                        onEditClick = { onEditItemClick(item) },
                        onMoveUpClick = { onMoveItemUpClick(item) },
                        onMoveDownClick = { onMoveItemDownClick(item) },
                        onDeleteClick = { onDeleteItemClick(item) }
                    )
                }
                if (canAddAudio) {
                    item {
                        AddAudioCard(
                            onImportClick = onImportAudioClick,
                            onRecordClick = onRecordAudioClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: ImportPackPreview,
    onDismiss: () -> Unit,
    onImport: (ImportPackMode) -> Unit
) {
    val conflictText = when {
        preview.existingPackName == null -> "No existing pack uses this id."
        preview.existingPackIsBuiltIn ->
            "This matches the built-in pack named ${preview.existingPackName}. Import a copy to keep both."
        else -> "This matches the existing pack named ${preview.existingPackName}."
    }
    val actionText = when {
        preview.existingPackName == null -> "Ready to import as a new pack."
        preview.existingPackIsBuiltIn -> "Replace is disabled for built-in packs; import a copy instead."
        else -> "Choose Replace to overwrite the existing pack, or Import Copy to keep both."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Pack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(preview.name, fontWeight = FontWeight.Bold)
                Text("Author: ${preview.author}")
                Text("Format: schema v${preview.schemaVersion}, supported.")
                Text("Contents: ${preview.categoryCount} categories, ${preview.itemCount} buttons.")
                Text("Media: ${preview.audioCount} audio, ${preview.videoCount} video.")
                Text(conflictText)
                Text(
                    text = actionText,
                    color = Color(0xFF625B71)
                )
                if (preview.description.isNotBlank()) {
                    Text(
                        text = preview.description,
                        color = Color(0xFF625B71),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preview.hasConflict) {
                    TextButton(onClick = { onImport(ImportPackMode.Copy) }) {
                        Text("Import Copy")
                    }
                    TextButton(
                        onClick = { onImport(ImportPackMode.Replace) },
                        enabled = !preview.existingPackIsBuiltIn
                    ) {
                        Text("Replace")
                    }
                } else {
                    TextButton(onClick = { onImport(ImportPackMode.Replace) }) {
                        Text("Import")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NewPackDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var packName by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("General") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Pack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = packName,
                    onValueChange = { packName = it },
                    label = { Text("Pack name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("First category") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(packName, categoryName) },
                enabled = packName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(categoryName) },
                enabled = categoryName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TextEditDialog(
    title: String,
    label: String,
    initialValue: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioButtonDialog(
    title: String,
    initialTitle: String,
    categories: List<ButtonCategory>,
    initialCategory: ButtonCategory?,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, ButtonCategory) -> Unit
) {
    var buttonTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var selectedCategory by remember(initialCategory, categories) {
        mutableStateOf(initialCategory ?: categories.firstOrNull())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = buttonTitle,
                    onValueChange = { buttonTitle = it },
                    label = { Text("Button title") },
                    singleLine = true
                )
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF625B71)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        AssistChip(
                            onClick = { selectedCategory = category },
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val category = selectedCategory ?: return@TextButton
                    onConfirm(buttonTitle, category)
                },
                enabled = buttonTitle.isNotBlank() && selectedCategory != null
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordAudioDialog(
    activity: ComponentActivity,
    categories: List<ButtonCategory>,
    initialCategory: ButtonCategory?,
    onDismiss: () -> Unit,
    onSave: (String, ButtonCategory, File) -> Unit
) {
    var buttonTitle by remember { mutableStateOf("Recorded Button") }
    var selectedCategory by remember(initialCategory, categories) {
        mutableStateOf(initialCategory ?: categories.firstOrNull())
    }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPreviewing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to record") }
    var keepRecordedFile by remember { mutableStateOf(false) }

    fun stopPreview() {
        previewPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        previewPlayer = null
        isPreviewing = false
    }

    fun startRecording() {
        stopPreview()
        recordedFile?.takeUnless { keepRecordedFile }?.delete()
        val outputFile = File(
            activity.cacheDir,
            "buttonbox-recordings/recording-${System.currentTimeMillis()}.m4a"
        )
        outputFile.parentFile?.mkdirs()
        runCatching {
            @Suppress("DEPRECATION")
            val mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            recordedFile = outputFile
            isRecording = true
            statusText = "Recording..."
        }.onFailure { error ->
            outputFile.delete()
            recorder = null
            recordedFile = null
            isRecording = false
            statusText = error.message ?: "Failed to start recording"
        }
    }

    fun stopRecording() {
        val activeRecorder = recorder ?: return
        runCatching { activeRecorder.stop() }
            .onFailure {
                recordedFile?.delete()
                recordedFile = null
                statusText = "Recording was too short"
            }
        activeRecorder.release()
        recorder = null
        isRecording = false
        if (recordedFile?.takeIf { it.length() > 0L } != null) {
            statusText = "Recording saved for preview"
        }
    }

    fun playPreview() {
        val file = recordedFile ?: return
        if (isPreviewing) {
            stopPreview()
            statusText = "Preview stopped"
            return
        }
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPreview()
                    statusText = "Preview finished"
                }
                prepare()
                start()
                previewPlayer = this
            }
            isPreviewing = true
            statusText = "Playing preview"
        }.onFailure { error ->
            stopPreview()
            statusText = error.message ?: "Failed to play preview"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching {
                if (isRecording) stop()
                release()
            }
            previewPlayer?.runCatching {
                if (isPlaying) stop()
                release()
            }
            if (!keepRecordedFile) {
                recordedFile?.delete()
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (isRecording) stopRecording()
            onDismiss()
        },
        title = { Text("Record Audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = buttonTitle,
                    onValueChange = { buttonTitle = it },
                    label = { Text("Button title") },
                    singleLine = true
                )
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF625B71)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        AssistChip(
                            onClick = { selectedCategory = category },
                            label = { Text(category.title) },
                            leadingIcon = if (category.id == selectedCategory?.id) {
                                { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00B8C8))) }
                            } else {
                                null
                            }
                        )
                    }
                }
                Text(statusText, color = Color(0xFF625B71))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isRecording) stopRecording() else startRecording()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(if (isRecording) "Stop" else "Record")
                    }
                    Button(
                        onClick = ::playPreview,
                        enabled = recordedFile != null && !isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isPreviewing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Preview")
                    }
                }
                TextButton(
                    onClick = {
                        stopPreview()
                        recordedFile?.delete()
                        recordedFile = null
                        statusText = "Ready to record"
                    },
                    enabled = recordedFile != null && !isRecording
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Re-record")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val file = recordedFile ?: return@TextButton
                    val category = selectedCategory ?: return@TextButton
                    stopPreview()
                    keepRecordedFile = true
                    onSave(buttonTitle, category, file)
                },
                enabled = recordedFile != null &&
                    !isRecording &&
                    buttonTitle.isNotBlank() &&
                    selectedCategory != null
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isRecording) stopRecording()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackSwitcher(
    packs: List<ButtonPack>,
    selectedPack: ButtonPack?,
    onPackClick: (ButtonPack) -> Unit,
    onNewPackClick: () -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        packs.forEach { pack ->
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
        AssistChip(
            onClick = onNewPackClick,
            label = { Text("New Pack") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun StatusNotice(notice: String?) {
    if (notice == null) return
    Text(
        text = notice,
        color = Color(0xFF625B71),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySwitcher(
    categories: List<ButtonCategory>,
    selectedCategory: ButtonCategory?,
    canAddCategory: Boolean,
    onCategoryClick: (ButtonCategory) -> Unit,
    onNewCategoryClick: () -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
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
        if (canAddCategory) {
            AssistChip(
                onClick = onNewCategoryClick,
                label = { Text("Add Category") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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
private fun ButtonItemCard(
    item: ButtonItem,
    canDelete: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showItemMenu by remember { mutableStateOf(false) }
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
            if (canDelete) {
                Box {
                    IconButton(onClick = { showItemMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Button actions",
                            tint = Color(0xFF625B71)
                        )
                    }
                    DropdownMenu(
                        expanded = showItemMenu,
                        onDismissRequest = { showItemMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Move Up") },
                            leadingIcon = {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                            },
                            enabled = canMoveUp,
                            onClick = {
                                showItemMenu = false
                                onMoveUpClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Down") },
                            leadingIcon = {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                            },
                            enabled = canMoveDown,
                            onClick = {
                                showItemMenu = false
                                onMoveDownClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                            },
                            onClick = {
                                showItemMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFB3261E)
                                )
                            },
                            onClick = {
                                showItemMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAudioCard(
    onImportClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5D9FF))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFF6C2BFF)
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Audio",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Import a file or record a new button",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A7286)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Import")
                }
                Button(
                    onClick = onRecordClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Record")
                }
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
        categoryId = categoryId,
        title = title,
        mediaType = mediaType,
        assetPath = assetPath,
        remoteUrl = remoteUrl,
        localPath = localPath
    )
}

private fun ButtonPack.toPackManifest(): JsonObject {
    return JsonObject().apply {
        addProperty("schemaVersion", PACK_SCHEMA_VERSION)
        addProperty("id", id)
        addProperty("name", name)
        addProperty("author", author)
        addProperty("description", description)
        val categoryArray = JsonArray()
        categories.forEach { category ->
            categoryArray.add(
                JsonObject().apply {
                    addProperty("id", category.id.substringAfter(':', category.id))
                    addProperty("title", category.title)
                    val itemArray = JsonArray()
                    category.items.forEach { item ->
                        itemArray.add(
                            JsonObject().apply {
                                addProperty("id", item.id.substringAfter(':', item.id))
                                addProperty("title", item.title)
                                addProperty("mediaType", item.mediaType)
                                addProperty("mediaPath", item.exportMediaPath(category.id))
                                add("triggerPhrases", JsonArray())
                            }
                        )
                    }
                    add("items", itemArray)
                }
            )
        }
        add("categories", categoryArray)
    }
}

private fun ButtonItem.exportMediaPath(categoryId: String): String {
    val extension = when {
        assetPath != null -> File(assetPath).extension.ifBlank { "mp3" }
        localPath != null -> File(localPath).extension.ifBlank { "mp3" }
        mediaType == "video" -> "mp4"
        else -> "mp3"
    }
    val fileName = "${categoryId.substringAfter(':', categoryId).safeId()}-${id.safeId()}.$extension"
    return if (mediaType == "video") {
        "assets/video/$fileName"
    } else {
        "assets/audio/$fileName"
    }
}

private fun JsonObject.getRequiredString(name: String): String {
    return requireNotNull(get(name)?.asString) { "$name is missing" }
}

private fun JsonObject.getOptionalString(name: String): String {
    return get(name)?.asString.orEmpty()
}

private fun JsonObject.getOptionalInt(name: String): Int? {
    return get(name)?.asInt
}

private fun String.safeId(): String {
    return trim()
        .lowercase(Locale.US)
        .replace(Regex("[^\\p{L}\\p{N}._-]+"), "-")
        .trim('-')
        .ifBlank { "pack-${System.currentTimeMillis()}" }
}

private fun String.safeExtension(): String {
    return lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "")
        .ifBlank { "mp3" }
}

private fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index).orEmpty().ifBlank { "audio-${System.currentTimeMillis()}.mp3" }
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
        ?: "audio-${System.currentTimeMillis()}.mp3"
}

private fun safeZipFile(root: File, entry: ZipEntry): File {
    return safeRelativeFile(root, entry.name)
}

private fun safeRelativeFile(root: File, relativePath: String): File {
    val target = File(root, relativePath.replace('\\', '/'))
    val rootPath = root.canonicalFile.toPath()
    val targetPath = target.canonicalFile.toPath()
    require(targetPath.startsWith(rootPath)) { "Unsafe zip path: $relativePath" }
    return target
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
