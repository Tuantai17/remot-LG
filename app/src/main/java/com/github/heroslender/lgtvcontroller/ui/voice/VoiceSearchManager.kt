package com.github.heroslender.lgtvcontroller.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceSearchManager(context: Context) : RecognitionListener {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow(VoiceSearchState())
    val state: StateFlow<VoiceSearchState> = _state.asStateFlow()

    fun start(localeTag: String = DEFAULT_VOICE_LOCALE) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            fail(VoiceSearchError.RECOGNIZER_UNAVAILABLE)
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also {
                it.setRecognitionListener(this)
            }
        }
        _state.value = VoiceSearchState(
            phase = VoiceSearchPhase.LISTENING,
            localeTag = localeTag,
        )
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
    }

    fun markRequestingPermission() {
        _state.value = _state.value.copy(phase = VoiceSearchPhase.REQUESTING_PERMISSION)
    }

    fun permissionDenied(permanently: Boolean) {
        fail(
            if (permanently) VoiceSearchError.PERMISSION_PERMANENTLY_DENIED
            else VoiceSearchError.PERMISSION_DENIED
        )
    }

    fun cancel() {
        recognizer?.cancel()
        _state.value = _state.value.copy(phase = VoiceSearchPhase.CANCELLED)
    }

    fun updateResult(text: String) {
        _state.value = _state.value.copy(
            phase = VoiceSearchPhase.RESULT,
            finalText = text,
            partialText = "",
            error = null,
        )
    }

    fun reset() {
        _state.value = VoiceSearchState(localeTag = _state.value.localeTag)
    }

    fun release() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        reset()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.value = _state.value.copy(phase = VoiceSearchPhase.LISTENING)
    }

    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() {
        _state.value = _state.value.copy(phase = VoiceSearchPhase.PROCESSING)
    }

    override fun onError(error: Int) {
        fail(
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceSearchError.PERMISSION_DENIED
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceSearchError.NETWORK
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceSearchError.NO_SPEECH
                SpeechRecognizer.ERROR_AUDIO -> VoiceSearchError.MICROPHONE_UNAVAILABLE
                SpeechRecognizer.ERROR_CLIENT -> {
                    if (_state.value.phase == VoiceSearchPhase.CANCELLED) return
                    VoiceSearchError.UNKNOWN
                }
                else -> VoiceSearchError.UNKNOWN
            }
        )
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (text.isBlank()) fail(VoiceSearchError.NO_SPEECH) else updateResult(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        _state.value = _state.value.copy(
            phase = VoiceSearchPhase.LISTENING,
            partialText = text,
        )
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun fail(error: VoiceSearchError) {
        _state.value = _state.value.copy(phase = VoiceSearchPhase.ERROR, error = error)
    }
}
