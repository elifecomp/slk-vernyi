package com.example.slk_verniy.ui.main

import android.content.Intent
import android.net.Uri
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

@Composable
fun UpdateDialog(
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by UpdateManager.downloadProgress.collectAsState()
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

                if (isDownloading) {
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
                        Text(
                            text = if (currentProgress > 0f) "Загрузка: ${(currentProgress * 100).toInt()}%" else "Подготовка к загрузке...",
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
                if (!isDownloading) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.releaseUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("В браузере")
                    }
                }

                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch {
                            UpdateManager.downloadAndInstallApk(context, releaseInfo)
                            isDownloading = false
                        }
                        onUpdateClick()
                    },
                    enabled = !isDownloading
                ) {
                    Text(if (isDownloading) "Загрузка..." else "Обновить")
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
