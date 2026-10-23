package com.example.slk_verniy.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slk_verniy.worker.ReleaseInfo

@Composable
fun UpdateDialog(
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Доступно обновление ${releaseInfo.versionName}")
        },
        text = {
            Column {
                Text(text = "Вышла новая версия приложения ${releaseInfo.versionName}. Рекомендуем обновиться до актуальной версии.")
                if (!releaseInfo.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = releaseInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdateClick) {
                Text("Обновить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Позже")
            }
        }
    )
}
