package com.voidlauncher.app.account

import android.content.Context
import com.voidlauncher.app.data.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepository(
    context: Context,
    private val api: AccountApi = AccountApi()
) {
    private val prefs = PreferencesRepository(context)

    val session: Flow<AccountSession> = prefs.preferences.map { p ->
        AccountSession(
            token = p.accountToken,
            email = p.accountEmail,
            displayName = p.accountDisplayName,
            developerAccountStatus = DeveloperAccountStatus.fromStorage(p.developerAccountStatus),
            enrollmentStatus = EnrollmentStatus.fromStorage(p.enrollmentStatus),
            signedIn = p.accountToken.isNotBlank()
        )
    }

    suspend fun register(email: String, password: String, displayName: String): AccountUser {
        val result = api.register(email.trim(), password, displayName.trim())
        persist(result)
        return result.user
    }

    suspend fun login(email: String, password: String): AccountUser {
        val result = api.login(email.trim(), password)
        persist(result)
        return result.user
    }

    suspend fun logout() {
        val token = prefs.currentAccountToken()
        if (token.isNotBlank()) {
            runCatching { api.logout(token) }
        }
        prefs.clearAccount()
    }

    suspend fun refreshMe(): AccountUser? {
        val token = prefs.currentAccountToken()
        if (token.isBlank()) return null
        return try {
            val user = api.me(token)
            prefs.setAccountProfile(
                email = user.email,
                displayName = user.displayName,
                developerAccountStatus = user.developerAccountStatus.storageKey,
                enrollmentStatus = user.enrollmentStatus.storageKey
            )
            user
        } catch (e: AccountApiException) {
            if (e.httpCode == 401) prefs.clearAccount()
            throw e
        }
    }

    suspend fun requestDeveloperAccount(): AccountUser {
        val token = prefs.currentAccountToken()
        if (token.isBlank()) throw IllegalStateException("Sign in first")
        return try {
            val user = api.requestDeveloperAccount(token)
            prefs.setAccountProfile(
                email = user.email,
                displayName = user.displayName,
                developerAccountStatus = user.developerAccountStatus.storageKey,
                enrollmentStatus = user.enrollmentStatus.storageKey
            )
            user
        } catch (e: AccountApiException) {
            if (e.httpCode == 401) prefs.clearAccount()
            throw e
        }
    }

    suspend fun requestDeveloperEnrollment(): AccountUser {
        val token = prefs.currentAccountToken()
        if (token.isBlank()) throw IllegalStateException("Sign in first")
        return try {
            val user = api.requestEnroll(token)
            prefs.setAccountProfile(
                email = user.email,
                displayName = user.displayName,
                developerAccountStatus = user.developerAccountStatus.storageKey,
                enrollmentStatus = user.enrollmentStatus.storageKey
            )
            user
        } catch (e: AccountApiException) {
            if (e.httpCode == 401) prefs.clearAccount()
            throw e
        }
    }

    private suspend fun persist(result: AuthResult) {
        prefs.setAccountSession(
            token = result.token,
            email = result.user.email,
            displayName = result.user.displayName,
            developerAccountStatus = result.user.developerAccountStatus.storageKey,
            enrollmentStatus = result.user.enrollmentStatus.storageKey
        )
    }
}

data class AccountSession(
    val token: String = "",
    val email: String = "",
    val displayName: String = "",
    val developerAccountStatus: DeveloperAccountStatus = DeveloperAccountStatus.None,
    val enrollmentStatus: EnrollmentStatus = EnrollmentStatus.None,
    val signedIn: Boolean = false
) {
    val isDeveloperAccount: Boolean
        get() = developerAccountStatus == DeveloperAccountStatus.Approved
    val developerEnrolled: Boolean
        get() = isDeveloperAccount && enrollmentStatus == EnrollmentStatus.Approved
}
