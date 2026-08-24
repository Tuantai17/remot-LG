package com.github.heroslender.lgtvcontroller.ui.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun VoiceSearchDialog(
    onSendToTv: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var recognizedText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasLaunched by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            recognizedText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            errorMessage = if (recognizedText.isBlank()) {
                "Không nhận được nội dung. Hãy thử nói lại."
            } else null
        } else if (recognizedText.isBlank()) {
            errorMessage = "Đã dừng nhận dạng giọng nói."
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói nội dung muốn tìm kiếm")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            errorMessage = null
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            errorMessage = "Điện thoại không có dịch vụ nhận dạng giọng nói."
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            startListening()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Tìm kiếm bằng giọng nói") },
        text = {
            Column {
                OutlinedTextField(
                    value = recognizedText,
                    onValueChange = { recognizedText = it },
                    label = { Text("Nội dung gửi tới TV") },
                )
                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = recognizedText.isNotBlank(),
                onClick = { onSendToTv(recognizedText.trim()) },
            ) {
                Text("Gửi tới TV")
            }
        },
        dismissButton = {
            TextButton(onClick = { startListening() }) { Text("Nói lại") }
            TextButton(onClick = onDismissRequest) { Text("Hủy") }
        },
    )
}
