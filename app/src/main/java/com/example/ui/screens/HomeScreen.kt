package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantMode
import com.example.data.model.OrbState
import com.example.data.model.UserSettings
import com.example.i18n.NovaLocalizedStrings
import com.example.ui.components.MicPrivacyIndicatorPill
import com.example.ui.components.NovaAnimatedOrb
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaWaveformVisualizer
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCrimson
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaViolet
import com.example.viewmodel.NovaRuntimeState
import com.example.viewmodel.NovaViewModel

private data class QuickChipData(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    settings: UserSettings,
    strings: NovaLocalizedStrings,
    onOrbTap: () -> Unit,
    onOpenControlPanel: () -> Unit,
    onOpenMicLog: () -> Unit,
    onOpenPermissionsWizard: () -> Unit
) {
    val novaColors = LocalNovaColors.current
    val haptic = LocalHapticFeedback.current
    var commandText by remember { mutableStateOf("") }

    val quickChips = remember(viewModel) {
        listOf(
            QuickChipData("Call", Icons.Default.Call) { viewModel.submitCommand("Call Mom on speaker") },
            QuickChipData("Message", Icons.Default.ChatBubble) { viewModel.submitCommand("Read my unread WhatsApp messages") },
            QuickChipData("Open App", Icons.Default.Apps) { viewModel.submitCommand("Open YouTube and search tech news") },
            QuickChipData("Alarm", Icons.Default.Alarm) { viewModel.submitCommand("Set an alarm for 7:00 AM") },
            QuickChipData("Music", Icons.Default.MusicNote) { viewModel.submitCommand("Play my focus playlist on Spotify") },
            QuickChipData("Navigate", Icons.Default.Navigation) { viewModel.submitCommand("Navigate to the nearest coffee shop") },
            QuickChipData("Briefing", Icons.Default.WbSunny) { viewModel.requestDailyBriefing() },
            QuickChipData("Vision", Icons.Default.CameraAlt) { viewModel.toggleVisionSheet(true) },
            QuickChipData("Translate", Icons.Default.Translate) { viewModel.toggleVisionSheet(true) },
            QuickChipData("Screen", Icons.Default.ScreenSearchDesktop) { viewModel.toggleScreenInspector(true) }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Privacy, Mic Indicator, and Emergency SOS Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicPrivacyIndicatorPill(
                    isListening = runtime.orbState == OrbState.LISTENING || settings.alwaysListening,
                    label = if (runtime.orbState == OrbState.LISTENING) strings.micInUse else "STANDBY",
                    onClick = onOpenMicLog
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenControlPanel,
                        shape = RoundedCornerShape(50),
                        color = novaColors.glassSurface,
                        border = BorderStroke(1.dp, novaColors.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Open Control Panel", tint = novaColors.cyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Controls", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.triggerEmergencySos()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NovaCrimson.copy(alpha = 0.22f), contentColor = NovaCrimson),
                        border = BorderStroke(1.dp, NovaCrimson.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("sos_button")
                    ) {
                        Icon(Icons.Default.Sos, contentDescription = "Emergency SOS", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.emergencySos, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 2. Special Modes Strip (Standard, Driving, Sleep, Work, Kids-Safe, Accessibility)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistantMode.entries.forEach { mode ->
                    val selected = runtime.assistantMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setAssistantMode(mode)
                        },
                        label = { Text(mode.label) },
                        leadingIcon = if (mode == AssistantMode.DRIVING) {
                            { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = novaColors.cyanAccent.copy(alpha = 0.2f),
                            selectedLabelColor = novaColors.cyanAccent
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
            }
        }

        // 3. Wake Word Always Listening Banner
        item {
            NovaGlassCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (settings.alwaysListening) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "\"${settings.wakeWord}\" • ${if (settings.alwaysListening) strings.wakeReady else "Paused"}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${settings.resolvedListeningMode().label} • ${settings.resolvedPersonality().label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = settings.alwaysListening,
                        onCheckedChange = { viewModel.updateSettingField("alwaysListening", it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan, checkedTrackColor = NovaCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("always_listening_switch")
                    )
                }
            }
        }

        // 4. Centerpiece 3D Animated Voice Orb + Waveform + Live Transcript
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NovaAnimatedOrb(
                    state = runtime.orbState,
                    amplitude = runtime.voiceAmplitude,
                    size = 216.dp,
                    onClick = onOrbTap
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = when (runtime.orbState) {
                        OrbState.IDLE -> strings.tapOrbPrompt
                        OrbState.LISTENING -> strings.listeningStatus
                        OrbState.THINKING -> strings.thinkingStatus
                        OrbState.SPEAKING -> strings.speakingStatus
                        OrbState.ERROR -> strings.errorStatus
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (runtime.orbState) {
                        OrbState.LISTENING -> NovaCyan
                        OrbState.THINKING -> NovaViolet
                        OrbState.SPEAKING -> NovaMagenta
                        OrbState.ERROR -> NovaCrimson
                        OrbState.IDLE -> MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                NovaWaveformVisualizer(
                    state = runtime.orbState,
                    amplitude = runtime.voiceAmplitude
                )
            }
        }

        // 5. Real-Time Transcript & Streaming Nova AI Reply Card
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                if (runtime.liveTranscript.isNotBlank()) {
                    Text(
                        text = "YOU SAID",
                        style = MaterialTheme.typography.labelSmall,
                        color = novaColors.cyanAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "\"${runtime.liveTranscript}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = novaColors.violetAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "NOVA RESPONSE",
                        style = MaterialTheme.typography.labelSmall,
                        color = novaColors.violetAccent
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = runtime.replyText,
                    style = MaterialTheme.typography.bodyLarge
                )

                AnimatedVisibility(visible = runtime.activeSteps.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            runtime.activeSteps.forEach { step ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = novaColors.cyanAccent.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, novaColors.cyanAccent.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = novaColors.cyanAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!runtime.errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NovaCrimson.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, NovaCrimson.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NovaCrimson, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = runtime.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NovaCrimson,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearStatusMessage() }) {
                                Text("Dismiss", color = NovaCrimson)
                            }
                        }
                    }
                }
            }
        }

        // 6. Hands-free / Emulator Multi-Step Command Input Bar
        item {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                placeholder = { Text(strings.askNovaPlaceholder, style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = novaColors.cyanAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = novaColors.glassSurface,
                    unfocusedContainerColor = novaColors.glassSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandText.isNotBlank()) {
                            viewModel.submitCommand(commandText)
                            commandText = ""
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (commandText.isNotBlank()) {
                                viewModel.submitCommand(commandText)
                                commandText = ""
                            }
                        },
                        modifier = Modifier.testTag("send_command_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.sendCommand, tint = novaColors.cyanAccent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_command_input")
            )
        }

        // 7. Quick Action Chips
        item {
            Text(
                text = strings.quickActionsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickChips) { chip ->
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            chip.onClick()
                        },
                        label = { Text(chip.label) },
                        leadingIcon = {
                            Icon(chip.icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = novaColors.cyanAccent)
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = novaColors.glassSurface
                        ),
                        border = BorderStroke(1.dp, novaColors.glassBorder)
                    )
                }
            }
        }

        // 8. Proactive Suggestion Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.proactiveSuggestionsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onOpenPermissionsWizard) {
                    Text("Setup Wizard", color = novaColors.cyanAccent)
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.proactiveInsights, key = { it.id }) { insight ->
                    NovaGlassCard(
                        onClick = { viewModel.submitCommand(insight.commandPrompt) },
                        modifier = Modifier.width(268.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when (insight.badge) {
                                "BATTERY" -> NovaAmber.copy(alpha = 0.18f)
                                "MEETING" -> NovaViolet.copy(alpha = 0.22f)
                                "CALLS" -> NovaMagenta.copy(alpha = 0.20f)
                                else -> NovaCyan.copy(alpha = 0.18f)
                            }
                        ) {
                            Text(
                                text = insight.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (insight.badge) {
                                    "BATTERY" -> NovaAmber
                                    "MEETING" -> NovaViolet
                                    "CALLS" -> NovaMagenta
                                    else -> NovaCyan
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = insight.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap to execute ->",
                            style = MaterialTheme.typography.labelMedium,
                            color = novaColors.cyanAccent
                        )
                    }
                }
            }
        }
    }
}
