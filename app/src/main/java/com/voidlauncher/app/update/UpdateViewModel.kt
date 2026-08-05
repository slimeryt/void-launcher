package com.voidlauncher.app.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val currentVersion: String = "",
    val statusMessage: String = "",
    val available: ReleaseInfo? = null,
    val downloadedApk: File? = null,
    val error: String? = null
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UpdateRepository(application)

    private val _state = MutableStateFlow(
        UpdateUiState(currentVersion = repo.currentVersionName())
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    checking = true,
                    error = null,
                    statusMessage = if (silent) it.statusMessage else "Checking GitHub…"
                )
            }
            when (val result = repo.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    _state.update {
                        it.copy(
                            checking = false,
                            available = result.release,
                            statusMessage = "Update ${result.release.versionName} available",
                            downloadedApk = null
                        )
                    }
                }
                UpdateCheckResult.UpToDate -> {
                    _state.update {
                        it.copy(
                            checking = false,
                            available = null,
                            statusMessage = "You're on the latest version",
                            downloadedApk = null
                        )
                    }
                }
                is UpdateCheckResult.Error -> {
                    _state.update {
                        it.copy(
                            checking = false,
                            error = result.message,
                            statusMessage = if (silent) it.statusMessage else "Check failed"
                        )
                    }
                }
            }
        }
    }

    fun downloadUpdate() {
        val release = _state.value.available ?: return
        viewModelScope.launch {
            _state.update {
                it.copy(downloading = true, progress = 0f, error = null, statusMessage = "Downloading…")
            }
            try {
                val file = repo.downloadApk(release.apkUrl) { p ->
                    _state.update { s -> s.copy(progress = p) }
                }
                _state.update {
                    it.copy(
                        downloading = false,
                        progress = 1f,
                        downloadedApk = file,
                        statusMessage = "Ready to install ${release.versionName}"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        downloading = false,
                        error = e.message ?: "Download failed",
                        statusMessage = "Download failed"
                    )
                }
            }
        }
    }
}
