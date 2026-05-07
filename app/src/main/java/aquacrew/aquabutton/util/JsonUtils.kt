package aquacrew.aquabutton.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import aquacrew.aquabutton.model.TextTranslation
import java.lang.reflect.Type

object JsonUtils {

    var gson: Gson = GsonBuilder()
        .registerTypeAdapter(TextTranslation::class.java, object : JsonDeserializer<TextTranslation> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): TextTranslation {
                return TextTranslation().apply {
                    json.asJsonObject.entrySet().forEach { (key, value) ->
                        this[key] = if (value.isJsonNull) null else value.asString
                    }
                }
            }
        })
        .create()

    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    fun <T> fromJson(string: String, clazz: Class<T>): T {
        return gson.fromJson(string, clazz)
    }

    inline fun <reified T> fromJson(string: String): T {
        return fromJson(string, T::class.java)
    }

}
