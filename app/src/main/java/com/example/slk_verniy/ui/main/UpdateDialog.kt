package com.example.slk_verniy.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.slk_verniy.worker.ReleaseInfo
import com.example.slk_verniy.worker.UpdateManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun UpdateDialog(
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by UpdateManager.downloadProgress.collectAsState()
    val downloadedBytes by UpdateManager.downloadedBytes.collectAsState()
    val totalBytes by UpdateManager.totalBytes.collectAsState()
    val error by UpdateManager.downloadError.collectAsState()
    var isDownloading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        title = {
            Text(text = "Доступно обновление ${releaseInfo.versionName}")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Вышла новая версия приложения ${releaseInfo.versionName}. Рекомендуем обновиться до актуальной версии.")
                if (!releaseInfo.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = releaseInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isDownloading || progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val currentProgress = progress ?: 0f
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val downloadedMb = downloadedBytes / (1024.0 * 1024.0)
                        val totalMb = totalBytes / (1024.0 * 1024.0)
                        val percent = (currentProgress * 100).toInt().coerceIn(0, 100)

                        val progressText = if (totalBytes > 0) {
                            String.format(Locale.ROOT, "Загрузка: %d%% (%.1f MB из %.1f MB)", percent, downloadedMb, totalMb)
                        } else if (downloadedBytes > 0) {
                            String.format(Locale.ROOT, "Загрузка: %.1f MB", downloadedMb)
                        } else if (currentProgress > 0f) {
                            String.format(Locale.ROOT, "Загрузка: %d%%", percent)
                        } else {
                            "Подготовка к загрузке..."
                        }

                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ошибка: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch {
                            UpdateManager.downloadAndInstallApk(context, releaseInfo)
                            isDownloading = false
                            onDismiss()
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text(if (isDownloading) "Загрузка..." else "Скачать")
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Позже")
                }
            }
        }
    )
}
