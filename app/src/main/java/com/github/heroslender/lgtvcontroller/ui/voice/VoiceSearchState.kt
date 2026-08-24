package com.github.heroslender.lgtvcontroller.ui.voice

enum class VoiceSearchPhase {
    IDLE,
    REQUESTING_PERMISSION,
    LISTENING,
    PROCESSING,
    RESULT,
    SEARCHING,
    SUCCESS,
    ERROR,
    CANCELLED,
}

enum class VoiceSearchError {
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    RECOGNIZER_UNAVAILABLE,
    MICROPHONE_UNAVAILABLE,
    NETWORK,
    NO_SPEECH,
    TV_DISCONNECTED,
    TV_TEXT_INPUT_UNAVAILABLE,
    UNKNOWN,
}

sealed interface VoiceAction {
    data class SearchText(val text: String) : VoiceAction
    data object VolumeUp : VoiceAction
    data object VolumeDown : VoiceAction
    data object Mute : VoiceAction
    data class LaunchApp(val appId: String) : VoiceAction
}

enum class VoiceDeliveryMode {
    TEXT_INPUT_FALLBACK,
}

data class VoiceSearchState(
    val phase: VoiceSearchPhase = VoiceSearchPhase.IDLE,
    val partialText: String = "",
    val finalText: String = "",
    val error: VoiceSearchError? = null,
    val localeTag: String = DEFAULT_VOICE_LOCALE,
    val deliveryMode: VoiceDeliveryMode? = null,
) {
    val displayText: String
        get() = finalText.ifBlank { partialText }
}

const val DEFAULT_VOICE_LOCALE = "vi-VN"
