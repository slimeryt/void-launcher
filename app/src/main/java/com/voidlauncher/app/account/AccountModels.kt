package com.voidlauncher.app.account

enum class EnrollmentStatus(val storageKey: String) {
    None("none"),
    Pending("pending"),
    Approved("approved"),
    Denied("denied");

    companion object {
        fun fromStorage(key: String?): EnrollmentStatus =
            entries.firstOrNull { it.storageKey == key } ?: None
    }
}

/** Admin-gated Developer Account application status. */
enum class DeveloperAccountStatus(val storageKey: String) {
    None("none"),
    Pending("pending"),
    Approved("approved"),
    Denied("denied");

    companion object {
        fun fromStorage(key: String?): DeveloperAccountStatus =
            entries.firstOrNull { it.storageKey == key } ?: None
    }
}

data class AccountUser(
    val id: String,
    val email: String,
    val displayName: String,
    val developerAccountStatus: DeveloperAccountStatus,
    val isDeveloperAccount: Boolean,
    val enrollmentStatus: EnrollmentStatus,
    val developerEnrolled: Boolean
)

data class AuthResult(
    val token: String,
    val user: AccountUser
)
