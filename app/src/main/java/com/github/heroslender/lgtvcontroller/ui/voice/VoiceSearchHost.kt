package com.github.heroslender.lgtvcontroller.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.heroslender.lgtvcontroller.device.DeviceControllerButton

@Composable
fun VoiceSearchHost(
    isConnected: Boolean,
    isTextInputAvailable: Boolean,
    sendText: (String) -> Unit,
    sendEnter: () -> Unit,
    executeButton: (DeviceControllerButton) -> Unit,
    launchApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember { VoiceSearchManager(context) }
    val state by manager.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) manager.start() else manager.permissionDenied(permanently = false)
    }

    fun begin() {
        showDialog = true
        if (!isConnected) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            manager.start()
        } else {
            manager.markRequestingPermission()
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(lifecycleOwner, manager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) manager.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.release()
        }
    }

    fun completeAndDispatch() {
        val transcript = manager.completeNow() ?: return
        when (val action = VoiceCommandParser.parse(transcript)) {
            is VoiceAction.SearchText -> {
                if (isTextInputAvailable) {
                    manager.markSendingToTv(action.text)
                    sendText(action.text, {
                        sendEnter({
                            manager.markSuccess(action.text)
                        }, {
                            manager.updateResult(action.text)
                        })
                    }, {
                        manager.updateResult(action.text)
                    })
                } else manager.updateResult(action.text)
            }
            VoiceAction.VolumeUp -> {
                manager.markSendingToTv(transcript)
                executeButton(DeviceControllerButton.VOLUME_UP)
                manager.markSuccess(transcript)
            }
            VoiceAction.VolumeDown -> {
                manager.markSendingToTv(transcript)
                executeButton(DeviceControllerButton.VOLUME_DOWN)
                manager.markSuccess(transcript)
            }
            VoiceAction.Mute -> {
                manager.markSendingToTv(transcript)
                executeButton(DeviceControllerButton.MUTE)
                manager.markSuccess(transcript)
            }
            is VoiceAction.LaunchApp -> {
                manager.markSendingToTv(transcript)
                launchApp(action.appId)
                manager.markSuccess(transcript)
            }
        }
    }

    androidx.compose.material3.Surface(
        shape = ShapeDefaults.Large,
        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.pointerInput(isConnected) {
            if (!isConnected) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                    if (downEvent.changes.any { it.pressed }) {
                        scope.launch { begin() }
                    }
                    var isReleased = false
                    while (!isReleased) {
                        val upEvent = awaitPointerEvent(PointerEventPass.Initial)
                        if (upEvent.changes.all { !it.pressed }) {
                            isReleased = true
                            // If the gesture was released, stop recording immediately
                            scope.launch {
                                if (manager.state.value.phase == VoiceSearchPhase.LISTENING) {
                                    completeAndDispatch()
                                }
                            }
                        }
                    }
                }
            }
        },
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Voice Search")
        }
    }

    if (showDialog) {
        VoiceSearchDialog(
            state = if (isConnected) state else VoiceSearchState(
                phase = VoiceSearchPhase.ERROR,
                error = VoiceSearchError.TV_DISCONNECTED,
            ),
            onTextChanged = manager::updateResult,
            onRetry = ::begin,
            onCancel = {
                manager.cancel()
                showDialog = false
            },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                })
            },
            onCompleteListening = ::completeAndDispatch,
            onConfirm = { rawText ->
                val action = VoiceCommandParser.parse(rawText)
                when (action) {
                    is VoiceAction.SearchText -> {
                        if (isTextInputAvailable) {
                            manager.markSendingToTv(action.text)
                            sendText(action.text)
                            sendEnter()
                            manager.markSuccess(action.text)
                        } else {
                            manager.updateResult(action.text)
                        }
                    }
                    VoiceAction.VolumeUp -> executeButton(DeviceControllerButton.VOLUME_UP)
                    VoiceAction.VolumeDown -> executeButton(DeviceControllerButton.VOLUME_DOWN)
                    VoiceAction.Mute -> executeButton(DeviceControllerButton.MUTE)
                    is VoiceAction.LaunchApp -> launchApp(action.appId)
                }
                if (action !is VoiceAction.SearchText) {
                    manager.markSuccess(rawText)
                }
            },
        )
    }
}

@Composable
fun VoiceSearchDialog(
    state: VoiceSearchState,
    onTextChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    onCompleteListening: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "voicePulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "voicePulseScale",
    )
    val message = when (state.error) {
        VoiceSearchError.PERMISSION_DENIED -> "Quyền microphone đã bị từ chối."
        VoiceSearchError.PERMISSION_PERMANENTLY_DENIED -> "Hãy cấp quyền microphone trong Cài đặt."
        VoiceSearchError.RECOGNIZER_UNAVAILABLE -> "Không tìm thấy dịch vụ nhận dạng giọng nói."
        VoiceSearchError.MICROPHONE_UNAVAILABLE -> "Microphone đang không khả dụng."
        VoiceSearchError.NETWORK -> "Không thể nhận dạng giọng nói. Hãy kiểm tra mạng."
        VoiceSearchError.NO_SPEECH -> "Không nghe thấy giọng nói."
        VoiceSearchError.TV_DISCONNECTED -> "TV chưa kết nối."
        else -> "Không thể nhận dạng giọng nói."
    }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(if (state.phase == VoiceSearchPhase.LISTENING) pulse else 1f),
            )
        },
        title = { Text("Voice Search") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (state.phase) {
                    VoiceSearchPhase.REQUESTING_PERMISSION -> Text("Đang yêu cầu quyền microphone…")
                    VoiceSearchPhase.LISTENING -> {
                        Text("Đang lắng nghe…")
                        if (state.partialText.isNotBlank()) Text(state.partialText)
                    }
                    VoiceSearchPhase.PROCESSING -> {
                        Text("Đang xử lý…")
                        Text(state.displayText)
                    }
                    VoiceSearchPhase.SENDING_TO_TV -> {
                        Text("Đang gửi tới TV…")
                        Text(state.displayText)
                    }
                    VoiceSearchPhase.SUCCESS -> {
                        Text("Đã gửi tới TV")
                        Text("Chế độ Voice → Text → TV")
                        Text(state.displayText)
                    }
                    VoiceSearchPhase.RESULT -> OutlinedTextField(
                        value = state.finalText,
                        onValueChange = onTextChanged,
                        label = { Text("Nội dung nhận dạng") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VoiceSearchPhase.ERROR -> Text(message, color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (state.phase) {
                VoiceSearchPhase.LISTENING -> {
                    TextButton(
                        onClick = onCompleteListening,
                        enabled = state.displayText.isNotBlank(),
                    ) { Text("Hoàn thành") }
                }
                VoiceSearchPhase.RESULT -> {
                    TextButton(onClick = { onConfirm(state.finalText) }, enabled = state.finalText.isNotBlank()) {
                        Text("Gửi tới TV")
                    }
                }
                VoiceSearchPhase.ERROR -> TextButton(onClick = onRetry) { Text("Thử lại") }
                else -> Unit
            }
        },
        dismissButton = {
            if (state.error == VoiceSearchError.PERMISSION_PERMANENTLY_DENIED || state.error == VoiceSearchError.PERMISSION_DENIED) {
                TextButton(onClick = onOpenSettings) { Text("Mở Cài đặt") }
            }
            TextButton(onClick = onCancel) { Text("Hủy") }
        },
    )
}
