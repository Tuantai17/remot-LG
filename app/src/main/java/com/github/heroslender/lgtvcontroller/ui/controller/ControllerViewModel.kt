package com.github.heroslender.lgtvcontroller.ui.controller

import androidx.lifecycle.viewModelScope
import com.github.heroslender.lgtvcontroller.DeviceManager
import com.github.heroslender.lgtvcontroller.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    deviceManager: DeviceManager,
) : BaseViewModel(deviceManager) {
    val uiState: StateFlow<ControllerUiState> =
        deviceManager.connectedDevice.flatMapLatest { device ->
            if (device == null) {
                return@flatMapLatest flowOf(ControllerUiState())
            }

            device.state.map { deviceState ->
                ControllerUiState(
                    deviceName = if (deviceState.displayName.isNullOrEmpty()) device.friendlyName else deviceState.displayName,
                    deviceStatus = deviceState.status,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ControllerUiState())

    fun clickMouse() {
        deviceManager.connectedDevice.value?.mouseClick()
    }

    fun moveMouse(x: Double, y: Double) {
        deviceManager.connectedDevice.value?.moveMouse(x, y)
    }

    fun scroll(x: Double, y: Double) {
        deviceManager.connectedDevice.value?.scroll(x, y)
    }

    fun sendPin(pin: String) {
        deviceManager.sendPin(pin)
    }

    fun hasCapability(button: com.github.heroslender.lgtvcontroller.device.DeviceControllerButton): Boolean {
        return deviceManager.connectedDevice.value?.hasCapability(button) == true
    }

    fun executeButton(button: com.github.heroslender.lgtvcontroller.device.DeviceControllerButton) {
        deviceManager.connectedDevice.value?.executeControllerButton(button)
    }

    fun launchApp(appId: String) {
        deviceManager.connectedDevice.value?.launchApp(appId)
    }
}