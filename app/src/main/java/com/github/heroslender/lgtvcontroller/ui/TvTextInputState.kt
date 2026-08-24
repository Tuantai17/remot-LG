package com.github.heroslender.lgtvcontroller.ui

data class TvTextInputState(
    val isKeyboardOpen: Boolean = false,
    val sendBackspace: () -> Unit = {},
    val sendEnter: (onSuccess: (() -> Unit)?, onError: ((Exception) -> Unit)?) -> Unit = { _, _ -> },
    val sendText: (String, onSuccess: (() -> Unit)?, onError: ((Exception) -> Unit)?) -> Unit = { _, _, _ -> },
)
