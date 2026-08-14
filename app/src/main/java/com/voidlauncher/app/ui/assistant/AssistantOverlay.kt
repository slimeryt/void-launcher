package com.voidlauncher.app.ui.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.delay

private val AssistantShellBg = Color(0xF2141418)
private val AssistantInputBg = Color(0xFF2A2D36)
private val AssistantShellShape = RoundedCornerShape(28.dp)

private enum class AssistantUiMode { Listening, Keyboard }

@Composable
fun AssistantOverlay(
    visible: Boolean,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onSearchApps: (query: String) -> Unit,
    onOpenSettings: () -> Unit,
    onEditHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val context = LocalContext.current
    var mode by remember { mutableStateOf(AssistantUiMode.Listening) }
    var answer by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var listeningLabel by remember { mutableStateOf("Listening...") }
    var voiceSession by remember { mutableStateOf(0) }

    fun handleCommand(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        when (val action = parseAssistantCommand(trimmed, apps)) {
            is AssistantAction.Reply -> answer = action.message
            is AssistantAction.LaunchApp -> {
                answer = action.message
                onLaunchApp(action.app)
            }
            is AssistantAction.OpenDrawer -> {
                answer = action.message
                onOpenDrawer()
            }
            is AssistantAction.SearchApps -> {
                answer = action.message
                onSearchApps(action.query)
            }
            is AssistantAction.OpenSettings -> {
                answer = action.message
                onOpenSettings()
            }
            is AssistantAction.OpenSystemSettings -> {
                answer = action.message
                runCatching {
                    context.startActivity(
                        Intent(action.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            is AssistantAction.EditHome -> {
                answer = action.message
                onEditHome()
            }
            AssistantAction.Dismiss -> onDismiss()
        }
    }

    LaunchedEffect(answer) {
        val a = answer ?: return@LaunchedEffect
        if (a.startsWith("Opening") || a.startsWith("Searching") || a.startsWith("Entering")) {
            delay(900)
            onDismiss()
        } else if (
            mode == AssistantUiMode.Listening &&
            a.startsWith("I'm not sure")
        ) {
            delay(2_000)
            answer = null
            voiceSession++
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Black shell wrapping the whole assistant chrome
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 88.dp)
                .clip(AssistantShellShape)
                .background(AssistantShellBg)
                .animateContentSize(animationSpec = tween(220))
                .heightIn(min = 88.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* consume */ }
                )
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                },
                label = "assistantMode",
                modifier = Modifier.fillMaxWidth()
            ) { uiMode ->
                when (uiMode) {
                    AssistantUiMode.Listening -> {
                        ListeningContent(
                            label = answer ?: listeningLabel,
                            onKeyboard = {
                                answer = null
                                mode = AssistantUiMode.Keyboard
                            }
                        )
                    }
                    AssistantUiMode.Keyboard -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (answer != null) {
                                Text(
                                    text = answer!!,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 21.sp
                                    ),
                                    color = VoidMist,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            KeyboardRow(
                                value = input,
                                onValueChange = { input = it },
                                onSend = {
                                    val q = input.trim()
                                    if (q.isNotEmpty()) {
                                        handleCommand(q)
                                        input = ""
                                    }
                                },
                                onVoice = {
                                    answer = null
                                    input = ""
                                    mode = AssistantUiMode.Listening
                                    voiceSession++
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mode == AssistantUiMode.Listening) {
        VoiceListener(
            session = voiceSession,
            onListening = { listeningLabel = "Listening..." },
            onPartial = { listeningLabel = it.ifBlank { "Listening..." } },
            onResult = { spoken ->
                listeningLabel = "Listening..."
                handleCommand(spoken)
            },
            onError = { msg ->
                listeningLabel = "Listening..."
                answer = msg
            }
        )
    }
}

@Composable
private fun ListeningContent(
    label: String,
    onKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            color = VoidMist,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 44.dp)
        )
        Icon(
            imageVector = Icons.Rounded.Keyboard,
            contentDescription = "Keyboard",
            tint = VoidMist,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(28.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onKeyboard
                )
        )
    }
}

@Composable
private fun KeyboardRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(CapsuleShape)
                .background(AssistantInputBg)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Voice",
                tint = VoidMist,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onVoice
                    )
                    .padding(11.dp)
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = VoidMist,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(VoidMist),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 4.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Ask Polar…",
                                color = VoidMuted,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                }
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Send",
            tint = VoidMist,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSend
                )
                .padding(12.dp)
        )
    }
}

@Composable
private fun VoiceListener(
    session: Int,
    onListening: () -> Unit,
    onPartial: (String) -> Unit,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var permitted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permitted = granted
        if (!granted) {
            onError("Microphone permission is needed for voice. Use the keyboard instead.")
        }
    }

    LaunchedEffect(Unit) {
        if (!permitted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(session, permitted) {
        if (!permitted) {
            return@DisposableEffect onDispose { }
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Voice isn’t available on this device. Use the keyboard instead.")
            return@DisposableEffect onDispose { }
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onListening()
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!text.isNullOrBlank()) onPartial(text)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!text.isNullOrBlank()) {
                    onResult(text)
                } else {
                    onError("I'm not sure I understand, could you try again?")
                }
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        onError("I'm not sure I understand, could you try again?")
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        onError("Microphone permission is needed for voice. Use the keyboard instead.")
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Unit
                    else -> onError("I'm not sure I understand, could you try again?")
                }
            }
        })

        onListening()
        runCatching { recognizer.startListening(intent) }

        onDispose {
            runCatching {
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            }
        }
    }
}
