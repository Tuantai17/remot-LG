package com.github.heroslender.lgtvcontroller.ui.voice

import java.util.Locale

object VoiceCommandParser {
    fun parse(text: String): VoiceAction {
        val normalized = text.trim().lowercase(Locale.forLanguageTag(DEFAULT_VOICE_LOCALE))
        return when (normalized) {
            "tăng âm lượng", "tăng volume", "âm lượng lên" -> VoiceAction.VolumeUp
            "giảm âm lượng", "giảm volume", "âm lượng xuống" -> VoiceAction.VolumeDown
            "tắt tiếng", "im lặng", "mute" -> VoiceAction.Mute
            "mở youtube", "youtube" -> VoiceAction.LaunchApp("youtube.leanback.v4")
            "mở netflix", "netflix" -> VoiceAction.LaunchApp("netflix")
            else -> VoiceAction.SearchText(text.trim())
        }
    }
}
