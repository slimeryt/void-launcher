package com.voidlauncher.app.account

import com.voidlauncher.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AccountApi(
    private val baseUrl: String = BuildConfig.ACCOUNT_API_BASE.trimEnd('/')
) {
    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("displayName", displayName)
        parseAuth(postJson("/v1/auth/register", body))
    }

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
        parseAuth(postJson("/v1/auth/login", body))
    }

    suspend fun logout(token: String) = withContext(Dispatchers.IO) {
        runCatching { request("POST", "/v1/auth/logout", token = token) }
    }

    suspend fun me(token: String): AccountUser = withContext(Dispatchers.IO) {
        parseUser(request("GET", "/v1/me", token = token))
    }

    suspend fun requestDeveloperAccount(token: String): AccountUser = withContext(Dispatchers.IO) {
        parseUser(request("POST", "/v1/developer/request", token = token))
    }

    suspend fun requestEnroll(token: String): AccountUser = withContext(Dispatchers.IO) {
        parseUser(request("POST", "/v1/enroll/request", token = token))
    }

    private fun parseAuth(json: JSONObject): AuthResult {
        val token = json.optString("token", "")
        if (token.isBlank()) throw IllegalStateException("Missing session token")
        val userObj = json.optJSONObject("user")
            ?: throw IllegalStateException("Missing user")
        return AuthResult(token = token, user = parseUser(userObj))
    }

    private fun parseUser(json: JSONObject): AccountUser {
        val developerAccountStatus = DeveloperAccountStatus.fromStorage(
            json.optString("developerAccountStatus", "none")
        )
        val enrollmentStatus = EnrollmentStatus.fromStorage(
            json.optString("enrollmentStatus", "none")
        )
        val isDeveloperAccount = json.optBoolean(
            "isDeveloperAccount",
            developerAccountStatus == DeveloperAccountStatus.Approved
        )
        return AccountUser(
            id = json.optString("id", ""),
            email = json.optString("email", ""),
            displayName = json.optString("displayName", ""),
            developerAccountStatus = developerAccountStatus,
            isDeveloperAccount = isDeveloperAccount,
            enrollmentStatus = enrollmentStatus,
            developerEnrolled = json.optBoolean(
                "developerEnrolled",
                isDeveloperAccount && enrollmentStatus == EnrollmentStatus.Approved
            )
        )
    }

    private fun postJson(path: String, body: JSONObject, token: String? = null): JSONObject {
        return request("POST", path, body = body, token = token)
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject {
        if (baseUrl.isBlank() || baseUrl.contains("example.invalid")) {
            throw IllegalStateException(
                "Account API not configured. Set ACCOUNT_API_BASE to your Worker URL."
            )
        }
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Polar-Account/1.0")
            if (token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (code !in 200..299) {
                val msg = json.optString("error").ifBlank { "Request failed ($code)" }
                throw AccountApiException(code, msg)
            }
            return json
        } finally {
            connection.disconnect()
        }
    }
}

class AccountApiException(val httpCode: Int, message: String) : Exception(message)
