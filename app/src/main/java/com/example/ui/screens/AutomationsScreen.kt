package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AutomationRoutine
import com.example.data.model.DeviceType
import com.example.data.model.SmartDevice
import com.example.data.model.TriggerType
import com.example.ui.components.NovaGlassCard
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaCrimson
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaViolet
import com.example.viewmodel.DataUiState
import com.example.viewmodel.NovaViewModel

@Composable
fun AutomationsScreen(
    viewModel: NovaViewModel,
    automationsState: DataUiState<List<AutomationRoutine>>,
    smartDevicesState: DataUiState<List<SmartDevice>>
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Automations & Smart Home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Routines") },
                modifier = Modifier.testTag("automations_tab_routines")
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Smart Home") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("automations_tab_smarthome")
            )
        }

        Spacer(Modifier.height(10.dp))

        if (selectedTab == 0) {
            RoutinesList(
                viewModel = viewModel,
                automationsState = automationsState,
                onOpenCreate = { showCreateDialog = true }
            )
        } else {
            SmartHomeTab(viewModel = viewModel, smartDevicesState = smartDevicesState)
        }
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, triggerType, detail, steps ->
                viewModel.createAutomation(title, triggerType, detail, steps)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun RoutinesList(
    viewModel: NovaViewModel,
    automationsState: DataUiState<List<AutomationRoutine>>,
    onOpenCreate: () -> Unit
) {
    val novaColors = LocalNovaColors.current

    LazyColumn(
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onOpenCreate,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaViolet, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("new_routine_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Multi-Action Routine", fontWeight = FontWeight.Bold)
            }
        }

        when (automationsState) {
            is DataUiState.Loading -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = NovaCyan)
                    }
                }
            }
            is DataUiState.Error -> {
                item {
                    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(automationsState.message, color = NovaCrimson)
                    }
                }
            }
            is DataUiState.Success -> {
                items(automationsState.data, key = { it.id }) { routine ->
                    val icon = when {
                        routine.title.contains("Morning", true) -> Icons.Default.WbSunny
                        routine.title.contains("Bed", true) -> Icons.Default.NightsStay
                        routine.title.contains("Driv", true) -> Icons.Default.DirectionsCar
                        routine.title.contains("Work", true) -> Icons.Default.Work
                        else -> when (routine.resolvedTrigger()) {
                            TriggerType.TIME -> Icons.Default.Schedule
                            TriggerType.LOCATION -> Icons.Default.LocationOn
                            TriggerType.CHARGING -> Icons.Default.BatteryChargingFull
                            else -> Icons.Default.Mic
                        }
                    }

                    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(icon, contentDescription = null, tint = novaColors.cyanAccent, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(routine.title, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "${routine.resolvedTrigger().label} • ${routine.triggerDetail}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = novaColors.cyanAccent
                                    )
                                }
                            }
                            Switch(
                                checked = routine.enabled,
                                onCheckedChange = { viewModel.toggleAutomationEnabled(routine) }
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            routine.actionSteps.forEachIndexed { idx, step ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${idx + 1}. $step",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.runAutomation(routine) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, novaColors.cyanAccent.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = novaColors.cyanAccent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Run Now (${routine.runCount})", color = novaColors.cyanAccent)
                            }
                            IconButton(onClick = { viewModel.deleteAutomation(routine.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete routine", tint = NovaCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartHomeTab(
    viewModel: NovaViewModel,
    smartDevicesState: DataUiState<List<SmartDevice>>
) {
    val novaColors = LocalNovaColors.current

    LazyColumn(
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Voice Smart Home Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Say \"Turn off living room lights\" or \"Set AC to 21\" to control connected home devices hands-free.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (smartDevicesState) {
            is DataUiState.Loading -> {
                item {
                    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = NovaCyan)
                    }
                }
            }
            is DataUiState.Error -> {
                item {
                    NovaGlassCard(Modifier.fillMaxWidth()) {
                        Text(smartDevicesState.message, color = NovaCrimson)
                    }
                }
            }
            is DataUiState.Success -> {
                items(smartDevicesState.data, key = { it.id }) { device ->
                    val icon = when (device.resolvedType()) {
                        DeviceType.LIGHT -> Icons.Default.Lightbulb
                        DeviceType.AC -> Icons.Default.AcUnit
                        DeviceType.TV -> Icons.Default.Tv
                        DeviceType.LOCK -> Icons.Default.Lock
                        DeviceType.SPEAKER -> Icons.Default.Speaker
                    }
                    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = device.name,
                                    tint = if (device.poweredOn) novaColors.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${device.room} • ${if (device.voiceEnabled) "Voice Ready" else "Manual"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (device.poweredOn) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = device.poweredOn,
                                onCheckedChange = { viewModel.toggleSmartDevicePower(device) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan, checkedTrackColor = NovaCyan.copy(alpha = 0.35f))
                            )
                        }

                        if (device.resolvedType() != DeviceType.LOCK) {
                            Spacer(Modifier.height(8.dp))
                            val unit = if (device.resolvedType() == DeviceType.AC) "°C" else "%"
                            Text(
                                text = "Level: ${device.levelValue}$unit",
                                style = MaterialTheme.typography.labelMedium,
                                color = novaColors.violetAccent
                            )
                            Slider(
                                value = device.levelValue.toFloat(),
                                onValueChange = { viewModel.updateSmartDeviceLevel(device, it.toLong()) },
                                valueRange = if (device.resolvedType() == DeviceType.AC) 16f..30f else 0f..100f,
                                colors = SliderDefaults.colors(thumbColor = NovaMagenta, activeTrackColor = NovaViolet)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoutineDialog(
    onDismiss: () -> Unit,
    onCreate: (String, TriggerType, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf(TriggerType.VOICE) }
    var triggerDetail by remember { mutableStateOf("\"Hey Nova, focus time\"") }
    var stepsText by remember { mutableStateOf("Enable Do Not Disturb, Set Volume 30%, Open Spotify") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Automation Routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine title") },
                    placeholder = { Text("e.g. Deep Focus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Trigger Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TriggerType.entries.forEach { t ->
                        FilterChip(
                            selected = triggerType == t,
                            onClick = { triggerType = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = triggerDetail,
                    onValueChange = { triggerDetail = it },
                    label = { Text("Trigger condition") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    label = { Text("Multi-action steps (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val steps = stepsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (title.isNotBlank() && steps.isNotEmpty()) {
                        onCreate(title, triggerType, triggerDetail, steps)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
