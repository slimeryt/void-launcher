package com.voidlauncher.app.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val notes: String,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    data class Available(val release: ReleaseInfo) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class UpdateRepository(private val context: Context) {

    fun currentVersionName(): String {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "0"
        }.getOrDefault("0")
    }

    fun currentVersionCode(): Long {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        }.getOrDefault(0L)
    }

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatestRelease()
                ?: return@withContext UpdateCheckResult.Error("No release found on GitHub")

            val currentCode = currentVersionCode()
            val currentName = currentVersionName()

            val newer = when {
                release.versionCode > 0 -> release.versionCode > currentCode
                else -> compareSemVer(release.versionName, currentName) > 0
            }

            if (newer) UpdateCheckResult.Available(release)
            else UpdateCheckResult.UpToDate
        } catch (e: Exception) {
            UpdateCheckResult.Error(friendlyError(e))
        }
    }

    suspend fun downloadApk(
        url: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(dir, UpdateConfig.APK_ASSET_NAME)
        if (outFile.exists()) outFile.delete()

        val connection = openDownload(url)
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Download failed ($code)")
            }
            val total = connection.contentLengthLong.coerceAtLeast(0L)
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8192)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        readTotal += read
                        if (total > 0L) {
                            onProgress((readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
            // Reject HTML/error bodies that some hosts return as 200
            if (outFile.length() < 64_000L || !isZipApk(outFile)) {
                outFile.delete()
                throw IllegalStateException("Downloaded file is not a valid APK")
            }
            onProgress(1f)
            outFile
        } finally {
            connection.disconnect()
        }
    }

    /** Follow redirects manually — Android HttpURLConnection can mishandle GitHub CDN hops. */
    private fun openDownload(startUrl: String): HttpURLConnection {
        var current = startUrl
        repeat(8) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)
                setRequestProperty("Accept", "application/octet-stream,*/*")
            }
            connection.connect()
            val code = connection.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val next = connection.getHeaderField("Location")
                    ?: connection.getHeaderField("location")
                connection.disconnect()
                if (next.isNullOrBlank()) {
                    throw IllegalStateException("Download redirect missing Location")
                }
                current = if (next.startsWith("http")) next else URL(URL(current), next).toString()
                return@repeat
            }
            return connection
        }
        throw IllegalStateException("Too many download redirects")
    }

    private fun isZipApk(file: File): Boolean {
        return runCatching {
            file.inputStream().use { input ->
                val magic = ByteArray(4)
                if (input.read(magic) != 4) return false
                // ZIP / APK local file header: PK\x03\x04
                magic[0] == 0x50.toByte() &&
                    magic[1] == 0x4B.toByte() &&
                    magic[2] == 0x03.toByte() &&
                    magic[3] == 0x04.toByte()
            }
        }.getOrDefault(false)
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        // HTML redirect first — avoids api.github.com rate-limit 403s
        fetchViaLatestRedirect()?.let { return it }
        return fetchViaApi()
    }

    /**
     * `GET /releases/latest` → 302 to `/releases/tag/vX.Y.Z`.
     * No API quota; works for public repos.
     */
    private fun fetchViaLatestRedirect(): ReleaseInfo? {
        val connection = (URL(UpdateConfig.LATEST_HTML).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)
            setRequestProperty("Accept", "text/html")
        }
        try {
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
                ?: connection.getHeaderField("location")
            if (code !in listOf(301, 302, 303, 307, 308) || location.isNullOrBlank()) {
                return null
            }
            val tag = Regex("""/releases/tag/([^/?#]+)""")
                .find(location)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?: return null
            val versionName = tag.removePrefix("v").trim()
            if (versionName.isEmpty()) return null
            return ReleaseInfo(
                tagName = tag,
                versionName = versionName,
                // No release notes here — force semver compare (Gradle versionCode ≠ semver encode)
                versionCode = 0,
                apkUrl = UpdateConfig.downloadUrl(tag),
                notes = "",
                publishedAt = ""
            )
        } catch (_: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchViaApi(): ReleaseInfo? {
        val connection = (URL(UpdateConfig.LATEST_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            val code = connection.responseCode
            if (code == 403 || code == 429) {
                throw IllegalStateException(
                    "GitHub rate limit (API $code). Try again in a few minutes."
                )
            }
            if (code != 200) {
                val err = runCatching {
                    (connection.errorStream ?: connection.inputStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?.take(200)
                }.getOrNull()
                throw IllegalStateException(
                    buildString {
                        append("GitHub API $code")
                        if (!err.isNullOrBlank()) append(": ").append(err)
                    }
                )
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val notes = json.optString("body", "")
            val published = json.optString("published_at", "")
            val assets = json.optJSONArray("assets") ?: return null

            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.equals(UpdateConfig.APK_ASSET_NAME, ignoreCase = true) ||
                    name.endsWith(".apk", ignoreCase = true)
                ) {
                    apkUrl = asset.optString("browser_download_url", null)
                    if (name.equals(UpdateConfig.APK_ASSET_NAME, ignoreCase = true)) break
                }
            }
            if (apkUrl.isNullOrBlank()) {
                apkUrl = UpdateConfig.downloadUrl(tag)
            }

            val versionName = tag.removePrefix("v").trim()
            val versionCode = parseVersionCode(notes, versionName)

            return ReleaseInfo(
                tagName = tag,
                versionName = versionName,
                versionCode = versionCode,
                apkUrl = apkUrl,
                notes = notes,
                publishedAt = published
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            "403" in msg || "429" in msg || msg.contains("rate limit", ignoreCase = true) ->
                "GitHub rate limit — wait a bit, then Check again"
            msg.isNotBlank() -> msg
            else -> "Update check failed"
        }
    }

    /**
     * Prefer an explicit line in the release body:
     *   versionCode: 12
     * Otherwise derive a numeric code from semver (major*1_000_000 + minor*1_000 + patch).
     */
    private fun parseVersionCode(notes: String, versionName: String): Int {
        val match = Regex("""versionCode\s*[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(notes)
        if (match != null) return match.groupValues[1].toInt()

        val parts = versionName.split(".", "-", "_")
            .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        if (parts.isEmpty()) return 0
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        // Match our Gradle scheme: 0.2.6 → 1000+style was 1012; keep semver fallback
        // Prefer encoded patch ladder used in releases when notes missing:
        return major * 1_000_000 + minor * 1_000 + patch
    }

    private fun compareSemVer(a: String, b: String): Int {
        fun parts(v: String) = v.split(".", "-", "_").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val pa = parts(a)
        val pb = parts(b)
        val len = maxOf(pa.size, pb.size)
        for (i in 0 until len) {
            val d = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}
