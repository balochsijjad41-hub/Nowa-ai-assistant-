package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.AssistantMode
import com.example.system.DeviceController
import com.example.system.DevicePermissionReport
import com.example.ui.components.NovaAnimatedOrb
import com.example.ui.components.NovaGlassCard
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaOledBlack
import com.example.ui.theme.NovaViolet
import com.example.viewmodel.NovaRuntimeState
import com.example.viewmodel.NovaViewModel

@Composable
fun DrivingModeOverlay(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    onOrbTap: () -> Unit
) {
    BackHandler { viewModel.setAssistantMode(AssistantMode.NORMAL) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NovaOledBlack, Color(0xFF0A1228), NovaOledBlack))
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("DRIVING MODE", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.setAssistantMode(AssistantMode.NORMAL) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Exit", style = MaterialTheme.typography.titleMedium)
                }
            }

            NovaAnimatedOrb(
                state = runtime.orbState,
                amplitude = runtime.voiceAmplitude,
                size = 240.dp,
                onClick = onOrbTap
            )

            Text(
                text = runtime.replyText,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 3
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DrivingBigButton("Call", Icons.Default.Call, NovaCyan, Modifier.weight(1f)) {
                        viewModel.submitCommand("Call Mom on speaker")
                    }
                    DrivingBigButton("Read SMS", Icons.Default.Message, NovaViolet, Modifier.weight(1f)) {
                        viewModel.submitCommand("Read my unread messages aloud")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DrivingBigButton("Navigate", Icons.Default.Navigation, NovaEmerald, Modifier.weight(1f)) {
                        viewModel.submitCommand("Navigate home via fastest route")
                    }
                    DrivingBigButton("Music", Icons.Default.MusicNote, NovaMagenta, Modifier.weight(1f)) {
                        viewModel.submitCommand("Play my driving playlist on Spotify")
                    }
                }
            }
        }
    }
}

@Composable
private fun DrivingBigButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.22f), contentColor = Color.White),
        border = BorderStroke(1.5.dp, accent),
        modifier = modifier.height(78.dp)
    ) {
        Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VisionAndTranslateDialog(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var targetLanguage by remember { mutableStateOf(AppLanguage.UR) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            viewModel.analyzeCameraBitmap(bitmap, "What is this? Identify objects, text, and helpful details.")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bmp = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) { null }
            if (bmp != null) {
                capturedBitmap = bmp
                viewModel.analyzeCameraBitmap(bmp, "What is this? Scan any visible text and explain it concisely.")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Vision, Dictation & Live Translate") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }

                capturedBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.analyzeCameraBitmap(bmp, "What is this?") },
                            label = { Text("What is this?") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.analyzeCameraBitmap(bmp, "Extract and format all visible text (OCR).") },
                            label = { Text("Scan Text") }
                        )
                    }
                }

                if (runtime.visionBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Analyzing with Gemini Vision...")
                    }
                }

                runtime.visionResult?.let { result ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(result, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                    }
                }

                Text("Dictation & Live Translator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = runtime.dictationText,
                    onValueChange = { viewModel.updateDictationText(it) },
                    label = { Text("Speak or type text to translate") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = targetLanguage == lang,
                            onClick = { targetLanguage = lang },
                            label = { Text(lang.code) }
                        )
                    }
                }
                Button(
                    onClick = { viewModel.translateInput(runtime.dictationText, targetLanguage) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Translate & Speak Aloud")
                }
                runtime.translationOutput?.let { out ->
                    Surface(shape = RoundedCornerShape(14.dp), color = NovaCyan.copy(alpha = 0.14f)) {
                        Text(out, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun ScreenControlInspectorOverlay(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    accessibilityConnected: Boolean,
    deviceController: DeviceController
) {
    BackHandler { viewModel.toggleScreenInspector(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD903040A))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("What's On My Screen?", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (accessibilityConnected) "Accessibility Service ACTIVE" else "Preview Overlay • Enable Accessibility for cross-app control",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (accessibilityConnected) NovaEmerald else NovaAmber
                    )
                }
                IconButton(onClick = { viewModel.toggleScreenInspector(false) }) {
                    Icon(Icons.Default.Close, contentDescription = "Close screen inspector", tint = Color.White)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                runtime.screenHighlights.forEach { node ->
                    NovaGlassCard(
                        onClick = { viewModel.performScreenCommand("CLICK:${node.title}", "Click ${node.title}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NovaCyan.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(node.title, style = MaterialTheme.typography.titleMedium, color = NovaCyan, fontWeight = FontWeight.Bold)
                                Text(node.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }
                            Surface(shape = RoundedCornerShape(12.dp), color = NovaCyan.copy(alpha = 0.18f)) {
                                Text(node.actionHint, style = MaterialTheme.typography.labelSmall, color = NovaCyan, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { viewModel.performScreenCommand("SCREEN_BACK", "System Back") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                    OutlinedButton(onClick = { viewModel.performScreenCommand("SCREEN_HOME", "Home") }) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Home")
                    }
                    OutlinedButton(onClick = { viewModel.performScreenCommand("SCREEN_RECENTS", "Recents") }) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Recents")
                    }
                    OutlinedButton(onClick = { viewModel.performScreenCommand("SCREEN_SHOT", "Screenshot") }) {
                        Icon(Icons.Default.Screenshot, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Screenshot")
                    }
                }
                if (!accessibilityConnected) {
                    Button(
                        onClick = { deviceController.openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
                        colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Nova Accessibility Service", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionWizardDialog(
    report: DevicePermissionReport,
    deviceController: DeviceController,
    onRequestMic: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Setup & Permissions Wizard") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Grant permissions to unlock full hands-free voice control, camera vision, and reliable background listening.",
                    style = MaterialTheme.typography.bodyMedium
                )
                PermissionRow("Microphone", "Wake-word & voice recognition", report.microphoneGranted, onRequestMic)
                PermissionRow("Accessibility", "Tap, scroll, type & click buttons by voice", report.accessibilityEnabled) {
                    deviceController.openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                }
                PermissionRow("Notifications", "Persistent foreground engine status", report.notificationsGranted, onRequestNotifications)
                PermissionRow("Camera", "Live Vision ('What is this?') & text scan", report.cameraGranted, onRequestCamera)
                PermissionRow("Location", "Navigation, reminders & Emergency SOS", report.locationGranted, onRequestLocation)
                PermissionRow("Battery Exemption", "Prevent OEM kill in background", report.batteryOptimizedIgnored) {
                    deviceController.openSystemSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    reason: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "$title granted", tint = NovaEmerald)
            } else {
                TextButton(onClick = onAction) { Text("Grant", color = NovaCyan) }
            }
        }
    }
}

@Composable
fun MicWakeWordTestDialog(
    wakeWord: String,
    amplitude: Float,
    transcript: String,
    onStartTest: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mic & Wake-Word Diagnostic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Say \"$wakeWord\" aloud to test microphone gain and wake-word recognition.")
                LinearProgressIndicator(
                    progress = { amplitude.coerceIn(0.05f, 1f) },
                    color = NovaCyan,
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50))
                )
                Text(
                    text = if (transcript.isBlank()) "Waiting for audio..." else "Detected: \"$transcript\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaCyan
                )
                Button(onClick = onStartTest, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Live Mic Test")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PinConfirmationDialog(
    title: String,
    subtitle: String,
    onConfirmPin: (String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NovaCyan) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                    label = { Text("4-Digit PIN (default 2026)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPin(pin) },
                enabled = pin.length == 4,
                modifier = Modifier.testTag("pin_confirm_button")
            ) {
                Text("Verify")
            }
        },
        dismissButton = if (onDismiss != null) {
            { TextButton(onClick = onDismiss) { Text("Cancel") } }
        } else null
    )
}
