package com.voidlauncher.app.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val signedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val developerAccountStatus: DeveloperAccountStatus = DeveloperAccountStatus.None,
    val isDeveloperAccount: Boolean = false,
    val enrollmentStatus: EnrollmentStatus = EnrollmentStatus.None,
    val developerEnrolled: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AccountRepository(application)

    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val info = MutableStateFlow<String?>(null)

    val state: StateFlow<AccountUiState> = combine(
        repo.session,
        busy,
        error,
        info
    ) { session, isBusy, err, msg ->
        AccountUiState(
            signedIn = session.signedIn,
            email = session.email,
            displayName = session.displayName,
            developerAccountStatus = session.developerAccountStatus,
            isDeveloperAccount = session.isDeveloperAccount,
            enrollmentStatus = session.enrollmentStatus,
            developerEnrolled = session.developerEnrolled,
            busy = isBusy,
            error = err,
            info = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountUiState()
    )

    init {
        refresh()
    }

    fun clearMessages() {
        error.value = null
        info.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { repo.refreshMe() }
                .onFailure { e ->
                    if (e is AccountApiException && e.httpCode != 401) {
                        error.value = e.message
                    }
                }
            busy.value = false
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            info.value = null
            runCatching {
                repo.register(email, password, displayName)
                info.value = "Account created"
            }.onFailure { e ->
                error.value = e.message ?: "Could not create account"
            }
            busy.value = false
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            info.value = null
            runCatching {
                repo.login(email, password)
                info.value = "Signed in"
            }.onFailure { e ->
                error.value = e.message ?: "Could not sign in"
            }
            busy.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            runCatching { repo.logout() }
            info.value = "Signed out"
            busy.value = false
        }
    }

    fun requestDeveloperAccount() {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            info.value = null
            runCatching {
                val user = repo.requestDeveloperAccount()
                info.value = when (user.developerAccountStatus) {
                    DeveloperAccountStatus.Pending -> "Developer Account requested — awaiting approval"
                    DeveloperAccountStatus.Approved -> "You have a Developer Account"
                    else -> "Developer Account updated"
                }
            }.onFailure { e ->
                error.value = e.message ?: "Could not request Developer Account"
            }
            busy.value = false
        }
    }

    fun requestDeveloperEnrollment() {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            info.value = null
            runCatching {
                val user = repo.requestDeveloperEnrollment()
                info.value = if (user.developerEnrolled) {
                    "Enrolled in Developer Beta"
                } else {
                    "Enrollment updated"
                }
            }.onFailure { e ->
                error.value = e.message ?: "Could not enroll"
            }
            busy.value = false
        }
    }
}
