package aquacrew.aquabutton.ui.main.event

import android.content.Context
import android.content.ContextWrapper
import aquacrew.aquabutton.model.VoiceItem

interface MainUiEventCallback {

    fun toggleCategoryMenu()

    fun showErrorTextOnSnackbar(text: String)

    fun requestSaveVoice(voice: VoiceItem)

    fun requestReload()

}

fun Context.mainUiEventCallback(): MainUiEventCallback? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is MainUiEventCallback) return current
        current = current.baseContext
    }
    return current as? MainUiEventCallback
}
