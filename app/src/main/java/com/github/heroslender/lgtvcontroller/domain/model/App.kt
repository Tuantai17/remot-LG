package com.github.heroslender.lgtvcontroller.domain.model

data class App(
    /**
     * App ID
     */
    val id: String,
    /**
     * App title
     */
    val name: String,
    /**
     * App icon URL hosted by the TV, requires ssl check to be disabled
     */
    val icon: String = "",
    /**
     * Timestamp the app was installed, 0 for pre-installed apps
     */
    val installedTime: Long = 0,
)
