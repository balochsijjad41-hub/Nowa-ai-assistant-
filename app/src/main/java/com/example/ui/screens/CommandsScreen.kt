package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomCommand
import com.example.data.model.PermissionPolicy
import com.example.system.DeviceController
import com.example.ui.components.NovaGlassCard
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCrimson
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaViolet
import com.example.viewmodel.DataUiState
import com.example.viewmodel.NovaRuntimeState
import com.example.viewmodel.NovaViewModel

@Composable
fun CommandsScreen(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    commandsState: DataUiState<List<CustomCommand>>,
    deviceController: DeviceController,
    initialSubTab: Int = 0
) {
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val subTabs = listOf("Categories", "Control Panel", "Custom Builder", "Skills Store")
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Commands & Device Control",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subTabs.forEachIndexed { idx, label ->
                FilterChip(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    label = { Text(label) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("commands_tab_$idx")
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when (selectedSubTab) {
            0 -> CategoriesTab(viewModel)
            1 -> PhoneControlPanelTab(viewModel, runtime, deviceController)
            2 -> CustomBuilderTab(viewModel, commandsState, onOpenDialog = { showCreateDialog = true })
            else -> SkillsStoreTab(viewModel, runtime)
        }
    }

    if (showCreateDialog) {
        CreateCustomCommandDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { phrase, category, steps, policy ->
                viewModel.createCustomCommand(phrase, category, steps, policy)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CategoriesTab(viewModel: NovaViewModel) {
    val novaColors = LocalNovaColors.current
    LazyColumn(
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(viewModel.voiceCategories, key = { it.id }) { cat ->
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = cat.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = novaColors.cyanAccent
                )
                Spacer(Modifier.height(8.dp))
                cat.examples.forEach { example ->
                    Surface(
                        onClick = { viewModel.submitCommand(example) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\"$example\"",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run example command",
                                tint = novaColors.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ControlToggleSpec(
    val key: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val settingsAction: String
)

@Composable
private fun PhoneControlPanelTab(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    deviceController: DeviceController
) {
    val novaColors = LocalNovaColors.current
    val phone = runtime.phoneControl
    val toggles = listOf(
        ControlToggleSpec("WiFi", Icons.Default.Wifi, phone.wifiOn, Settings.ACTION_WIFI_SETTINGS),
        ControlToggleSpec("Bluetooth", Icons.Default.Bluetooth, phone.bluetoothOn, Settings.ACTION_BLUETOOTH_SETTINGS),
        ControlToggleSpec("Mobile Data", Icons.Default.CellTower, phone.mobileDataOn, Settings.ACTION_WIRELESS_SETTINGS),
        ControlToggleSpec("Hotspot", Icons.Default.WifiTethering, phone.hotspotOn, Settings.ACTION_WIRELESS_SETTINGS),
        ControlToggleSpec("Airplane Mode", Icons.Default.AirplanemodeActive, phone.airplaneModeOn, Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        ControlToggleSpec("Flashlight", Icons.Default.FlashlightOn, phone.flashlightOn, Settings.ACTION_SETTINGS),
        ControlToggleSpec("Do Not Disturb", Icons.Default.DoNotDisturbOn, phone.dndOn, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
        ControlToggleSpec("Auto-Rotate", Icons.Default.ScreenRotation, phone.autoRotateOn, Settings.ACTION_DISPLAY_SETTINGS)
    )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sliders card: Volume & Brightness
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Volume & Brightness", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = novaColors.cyanAccent)
                    Spacer(Modifier.width(10.dp))
                    Text("Media Volume (${(phone.volumePercent * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("VOICE READY", style = MaterialTheme.typography.labelSmall, color = NovaEmerald)
                }
                Slider(
                    value = phone.volumePercent,
                    onValueChange = { viewModel.setVolumePercent(it) },
                    colors = SliderDefaults.colors(thumbColor = NovaCyan, activeTrackColor = NovaCyan)
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brightness6, contentDescription = "Brightness", tint = novaColors.violetAccent)
                    Spacer(Modifier.width(10.dp))
                    Text("Display Brightness (${(phone.brightnessPercent * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("VOICE READY", style = MaterialTheme.typography.labelSmall, color = NovaEmerald)
                }
                Slider(
                    value = phone.brightnessPercent,
                    onValueChange = { viewModel.setBrightnessPercent(it) },
                    colors = SliderDefaults.colors(thumbColor = NovaViolet, activeTrackColor = NovaViolet)
                )
            }
        }

        // Toggle Grid (2 per row) with Voice-Control status and Per-Command Permission selector
        items(toggles.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { spec ->
                    val policy = phone.controlPermissions[spec.key] ?: PermissionPolicy.ALWAYS_ALLOW
                    var menuOpen by remember { mutableStateOf(false) }

                    NovaGlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.togglePhoneSetting(spec.key) },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.key,
                                tint = if (spec.enabled) novaColors.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Switch(
                                checked = spec.enabled,
                                onCheckedChange = { viewModel.togglePhoneSetting(spec.key) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan, checkedTrackColor = NovaCyan.copy(alpha = 0.35f))
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(spec.key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { menuOpen = true },
                                shape = RoundedCornerShape(10.dp),
                                color = when (policy) {
                                    PermissionPolicy.ALWAYS_ALLOW -> NovaEmerald.copy(alpha = 0.16f)
                                    PermissionPolicy.ASK_EACH_TIME -> NovaAmber.copy(alpha = 0.16f)
                                    PermissionPolicy.BLOCK -> NovaCrimson.copy(alpha = 0.16f)
                                }
                            ) {
                                Text(
                                    text = policy.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (policy) {
                                        PermissionPolicy.ALWAYS_ALLOW -> NovaEmerald
                                        PermissionPolicy.ASK_EACH_TIME -> NovaAmber
                                        PermissionPolicy.BLOCK -> NovaCrimson
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            IconButton(
                                onClick = { deviceController.openSystemSettings(spec.settingsAction) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Open ${spec.key} system settings", modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            PermissionPolicy.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        viewModel.setControlPermission(spec.key, option)
                                        menuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CustomBuilderTab(
    viewModel: NovaViewModel,
    commandsState: DataUiState<List<CustomCommand>>,
    onOpenDialog: () -> Unit
) {
    val novaColors = LocalNovaColors.current
    val commands = (commandsState as? DataUiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onOpenDialog,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_custom_command_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Build Phrase -> Action Chain", fontWeight = FontWeight.Bold)
            }
        }

        if (commands.isEmpty()) {
            item {
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No custom commands yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Create custom voice shortcuts like \"Movie night\" to chain multiple phone actions together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(commands, key = { it.id }) { cmd ->
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("\"${cmd.phrase}\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${cmd.category} • ${cmd.resolvedPolicy().label} • ${cmd.usageCount} runs",
                                style = MaterialTheme.typography.labelSmall,
                                color = novaColors.cyanAccent
                            )
                        }
                        Switch(
                            checked = cmd.enabled,
                            onCheckedChange = { viewModel.toggleCustomCommandEnabled(cmd) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = cmd.actionsSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.submitCommand(cmd.phrase) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Test Voice Chain")
                        }
                        IconButton(onClick = { viewModel.deleteCustomCommand(cmd.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete custom command", tint = NovaCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillsStoreTab(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState
) {
    val novaColors = LocalNovaColors.current
    LazyColumn(
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(runtime.skills, key = { it.id }) { skill ->
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = novaColors.violetAccent)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(skill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(skill.category, style = MaterialTheme.typography.labelSmall, color = novaColors.cyanAccent)
                        }
                    }
                    Switch(
                        checked = skill.connected,
                        onCheckedChange = { viewModel.toggleSkill(skill.id) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = { viewModel.submitCommand(skill.samplePhrase.trim('"')) },
                    shape = RoundedCornerShape(12.dp),
                    color = novaColors.cyanAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, novaColors.cyanAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = novaColors.cyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(skill.samplePhrase, style = MaterialTheme.typography.labelMedium, color = novaColors.cyanAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCustomCommandDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>, PermissionPolicy) -> Unit
) {
    var phrase by remember { mutableStateOf("") }
    var stepsText by remember { mutableStateOf("Turn on WiFi, Set Volume 75%, Open YouTube") }
    var selectedCategory by remember { mutableStateOf("CUSTOM") }
    var selectedPolicy by remember { mutableStateOf(PermissionPolicy.ALWAYS_ALLOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Voice Command Chain") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("Trigger phrase") },
                    placeholder = { Text("e.g. Start gaming mode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    label = { Text("Action chain (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Permission Policy", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PermissionPolicy.entries.forEach { policy ->
                        FilterChip(
                            selected = selectedPolicy == policy,
                            onClick = { selectedPolicy = policy },
                            label = { Text(policy.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val steps = stepsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (phrase.isNotBlank() && steps.isNotEmpty()) {
                        onCreate(phrase, selectedCategory, steps, selectedPolicy)
                    }
                }
            ) {
                Text("Save Command")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
