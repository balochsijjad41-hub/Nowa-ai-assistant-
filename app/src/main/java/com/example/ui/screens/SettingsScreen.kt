package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.AssistantMode
import com.example.data.model.ListeningMode
import com.example.data.model.NovaPersonality
import com.example.data.model.ServiceHealth
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.model.VoiceGender
import com.example.system.DeviceController
import com.example.system.DevicePermissionReport
import com.example.ui.components.NovaGlassCard
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCrimson
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaViolet
import com.example.viewmodel.NovaRuntimeState
import com.example.viewmodel.NovaViewModel

@Composable
fun SettingsScreen(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    settings: UserSettings,
    userEmail: String,
    serviceRunning: Boolean,
    permissionReport: DevicePermissionReport,
    deviceController: DeviceController,
    onOpenPermissionWizard: () -> Unit,
    onOpenMicTest: () -> Unit,
    onSignOut: () -> Unit
) {
    val novaColors = LocalNovaColors.current
    var wakeWordInput by remember(settings.wakeWord) { mutableStateOf(settings.wakeWord) }
    var emergencyInput by remember(settings.emergencyContact) { mutableStateOf(settings.emergencyContact) }
    var newPinInput by remember { mutableStateOf("") }
    var showCheatSheet by remember { mutableStateOf(false) }

    val serviceHealth = viewModel.computeServiceHealth(
        serviceRunning = serviceRunning,
        micGranted = permissionReport.microphoneGranted,
        batteryExempt = permissionReport.batteryOptimizedIgnored
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings & Nova Engine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // 1. Voice, Language (RTL Urdu / Hindi / English) & Personality
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = novaColors.cyanAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Voice, Language & Persona", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wakeWordInput,
                        onValueChange = { wakeWordInput = it },
                        label = { Text("Wake Word") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (wakeWordInput.trim().length >= 2) {
                                viewModel.updateSettingField("wakeWord", wakeWordInput.trim())
                            }
                        }
                    ) { Text("Save") }
                }

                Spacer(Modifier.height(10.dp))
                Text("Language & Layout Direction", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = settings.language == lang.code,
                            onClick = { viewModel.updateSettingField("language", lang.code) },
                            label = { Text(lang.displayName) },
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Voice Style", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoiceGender.entries.forEach { gender ->
                        FilterChip(
                            selected = settings.voiceGender == gender.name,
                            onClick = { viewModel.updateSettingField("voiceGender", gender.name) },
                            label = { Text(gender.label) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Speech Speed: ${String.format("%.1fx", settings.voiceSpeed)}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = settings.voiceSpeed.toFloat(),
                    onValueChange = { viewModel.updateSettingField("voiceSpeed", it.toDouble()) },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = NovaCyan, activeTrackColor = NovaCyan)
                )

                Text("Personality", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NovaPersonality.entries.forEach { p ->
                        FilterChip(
                            selected = settings.personality == p.name,
                            onClick = { viewModel.updateSettingField("personality", p.name) },
                            label = { Text(p.label) }
                        )
                    }
                }
            }
        }

        // 2. Theme & Accessibility Mode
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = novaColors.violetAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Theme & Accessibility", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode.name,
                            onClick = { viewModel.updateSettingField("themeMode", mode.name) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("theme_chip_${mode.name}")
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                SettingSwitchRow(
                    title = "Accessibility Mode",
                    subtitle = "Larger UI typography & automatic spoken feedback",
                    checked = runtime.assistantMode == AssistantMode.ACCESSIBILITY,
                    onCheckedChange = { enabled ->
                        viewModel.setAssistantMode(if (enabled) AssistantMode.ACCESSIBILITY else AssistantMode.NORMAL)
                    }
                )
            }
        }

        // 3. Background Service, Health Indicator & OEM Battery Optimization Guide
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = novaColors.cyanAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Background & Always-On", style = MaterialTheme.typography.titleLarge)
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when (serviceHealth) {
                            ServiceHealth.GREEN -> NovaEmerald.copy(alpha = 0.2f)
                            ServiceHealth.ORANGE -> NovaAmber.copy(alpha = 0.2f)
                            ServiceHealth.RED -> NovaCrimson.copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (serviceHealth) {
                                            ServiceHealth.GREEN -> NovaEmerald
                                            ServiceHealth.ORANGE -> NovaAmber
                                            ServiceHealth.RED -> NovaCrimson
                                        }
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when (serviceHealth) {
                                    ServiceHealth.GREEN -> "HEALTHY"
                                    ServiceHealth.ORANGE -> "LIMITED"
                                    ServiceHealth.RED -> "NEEDS MIC"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (serviceHealth) {
                                    ServiceHealth.GREEN -> NovaEmerald
                                    ServiceHealth.ORANGE -> NovaAmber
                                    ServiceHealth.RED -> NovaCrimson
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Battery Usage Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("24h Nova Engine Battery Usage", style = MaterialTheme.typography.labelLarge)
                            Text("1.4%", style = MaterialTheme.typography.labelLarge, color = NovaEmerald, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0.14f },
                            color = NovaEmerald,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                SettingSwitchRow(
                    title = "Run in Background (Foreground Service)",
                    subtitle = if (serviceRunning) "Active persistent notification with Mic, Speak & Stop" else "Paused",
                    checked = serviceRunning,
                    onCheckedChange = { viewModel.updateSettingField("runInBackground", it) }
                )
                SettingSwitchRow(
                    title = "Start on Boot",
                    subtitle = "Auto-launch listening engine when phone restarts",
                    checked = settings.startOnBoot,
                    onCheckedChange = { viewModel.updateSettingField("startOnBoot", it) }
                )
                SettingSwitchRow(
                    title = "Keep Alive & Screen-Off Ready",
                    subtitle = "Preserve DSP wake-word listener across lock screen",
                    checked = settings.keepAlive,
                    onCheckedChange = { viewModel.updateSettingField("keepAlive", it) }
                )

                Spacer(Modifier.height(8.dp))
                Text("Listening Schedule Mode", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListeningMode.entries.forEach { lm ->
                        FilterChip(
                            selected = settings.listeningMode == lm.name,
                            onClick = { viewModel.updateSettingField("listeningMode", lm.name) },
                            label = { Text(lm.label) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("OEM Battery Optimization Guide", style = MaterialTheme.typography.labelLarge, color = novaColors.cyanAccent)
                Spacer(Modifier.height(6.dp))
                val oemGuides = listOf(
                    "Xiaomi" to "Settings -> Apps -> Nova -> Battery Saver -> No restrictions + enable Autostart",
                    "Oppo" to "App Info -> Battery usage -> Allow background activity & Auto-launch",
                    "Vivo" to "Settings -> Battery -> High background power consumption -> Enable Nova",
                    "Samsung" to "Device Care -> Battery -> Background usage limits -> Never sleeping apps -> Add Nova"
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    oemGuides.forEach { (brand, steps) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, novaColors.glassBorder),
                            modifier = Modifier.width(215.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(brand, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = novaColors.cyanAccent)
                                Spacer(Modifier.height(4.dp))
                                Text(steps, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { deviceController.openSystemSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Request Unrestricted Battery Exemption")
                }
            }
        }

        // 4. Quick Access & Accessibility Controls
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = novaColors.violetAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Quick Access & Permissions", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                SettingSwitchRow(
                    title = "Floating Assistant Bubble",
                    subtitle = "Draggable mini orb that expands into a quick listening card",
                    checked = runtime.floatingBubbleEnabled,
                    onCheckedChange = { viewModel.toggleFloatingBubble(it) }
                )
                Text(
                    text = "Also available via Nova Home Widget, Quick Settings Tile ('Ask Nova'), Lock Screen Assist, Power-button long-press, and Shake shortcut.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onOpenPermissionWizard, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Permission Wizard")
                    }
                    OutlinedButton(onClick = onOpenMicTest, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mic & Wake Test")
                    }
                }
            }
        }

        // 5. Security & Trust
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NovaEmerald)
                    Spacer(Modifier.width(8.dp))
                    Text("Security, Privacy & SOS", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                SettingSwitchRow(
                    title = "On-Device Basic Processing",
                    subtitle = "Handle WiFi, Bluetooth, Flashlight & DND offline without cloud calls",
                    checked = settings.onDeviceProcessing,
                    onCheckedChange = { viewModel.updateSettingField("onDeviceProcessing", it) }
                )
                SettingSwitchRow(
                    title = "Speaker Voice Recognition",
                    subtitle = "Verify owner voice print before running private commands",
                    checked = settings.speakerRecognition,
                    onCheckedChange = { viewModel.updateSettingField("speakerRecognition", it) }
                )
                SettingSwitchRow(
                    title = "PIN Confirmation for Sensitive Actions",
                    subtitle = "Require PIN (${runtime.securityPinCode}) before calls, payments & deletes",
                    checked = settings.pinConfirmationEnabled,
                    onCheckedChange = { viewModel.updateSettingField("pinConfirmationEnabled", it) }
                )
                SettingSwitchRow(
                    title = "App Lock Protection",
                    subtitle = "Lock Nova workspace with your 4-digit PIN",
                    checked = settings.appLockEnabled,
                    onCheckedChange = { viewModel.updateSettingField("appLockEnabled", it) }
                )

                if (settings.appLockEnabled) {
                    TextButton(onClick = { viewModel.lockAppNow() }) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lock Nova Now")
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Auto-Delete History Retention", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7L to "7 Days", 30L to "30 Days", 90L to "90 Days", 0L to "Keep All").forEach { (days, label) ->
                        FilterChip(
                            selected = settings.autoDeleteDays == days,
                            onClick = { viewModel.updateSettingField("autoDeleteDays", days) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPinInput = it },
                        label = { Text("Change 4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newPinInput.length == 4) {
                                viewModel.updateSecurityPin(newPinInput)
                                newPinInput = ""
                            }
                        }
                    ) { Text("Set PIN") }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = emergencyInput,
                        onValueChange = { emergencyInput = it },
                        label = { Text("Emergency SOS Contact") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.updateSettingField("emergencyContact", emergencyInput.trim()) }
                    ) { Text("Save") }
                }
            }
        }

        // 6. Tutorial Cheat Sheet, Cloud Backup & Sign Out
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = novaColors.cyanAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Cloud Sync, Cheat Sheet & Account", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(6.dp))
                Text("Signed in as $userEmail", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                runtime.backupStatusMessage?.let { status ->
                    Spacer(Modifier.height(6.dp))
                    Text(status, style = MaterialTheme.typography.labelMedium, color = NovaEmerald)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.triggerManualCloudBackup() },
                        colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup & Sync", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showCheatSheet = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cheat Sheet")
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    border = BorderStroke(1.dp, NovaCrimson.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }

    if (showCheatSheet) {
        AlertDialog(
            onDismissRequest = { showCheatSheet = false },
            title = { Text("Nova Voice Cheat Sheet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Multi-step: \"Turn on WiFi, open YouTube and play music\"")
                    Text("• Memory: \"Call Alex\" then \"Call him again\"")
                    Text("• Screen Control: \"Scroll down\", \"Click Send\", \"Take a screenshot\"")
                    Text("• Vision: Tap Vision -> \"What is this?\" or \"Translate menu\"")
                    Text("• Modes: \"Switch to Driving Mode\" or \"Bedtime routine\"")
                    Text("• Emergency: \"Emergency SOS\" dials saved contact with location")
                }
            },
            confirmButton = {
                TextButton(onClick = { showCheatSheet = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
