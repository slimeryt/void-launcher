package com.voidlauncher.app.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voidlauncher.app.data.PreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    val error: String? = null,
    val channel: UpdateChannel = UpdateChannel.Off,
    val developerEnrolled: Boolean = false,
    val agreedPublicBeta: Boolean = false,
    val agreedDeveloperBeta: Boolean = false
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UpdateRepository(application)
    private val prefs = PreferencesRepository(application)

    private val _state = MutableStateFlow(
        UpdateUiState(currentVersion = repo.currentVersionName())
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            val p = prefs.preferences.first()
            val channel = UpdateChannel.fromStorage(p.updateChannel)
            val enrolled = p.enrollmentStatus == "approved"
            val safeChannel =
                if (channel == UpdateChannel.Developer && !enrolled) UpdateChannel.Off else channel
            if (safeChannel != channel) {
                prefs.setUpdateChannel(safeChannel.storageKey)
            }
            _state.update {
                it.copy(
                    channel = safeChannel,
                    developerEnrolled = enrolled,
                    agreedPublicBeta = p.agreedPublicBeta,
                    agreedDeveloperBeta = p.agreedDeveloperBeta
                )
            }
            checkForUpdates(silent = true)
        }

        viewModelScope.launch {
            prefs.preferences
                .map { p ->
                    Triple(
                        p.enrollmentStatus == "approved",
                        UpdateChannel.fromStorage(p.updateChannel),
                        p.agreedPublicBeta to p.agreedDeveloperBeta
                    )
                }
                .distinctUntilChanged()
                .collect { (enrolled, channel, agreed) ->
                    _state.update {
                        it.copy(
                            developerEnrolled = enrolled,
                            agreedPublicBeta = agreed.first,
                            agreedDeveloperBeta = agreed.second
                        )
                    }
                    if (channel == UpdateChannel.Developer && !enrolled) {
                        prefs.setUpdateChannel(UpdateChannel.Off.storageKey)
                        _state.update {
                            it.copy(
                                channel = UpdateChannel.Off,
                                available = null,
                                downloadedApk = null,
                                statusMessage = "Developer enrollment required — switched to Off"
                            )
                        }
                        checkForUpdates(silent = true)
                    }
                }
        }
    }

fun setUpdateChannel(channel: UpdateChannel) {
        if (_state.value.channel == channel) return
        viewModelScope.launch {
            if (channel == UpdateChannel.Developer) {
                val enrolled = prefs.preferences.first().enrollmentStatus == "approved"
                if (!enrolled) {
                    _state.update {
                        it.copy(error = "Developer Beta requires an approved enrollment")
                    }
                    return@launch
                }
            }
            // First successful enable of a beta channel counts as agreement.
            if (channel == UpdateChannel.PublicBeta || channel == UpdateChannel.Developer) {
                prefs.setBetaChannelAgreed(channel.storageKey)
            }
            prefs.setUpdateChannel(channel.storageKey)
            _state.update {
                it.copy(
                    channel = channel,
                    available = null,
                    downloadedApk = null,
                    error = null,
                    statusMessage = "Switching to ${channel.label}…",
                    agreedPublicBeta = it.agreedPublicBeta || channel == UpdateChannel.PublicBeta,
                    agreedDeveloperBeta = it.agreedDeveloperBeta || channel == UpdateChannel.Developer
                )
            }
            checkForUpdates(silent = false)
        }
    }

    /** Persist agreement without switching channel (e.g. download confirm). */
    fun markBetaChannelAgreed(channel: UpdateChannel) {
        if (channel == UpdateChannel.Off) return
        viewModelScope.launch {
            prefs.setBetaChannelAgreed(channel.storageKey)
            _state.update {
                when (channel) {
                    UpdateChannel.PublicBeta -> it.copy(agreedPublicBeta = true)
                    UpdateChannel.Developer -> it.copy(agreedDeveloperBeta = true)
                    UpdateChannel.Off -> it
                }
            }
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            val channel = _state.value.channel
            _state.update {
                it.copy(
                    checking = true,
                    error = null,
                    statusMessage = if (silent) it.statusMessage else "Checking for updates…"
                )
            }
            when (val result = repo.checkForUpdate(channel)) {
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
                            statusMessage = when (channel) {
                                UpdateChannel.Off -> "You're on the latest version"
                                UpdateChannel.PublicBeta -> "You're on the latest Public Beta"
                                UpdateChannel.Developer -> "You're on the latest Developer build"
                            },
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

    fun cancelUpdateAction() {
        checkJob?.cancel()
        checkJob = null
        downloadJob?.cancel()
        downloadJob = null
        _state.update {
            it.copy(
                checking = false,
                downloading = false,
                progress = 0f,
                statusMessage = if (it.available != null) {
                    "Update ${it.available.versionName} available"
                } else {
                    "Check cancelled"
                },
                error = null
            )
        }
    }

    fun downloadUpdate() {
        val release = _state.value.available ?: return
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
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
                if (e is CancellationException) throw e
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
