package com.example.slk_verniy.worker

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class ReleaseInfo(
    val versionName: String,
    val releaseUrl: String,
    val releaseNotes: String?,
    val apkUrl: String?
)

object UpdateManager {
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/elifecomp/slk-vernyi/releases/latest"
    private val client = OkHttpClient()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadedBytes = MutableStateFlow<Long>(0L)
    val downloadedBytes = _downloadedBytes.asStateFlow()

    private val _totalBytes = MutableStateFlow<Long>(0L)
    val totalBytes = _totalBytes.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError = _downloadError.asStateFlow()

    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").removePrefix("V").trim()
        val cleanLatest = latest.removePrefix("v").removePrefix("V").trim()

        if (cleanCurrent == cleanLatest) return false

        val currParts = cleanCurrent.split(".", "-").mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split(".", "-").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(currParts.size, latestParts.size)) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return cleanLatest > cleanCurrent
    }

    fun checkForUpdate(currentVersion: String): ReleaseInfo? {
        return runCatching {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SLK-VERNIY-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val bodyString = response.body?.string() ?: return@runCatching null
                val json = JSONObject(bodyString)
                val tagName = json.optString("tag_name", json.optString("tagName", ""))
                val htmlUrl = json.optString("html_url", json.optString("htmlUrl", "https://github.com/elifecomp/slk-vernyi/releases"))
                val body = if (json.has("body")) json.optString("body") else null

                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i)
                        if (asset != null) {
                            val name = asset.optString("name", "")
                            val browserDownloadUrl = asset.optString("browser_download_url", "")
                            if (name.endsWith(".apk", ignoreCase = true) && browserDownloadUrl.isNotBlank()) {
                                apkUrl = browserDownloadUrl
                                break
                            }
                        }
                    }
                }
                if (apkUrl.isNullOrBlank() && tagName.isNotBlank()) {
                    apkUrl = "https://github.com/elifecomp/slk-vernyi/releases/download/$tagName/slk-vernyi.$tagName.apk"
                }

                if (tagName.isNotBlank() && isNewerVersion(currentVersion, tagName)) {
                    ReleaseInfo(
                        versionName = tagName,
                        releaseUrl = htmlUrl,
                        releaseNotes = body,
                        apkUrl = apkUrl
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    suspend fun downloadAndInstallApk(context: Context, releaseInfo: ReleaseInfo) {
        withContext(Dispatchers.IO) {
            val url = releaseInfo.apkUrl ?: run {
                _downloadError.value = "Ссылка на APK не найдена"
                return@withContext
            }
            _downloadProgress.value = 0f
            _downloadedBytes.value = 0L
            _totalBytes.value = 0L
            _downloadError.value = null

            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _downloadError.value = "Ошибка загрузки: ${response.code}"
                        _downloadProgress.value = null
                        return@withContext
                    }

                    val body = response.body
                    if (body == null) {
                        _downloadError.value = "Пустой ответ сервера"
                        _downloadProgress.value = null
                        return@withContext
                    }

                    val contentLength = body.contentLength()
                    _totalBytes.value = contentLength
                    val apkFile = File(context.cacheDir, "update.apk")
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }

                    body.byteStream().use { input ->
                        FileOutputStream(apkFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesCopied = 0L
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesCopied += bytesRead
                                _downloadedBytes.value = bytesCopied
                                if (contentLength > 0) {
                                    _downloadProgress.value = bytesCopied.toFloat() / contentLength.toFloat()
                                }
                            }
                        }
                    }

                    _downloadProgress.value = 1f

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    withContext(Dispatchers.Main) {
                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                _downloadError.value = e.localizedMessage ?: "Неизвестная ошибка скачивания"
                _downloadProgress.value = null
            }
        }
    }
}

object AppUpdateChecker {
    fun isNewerVersion(current: String, latest: String): Boolean =
        UpdateManager.isNewerVersion(current, latest)

    fun checkForUpdate(currentVersion: String): ReleaseInfo? =
        UpdateManager.checkForUpdate(currentVersion)
}
