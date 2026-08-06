package com.voidlauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import com.voidlauncher.app.account.AccountUiState
import com.voidlauncher.app.account.DeveloperAccountStatus
import com.voidlauncher.app.account.EnrollmentStatus
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted

private val CardShape = SettingsCardShape
private val PillFill = Color(0xFF2C2C2E)
private val PillStroke = Color.White.copy(alpha = 0.08f)

@Composable
private fun IosPillField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(
            color = VoidMist,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal
        ),
        cursorBrush = SolidColor(IosBlue),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CapsuleShape)
            .background(PillFill)
            .border(1.dp, PillStroke, CapsuleShape)
            .padding(horizontal = 18.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) {
                    Text(placeholder, color = VoidMuted, style = MaterialTheme.typography.bodyLarge)
                }
                inner()
            }
        }
    )
}

@Composable
fun AccountScreen(
    accountState: AccountUiState,
    onBack: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, password: String, displayName: String) -> Unit,
    onLogout: () -> Unit,
    onRequestDeveloperAccount: () -> Unit,
    onRequestEnroll: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var modeCreate by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidInk)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsBackBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sign in for Public Beta. Become a Developer to enroll in Developer builds.",
                style = MaterialTheme.typography.bodyMedium,
                color = VoidMuted
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (accountState.signedIn) {
                SignedInCard(
                    state = accountState,
                    busy = accountState.busy,
                    onLogout = onLogout,
                    onRequestDeveloperAccount = onRequestDeveloperAccount,
                    onRequestEnroll = onRequestEnroll,
                    onRefresh = onRefresh
                )
            } else {
                AuthForm(
                    modeCreate = modeCreate,
                    email = email,
                    password = password,
                    displayName = displayName,
                    busy = accountState.busy,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onDisplayNameChange = { displayName = it },
                    onToggleMode = { modeCreate = !modeCreate },
                    onSubmit = {
                        if (modeCreate) onRegister(email, password, displayName)
                        else onLogin(email, password)
                    }
                )
            }

            accountState.error?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(it, color = Color(0xFFF87171), style = MaterialTheme.typography.bodyMedium)
            }
            accountState.info?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(it, color = IosBlue, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SignedInCard(
    state: AccountUiState,
    busy: Boolean,
    onLogout: () -> Unit,
    onRequestDeveloperAccount: () -> Unit,
    onRequestEnroll: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SettingsCardBg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = state.displayName.ifBlank { state.email.substringBefore("@") },
            style = MaterialTheme.typography.titleLarge,
            color = VoidMist,
            fontWeight = FontWeight.SemiBold
        )
        Text(state.email, style = MaterialTheme.typography.bodyLarge, color = VoidMuted)

        val accountLabel = when (state.developerAccountStatus) {
            DeveloperAccountStatus.None -> "Standard"
            DeveloperAccountStatus.Pending -> "Pending approval"
            DeveloperAccountStatus.Approved -> "Developer"
            DeveloperAccountStatus.Denied -> "Not approved"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Developer Account", color = VoidMuted, style = MaterialTheme.typography.bodyLarge)
            Text(
                accountLabel,
                color = if (state.isDeveloperAccount) IosBlue else VoidMist,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        if (state.isDeveloperAccount) {
            val enrollLabel = if (state.developerEnrolled) "Enrolled" else "Not enrolled"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Developer Beta", color = VoidMuted, style = MaterialTheme.typography.bodyLarge)
                Text(
                    enrollLabel,
                    color = if (state.developerEnrolled) IosBlue else VoidMist,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        when {
            state.developerAccountStatus == DeveloperAccountStatus.None ||
                state.developerAccountStatus == DeveloperAccountStatus.Denied -> {
                PrimaryButton(
                    label = if (state.developerAccountStatus == DeveloperAccountStatus.Denied) {
                        "Request Developer Account again"
                    } else {
                        "Become a Developer"
                    },
                    enabled = !busy,
                    onClick = onRequestDeveloperAccount
                )
                Text(
                    "A Developer Account lets you enroll in early Developer Beta builds.",
                    color = VoidMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.developerAccountStatus == DeveloperAccountStatus.Pending -> {
                Text(
                    "Your Developer Account request is awaiting approval.",
                    color = VoidMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.isDeveloperAccount && !state.developerEnrolled -> {
                PrimaryButton(
                    label = "Enroll in Developer Beta",
                    enabled = !busy,
                    onClick = onRequestEnroll
                )
                Text(
                    "Enrolling unlocks Developer in Settings → Updates → Beta Updates.",
                    color = VoidMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.developerEnrolled -> {
                Text(
                    "You can select Developer in Settings → Updates → Beta Updates.",
                    color = VoidMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SecondaryButton(
                    label = "Refresh",
                    enabled = !busy,
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecondaryButton(
                    label = "Sign out",
                    enabled = !busy,
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = IosBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthForm(
    modeCreate: Boolean,
    email: String,
    password: String,
    displayName: String,
    busy: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SettingsCardBg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (modeCreate) "Create Account" else "Sign In",
            style = MaterialTheme.typography.titleMedium,
            color = VoidMist,
            fontWeight = FontWeight.SemiBold
        )

        if (modeCreate) {
            IosPillField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                placeholder = "Name",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        IosPillField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Email",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        IosPillField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (!busy) onSubmit() })
        )

        PrimaryButton(
            label = if (modeCreate) "Create Account" else "Sign In",
            enabled = !busy && email.isNotBlank() && password.length >= 8,
            onClick = onSubmit
        )

        Text(
            text = if (modeCreate) {
                "Already have an account? Sign in"
            } else {
                "Need an account? Create one"
            },
            color = IosBlue,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable(enabled = !busy, onClick = onToggleMode)
                .padding(vertical = 4.dp)
        )

        if (busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = IosBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CapsuleShape)
            .background(if (enabled) IosBlue else IosBlue.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = VoidMist, style = MaterialTheme.typography.bodyLarge)
    }
}
