package com.gopspawar.jarvisqa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(val text: String, val fromJarvis: Boolean)
enum class AgentMode(val label: String) { PERSONAL("Personal"), QA("QA Expert") }

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { JarvisApp(::speak) }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "IN")
            tts?.setSpeechRate(0.92f)
            tts?.setPitch(0.96f)
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-response")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun JarvisApp(speak: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navy = Color(0xFF07111F)
    val cyan = Color(0xFF3DE6FF)
    var mode by remember { mutableStateOf(AgentMode.PERSONAL) }
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(true) }
    val messages = remember {
        mutableStateListOf(ChatMessage("Good day, Gopal. Jarvis is online. How may I assist you?", true))
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!heard.isNullOrBlank()) input = heard
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchSpeech(speechLauncher::launch) else listening = false
    }

    fun send() {
        val prompt = input.trim()
        if (prompt.isEmpty()) return
        messages += ChatMessage(prompt, false)
        val response = JarvisEngine.respond(prompt, mode)
        messages += ChatMessage(response, true)
        input = ""
        if (voiceEnabled) speak(response)
        scope.launch { listState.animateScrollToItem(messages.lastIndex) }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(primary = cyan, background = navy, surface = Color(0xFF0D1C2C))
    ) {
        Scaffold(containerColor = navy) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).background(
                    Brush.verticalGradient(listOf(Color(0xFF07111F), Color(0xFF0B2033)))
                ).padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("J.A.R.V.I.S.", color = cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("JUST A RESPONSIVE VIRTUAL INTELLIGENCE SYSTEM", color = Color.Gray, fontSize = 8.sp)
                    }
                    Switch(checked = voiceEnabled, onCheckedChange = { voiceEnabled = it })
                    Text("Voice", fontSize = 12.sp, color = Color.LightGray)
                }

                Spacer(Modifier.height(12.dp))
                AgentSelector(mode) { mode = it }
                Spacer(Modifier.height(14.dp))
                JarvisCore(listening, cyan)
                Text(
                    if (listening) "LISTENING…" else "SYSTEM ONLINE • ${mode.label.uppercase()} AGENT",
                    color = if (listening) Color(0xFFFFD166) else cyan,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { items(messages) { MessageBubble(it, cyan) } }

                Row(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask Jarvis…") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (listening) { listening = false; return@FilledIconButton }
                            listening = true
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) launchSpeech(speechLauncher::launch)
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = cyan)
                    ) { Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, null, tint = navy) }
                    FilledIconButton(onClick = ::send) { Icon(Icons.Default.Send, "Send") }
                }
            }
        }
    }
}

private fun launchSpeech(launch: (Intent) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Jarvis")
    }
    launch(intent)
}

@Composable
private fun AgentSelector(selected: AgentMode, onSelect: (AgentMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AgentMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun JarvisCore(listening: Boolean, cyan: Color) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (listening) 450 else 1300), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(Modifier.fillMaxWidth().height(105.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(94.dp).alpha(pulse).border(2.dp, cyan.copy(alpha = .5f), CircleShape))
        Box(Modifier.size(72.dp).border(3.dp, cyan, CircleShape).background(cyan.copy(alpha = .12f), CircleShape))
        Text("J", color = cyan, fontSize = 34.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, cyan: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromJarvis) Arrangement.Start else Arrangement.End) {
        Surface(
            color = if (message.fromJarvis) Color(0xFF102A3E) else cyan.copy(alpha = .18f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(.86f).clickable(enabled = false) {}
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(if (message.fromJarvis) "JARVIS" else "YOU", color = cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(message.text, color = Color(0xFFEAF7FF), fontSize = 14.sp)
            }
        }
    }
}

object JarvisEngine {
    fun respond(input: String, mode: AgentMode): String {
        val q = input.lowercase(Locale.getDefault())
        return when (mode) {
            AgentMode.PERSONAL -> when {
                "time" in q -> "It is ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."
                "date" in q || "day" in q -> "Today is ${SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())}."
                "hello" in q || "hey" in q -> "Hello, Gopal. All systems are operational."
                "gym" in q || "workout" in q -> "Your usual gym window is 6 to 7 AM. Consistency first; intensity follows."
                "plan" in q || "todo" in q -> "Tell me the tasks and priorities. I will turn them into a focused action list."
                else -> "I understand. This offline build handles core commands. Connect an AI provider in Settings in the next release for open-ended assistance."
            }
            AgentMode.QA -> when {
                "test case" in q -> "Use this structure: test ID, objective, preconditions, data, steps, expected result, priority, and automation status. Tell me the feature to generate specific cases."
                "bug" in q || "defect" in q -> "A strong defect includes a concise title, environment, prerequisites, exact reproduction steps, expected versus actual result, severity, evidence, and reproducibility."
                "api" in q -> "Validate status codes, schema, headers, authentication, positive and negative data, boundaries, idempotency, latency, and error contracts."
                "selenium" in q -> "Prefer stable IDs or data-test attributes, explicit waits, Page Objects, isolated test data, and screenshots on failure. Avoid hard sleeps."
                "playwright" in q -> "Use role or test-id locators, auto-waiting, storage state for authentication, independent fixtures, traces on retry, and HTML reports."
                "regression" in q -> "Prioritize business-critical flows, recently changed areas, integrations, past defect clusters, and cross-browser risk. Keep smoke coverage separate."
                "interview" in q -> "I can conduct a QA mock interview. Say manual, automation, API, SQL, Selenium, Playwright, or Java to begin."
                else -> "QA agent ready. Ask me for test cases, a test plan, defect analysis, automation design, API checks, SQL validation, or interview practice."
            }
        }
    }
}
