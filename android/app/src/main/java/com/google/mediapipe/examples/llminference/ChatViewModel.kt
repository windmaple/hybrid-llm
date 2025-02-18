package com.google.mediapipe.examples.llminference

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.FLAG_SHOW_UI
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Parameter(
    val name: String,
    val value: Float
)

@Serializable
data class Action(
    val tool: String,
    val parameters: List<Parameter>? = null
)

class ChatViewModel(
    private val inferenceModel: InferenceModel,
    private val context: Context
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(inferenceModel.uiState)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> =
        _textInputEnabled.asStateFlow()

    private fun dimScreen() {
        android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, 20)
    }

    private fun increaseVolume() {
        val audioManager:AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 50, FLAG_SHOW_UI);
    }
    @OptIn(ExperimentalSerializationApi::class)
    fun sendMessage(userMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            var currentMessageId: String? = _uiState.value.createLoadingMessage()
            setInputEnabled(false)
            try {
                inferenceModel.generateResponseAsync(userMessage)
                inferenceModel.partialResults
                    .collectIndexed { _, (partialResult, done) ->
                        currentMessageId?.let {
                            currentMessageId = _uiState.value.appendMessage(it, partialResult, done)
                            if (done) {
                                val modelOutput = uiState.value.messages.first().rawMessage
                                val pattern = """```json\s*(\{[\s\S]*?\})\s*```""".toRegex()
                                val jsonContent = pattern.find(modelOutput)?.groupValues?.get(1)
                                val cleanedJsonContent = jsonContent?.replace(Regex(",\\s*]"), "]")?.replace(Regex(",\\s*\\}"), "}")  // remove potential trailing comma; I could not get allowTrailingComma working
                                val json = Json { ignoreUnknownKeys = true }
                                val action = cleanedJsonContent?.let { json.decodeFromString<Action>(it) }

                                if (action?.tool?.contains("dim_screen") == true) {
                                    dimScreen()
                                } else if (action?.tool?.contains("increase_volume") == true) {
                                    increaseVolume()
                                }

                                currentMessageId = null
                                // Re-enable text input
                                setInputEnabled(true)
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                setInputEnabled(true)
            }
        }
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                return ChatViewModel(inferenceModel, context) as T
            }
        }
    }
}
