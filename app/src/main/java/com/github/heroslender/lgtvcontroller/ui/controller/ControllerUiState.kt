package com.github.heroslender.lgtvcontroller.ui.controller

import com.github.heroslender.lgtvcontroller.device.DeviceControllerButton
import com.github.heroslender.lgtvcontroller.device.DeviceStatus

data class ControllerUiState(
    val deviceName: String? = null,
    val deviceStatus: DeviceStatus = DeviceStatus.DISCONNECTED,
)