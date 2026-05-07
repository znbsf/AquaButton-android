package aquacrew.aquabutton.api.provider

import aquacrew.aquabutton.model.TextTranslation
import aquacrew.aquabutton.model.VoiceCategory
import aquacrew.aquabutton.model.VoiceFileResponse
import aquacrew.aquabutton.model.VoiceItem
import aquacrew.aquabutton.util.HttpUtils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

open class AquaButtonApiProvider : IAssetsApiProvider {

    companion object {

        private const val GITHUB_API_HOST = "https://raw.githubusercontent.com"

        private const val REQ_TAG_DOWNLOAD_VOICE = "tag_download_voice"

    }

    protected open val repoOwner = "zyzsdy"
    protected open val repoName = "aqua-button"

    open val repoUrl: String get() = "$GITHUB_API_HOST/$repoOwner/$repoName"
    open val voicesUrl: String get() = "$repoUrl/master/src/voices.json"
    open val voiceFileUrl: String get() = "$repoUrl/master/public/voices/%s"

    override suspend fun getVoices(): List<VoiceCategory> {
        val request = Request.Builder().url(voicesUrl).build()
        val response = withContext(Dispatchers.IO) { HttpUtils.client.newCall(request).execute() }
        if (!response.isSuccessful) {
            throw IOException("Response code is not okay (${response.code})")
        }
        val rawJson = response.body?.string()?.trim()
            ?: throw IOException("This response has no body.")
        return JsonParser.parseString(rawJson).asJsonObject
            .getAsJsonArray("voices")
            .map { categoryJson ->
                val category = categoryJson.asJsonObject
                VoiceCategory(
                    name = category.get("categoryName").asString,
                    description = category.getAsJsonObject("categoryDescription").asTextTranslation(),
                    voiceList = category.getAsJsonArray("voiceList").map { voiceJson ->
                        val voice = voiceJson.asJsonObject
                        VoiceItem(
                            name = voice.get("name").asString,
                            path = voice.get("path").asString,
                            description = voice.getAsJsonObject("description").asTextTranslation()
                        )
                    }
                )
            }
    }

    private fun JsonObject.asTextTranslation(): TextTranslation {
        return TextTranslation().apply {
            entrySet().forEach { (key, value) ->
                this[key] = if (value.isJsonNull) null else value.asString
            }
        }
    }

    override suspend fun getVoiceFileResponse(voice: VoiceItem): VoiceFileResponse {
        val path = voice.path

        val request = Request.Builder()
            .url(voiceFileUrl.format(path))
            .tag(REQ_TAG_DOWNLOAD_VOICE)
            .build()

        val response = withContext(Dispatchers.IO) { HttpUtils.client.newCall(request).execute() }
        if (response.code == 200) {
            return VoiceFileResponse(
                response.body?.byteStream() ?: throw IOException("This response has no body."),
                path
            )
        } else {
            throw IOException("Response code is not okay (${response.code})")
        }
    }

    override fun cancelVoiceFileRequest() {
        HttpUtils.cancelRequest(REQ_TAG_DOWNLOAD_VOICE)
    }

}
