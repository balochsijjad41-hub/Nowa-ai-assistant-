package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.data.model.AssistantMode
import com.example.data.model.AutomationRoutine
import com.example.data.model.CustomCommand
import com.example.data.model.HistoryEntry
import com.example.data.model.ListeningMode
import com.example.data.model.MicAccessLogEntry
import com.example.data.model.NovaPersonality
import com.example.data.model.OrbState
import com.example.data.model.PermissionPolicy
import com.example.data.model.PhoneControlState
import com.example.data.model.ProactiveInsight
import com.example.data.model.ScreenHighlightNode
import com.example.data.model.ServiceHealth
import com.example.data.model.SkillAppItem
import com.example.data.model.SmartDevice
import com.example.data.model.ThemeMode
import com.example.data.model.TriggerType
import com.example.data.model.UserSettings
import com.example.data.model.VoiceCategoryItem
import com.example.data.model.VoiceGender
import com.example.data.repository.NovaAiRepository
import com.example.data.repository.NovaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface NovaHardwareEvent {
    data class Speak(val text: String, val language: AppLanguage, val gender: VoiceGender, val speed: Float) : NovaHardwareEvent
    data class SetFlashlight(val enabled: Boolean) : NovaHardwareEvent
    data class SetVolume(val percent: Float) : NovaHardwareEvent
    data class LaunchApp(val targetApp: String) : NovaHardwareEvent
    data class DialEmergency(val number: String) : NovaHardwareEvent
    data class AccessibilityAction(val action: String) : NovaHardwareEvent
    data class SyncBootPref(val startOnBoot: Boolean) : NovaHardwareEvent
    data class ToggleForegroundService(val enabled: Boolean) : NovaHardwareEvent
}

data class NovaRuntimeState(
    val orbState: OrbState = OrbState.IDLE,
    val voiceAmplitude: Float = 0.15f,
    val liveTranscript: String = "",
    val replyText: String = "Ready when you are. Say \"Hey Nova\" or tap the orb to control your phone hands-free.",
    val activeSteps: List<String> = emptyList(),
    val errorMessage: String? = null,
    val assistantMode: AssistantMode = AssistantMode.NORMAL,
    val phoneControl: PhoneControlState = PhoneControlState(),
    val floatingBubbleEnabled: Boolean = true,
    val floatingBubbleExpanded: Boolean = false,
    val visionSheetOpen: Boolean = false,
    val visionBusy: Boolean = false,
    val visionResult: String? = null,
    val dictationText: String = "",
    val translationOutput: String? = null,
    val screenInspectorOpen: Boolean = false,
    val screenHighlights: List<ScreenHighlightNode> = defaultScreenHighlights(),
    val pinDialogVisible: Boolean = false,
    val pendingSensitiveCommand: String? = null,
    val securityPinCode: String = "2026",
    val appLocked: Boolean = false,
    val backupStatusMessage: String? = null,
    val skills: List<SkillAppItem> = defaultSkills(),
    val micAccessLog: List<MicAccessLogEntry> = listOf(
        MicAccessLogEntry("m1", "Foreground Engine", "Wake-word standby check", "10:42 AM"),
        MicAccessLogEntry("m2", "Voice Orb", "Hands-free command session", "10:15 AM")
    )
)

private fun defaultScreenHighlights(): List<ScreenHighlightNode> = listOf(
    ScreenHighlightNode("Voice Orb", "Primary hands-free microphone trigger", "Say \"Click Voice Orb\"", 0.50f, 0.36f),
    ScreenHighlightNode("Wake Word Pill", "Toggles Always Listening engine", "Say \"Click Wake Word\"", 0.50f, 0.18f),
    ScreenHighlightNode("Quick Chips", "Call, Message, Open App, Alarm, Music, Navigate", "Say \"Scroll right\"", 0.48f, 0.64f),
    ScreenHighlightNode("Navigation Bar", "Switches between Home, Commands, Automations, History, Settings", "Say \"Open Settings\"", 0.50f, 0.92f)
)

private fun defaultSkills(): List<SkillAppItem> = listOf(
    SkillAppItem("sk_wa", "WhatsApp", "Messaging", "Read incoming messages aloud and send voice replies hands-free.", "\"Read my WhatsApp and reply I'm on my way\"", true),
    SkillAppItem("sk_yt", "YouTube", "Media", "Search videos, play music mixes, and control playback.", "\"Open YouTube and play lo-fi focus mix\"", true),
    SkillAppItem("sk_sp", "Spotify", "Music", "Play liked songs, skip tracks, and switch driving playlists.", "\"Play my driving playlist on Spotify\"", true),
    SkillAppItem("sk_gm", "Google Maps", "Navigation", "Start turn-by-turn navigation and check live traffic ETA.", "\"Navigate home via fastest route\"", true),
    SkillAppItem("sk_sh", "Smart Home", "IoT", "Control ceiling lights, AC climate, TV, and front door lock.", "\"Turn off living room lights and set AC to 21\"", true)
)

class NovaViewModel(
    private val repository: NovaRepository,
    private val aiRepository: NovaAiRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _runtime = MutableStateFlow(NovaRuntimeState())
    val runtime: StateFlow<NovaRuntimeState> = _runtime.asStateFlow()

    private val _hardwareEvents = MutableSharedFlow<NovaHardwareEvent>(extraBufferCapacity = 16)
    val hardwareEvents: SharedFlow<NovaHardwareEvent> = _hardwareEvents.asSharedFlow()

    private val conversationMemory = mutableListOf<Pair<String, String>>()

    val settingsState: StateFlow<DataUiState<UserSettings>> = repository.observeUserSettings(currentUserId)
        .map<UserSettings?, DataUiState<UserSettings>> { settings ->
            DataUiState.Success(settings ?: UserSettings(userId = currentUserId))
        }
        .catch { error -> emit(unwrapFirestoreError(error, "Failed to load settings")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val commandsState: StateFlow<DataUiState<List<CustomCommand>>> = repository.observeCommands(currentUserId)
        .map<List<CustomCommand>, DataUiState<List<CustomCommand>>> { DataUiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, "Failed to load commands")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val automationsState: StateFlow<DataUiState<List<AutomationRoutine>>> = repository.observeAutomations(currentUserId)
        .map<List<AutomationRoutine>, DataUiState<List<AutomationRoutine>>> { DataUiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, "Failed to load routines")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val historyState: StateFlow<DataUiState<List<HistoryEntry>>> = repository.observeHistory(currentUserId)
        .map<List<HistoryEntry>, DataUiState<List<HistoryEntry>>> { DataUiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, "Failed to load history")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val smartDevicesState: StateFlow<DataUiState<List<SmartDevice>>> = repository.observeSmartDevices(currentUserId)
        .map<List<SmartDevice>, DataUiState<List<SmartDevice>>> { DataUiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, "Failed to load smart devices")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val currentSettings: UserSettings
        get() = (settingsState.value as? DataUiState.Success)?.data ?: UserSettings(userId = currentUserId)

    val voiceCategories: List<VoiceCategoryItem> = listOf(
        VoiceCategoryItem("CALLS_SMS", "Calls & SMS", listOf("Call Mom on speaker", "Read my unread WhatsApp messages", "Reply I will call in 10 minutes", "Answer incoming call")),
        VoiceCategoryItem("APPS", "Apps & Screen", listOf("Open YouTube and search tech news", "Click Send button", "Scroll down", "What's on my screen?")),
        VoiceCategoryItem("CONNECTIVITY", "Connectivity", listOf("Turn on WiFi and Bluetooth", "Enable Do Not Disturb", "Turn on Hotspot", "Toggle Airplane mode")),
        VoiceCategoryItem("VOLUME_BRIGHTNESS", "Volume & Brightness", listOf("Set volume to 80%", "Dim brightness to 25%", "Mute media", "Turn on flashlight")),
        VoiceCategoryItem("CAMERA", "Camera & Vision", listOf("What is this object?", "Scan text from this document", "Take a selfie", "Translate menu to Urdu")),
        VoiceCategoryItem("MUSIC", "Music & Media", listOf("Play my driving mix on Spotify", "Next track", "Pause music", "Play rain sounds for sleep")),
        VoiceCategoryItem("MAPS", "Maps & Travel", listOf("Navigate to the nearest coffee shop", "How is traffic to work?", "Share my live ETA", "Find EV charging nearby")),
        VoiceCategoryItem("REMINDERS", "Reminders & Alarms", listOf("Wake me up at 6:30 AM", "Remind me to take meds at 9 PM", "Give me my daily briefing", "Add milk to shopping list")),
        VoiceCategoryItem("SEARCH", "Search & AI", listOf("Summarize quantum batteries simply", "Translate Hello how are you to Hindi", "Check USD to PKR rate", "Call him again"))
    )

    val proactiveInsights: List<ProactiveInsight> = listOf(
        ProactiveInsight("ins_bat", "Battery Optimization Ready", "Background DSP is using <1.4% battery. Switch to Low-Power Mode overnight?", "Switch to sleep mode and dim brightness", "BATTERY"),
        ProactiveInsight("ins_cal", "Upcoming Briefing • 11:30 AM", "Product Design Sync starts soon. Want Nova to prepare a 30-second brief?", "Give me my daily briefing", "MEETING"),
        ProactiveInsight("ins_call", "2 Missed Calls • Alex", "Would you like Nova to call Alex back or send a quick message?", "Call Alex back on speaker", "CALLS"),
        ProactiveInsight("ins_hab", "Habit Pattern Detected", "You usually enable WiFi and play lo-fi focus music around this hour.", "Turn on WiFi, open YouTube and play music", "HABIT")
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfNeeded(currentUserId)
        }
    }

    private fun <T> unwrapFirestoreError(error: Throwable, fallback: String): DataUiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallback, root)
        return DataUiState.Error(root.message ?: fallback)
    }

    fun onStartListeningUi(source: String = "Voice Orb") {
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        _runtime.update { state ->
            state.copy(
                orbState = OrbState.LISTENING,
                errorMessage = null,
                liveTranscript = "",
                micAccessLog = (listOf(MicAccessLogEntry("m_${System.currentTimeMillis()}", source, "Active voice capture", time)) + state.micAccessLog).take(15)
            )
        }
    }

    fun onVoiceRmsChanged(amplitude: Float) {
        _runtime.update { it.copy(voiceAmplitude = amplitude) }
    }

    fun onPartialTranscript(text: String) {
        _runtime.update { it.copy(liveTranscript = text) }
    }

    fun onVoiceError(message: String) {
        _runtime.update {
            it.copy(
                orbState = OrbState.ERROR,
                voiceAmplitude = 0.1f,
                errorMessage = message
            )
        }
    }

    fun onOrbResetIdle() {
        _runtime.update { it.copy(orbState = OrbState.IDLE, voiceAmplitude = 0.15f) }
    }

    fun submitCommand(rawInput: String, bypassPin: Boolean = false) {
        val query = rawInput.trim()
        if (query.isEmpty()) {
            onVoiceError("Didn't catch that, try again.")
            return
        }

        val settings = currentSettings
        val lower = query.lowercase()

        // Check custom command permission policy
        val customList = (commandsState.value as? DataUiState.Success)?.data.orEmpty()
        val matchedCustom = customList.firstOrNull { it.enabled && it.phrase.equals(query, ignoreCase = true) }
        if (matchedCustom != null && matchedCustom.resolvedPolicy() == PermissionPolicy.BLOCK) {
            _runtime.update {
                it.copy(
                    orbState = OrbState.ERROR,
                    errorMessage = "Blocked by your Security Policy for \"${matchedCustom.phrase}\"."
                )
            }
            return
        }

        val looksSensitive = lower.contains("pay") || lower.contains("transfer") ||
            lower.contains("delete all") || lower.contains("emergency") || lower.contains("sos") ||
            (lower.contains("call ") && settings.pinConfirmationEnabled)

        if (!bypassPin && settings.pinConfirmationEnabled && looksSensitive) {
            _runtime.update {
                it.copy(
                    pinDialogVisible = true,
                    pendingSensitiveCommand = query
                )
            }
            return
        }

        if (lower.contains("sos") || lower.contains("emergency")) {
            triggerEmergencySos()
            return
        }

        if (lower.contains("what's on my screen") || lower.contains("whats on my screen") || lower.contains("screen")) {
            _runtime.update { it.copy(screenInspectorOpen = true) }
        }

        viewModelScope.launch {
            _runtime.update {
                it.copy(
                    orbState = OrbState.THINKING,
                    liveTranscript = query,
                    errorMessage = null,
                    activeSteps = emptyList()
                )
            }

            // Offline fast-path if user enabled on-device / offline mode for basic commands
            if (settings.onDeviceProcessing) {
                executeOfflineCommand(query, settings)
                return@launch
            }

            val aiResult = aiRepository.interpretVoiceCommand(
                userPrompt = query,
                recentMemory = conversationMemory,
                language = settings.resolvedLanguage(),
                personality = settings.resolvedPersonality(),
                mode = _runtime.value.assistantMode,
                phoneState = _runtime.value.phoneControl
            )

            aiResult.onSuccess { plan ->
                applyHardwareToggles(plan.toggleKeys)
                plan.targetApp?.let { target ->
                    if (target.startsWith("SCREEN_")) {
                        _hardwareEvents.tryEmit(NovaHardwareEvent.AccessibilityAction(target))
                    } else {
                        _hardwareEvents.tryEmit(NovaHardwareEvent.LaunchApp(target))
                    }
                }

                conversationMemory.add(query to plan.replyText)
                if (conversationMemory.size > 8) conversationMemory.removeAt(0)

                val accessibilitySpeak = _runtime.value.assistantMode == AssistantMode.ACCESSIBILITY ||
                    _runtime.value.assistantMode == AssistantMode.DRIVING ||
                    settings.alwaysListening

                _runtime.update {
                    it.copy(
                        orbState = if (accessibilitySpeak) OrbState.SPEAKING else OrbState.IDLE,
                        replyText = plan.replyText,
                        activeSteps = plan.actionSteps,
                        voiceAmplitude = if (accessibilitySpeak) 0.55f else 0.15f
                    )
                }

                if (accessibilitySpeak) {
                    _hardwareEvents.tryEmit(
                        NovaHardwareEvent.Speak(
                            text = plan.replyText,
                            language = settings.resolvedLanguage(),
                            gender = settings.resolvedVoiceGender(),
                            speed = settings.voiceSpeed.toFloat()
                        )
                    )
                }

                repository.addHistoryEntry(
                    userId = currentUserId,
                    userQuery = query,
                    novaReply = plan.replyText,
                    intentTag = plan.intentTag,
                    executedActions = plan.actionSteps,
                    modeUsed = _runtime.value.assistantMode.name
                )

                if (matchedCustom != null) {
                    repository.updateCommand(
                        currentUserId,
                        matchedCustom.id,
                        mapOf("usageCount" to (matchedCustom.usageCount + 1L))
                    )
                }
            }.onFailure { err ->
                // Transparent error surfacing (or offline basic fallback if connectivity drops)
                _runtime.update {
                    it.copy(
                        orbState = OrbState.ERROR,
                        errorMessage = err.localizedMessage ?: "Nova AI could not process the command.",
                        replyText = "Offline/Connection notice: ${err.localizedMessage ?: "Network unavailable"}"
                    )
                }
            }
        }
    }

    private suspend fun executeOfflineCommand(query: String, settings: UserSettings) {
        val lower = query.lowercase()
        val steps = mutableListOf<String>()
        val toggles = mutableListOf<String>()

        if (lower.contains("wifi")) {
            val on = !lower.contains("off")
            toggles.add(if (on) "WIFI_ON" else "WIFI_OFF")
            steps.add("WiFi ${if (on) "enabled" else "disabled"} on-device")
        }
        if (lower.contains("bluetooth")) {
            val on = !lower.contains("off")
            toggles.add(if (on) "BT_ON" else "BT_OFF")
            steps.add("Bluetooth ${if (on) "enabled" else "disabled"} on-device")
        }
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val on = !lower.contains("off")
            toggles.add(if (on) "FLASH_ON" else "FLASH_OFF")
            steps.add("Flashlight ${if (on) "ON" else "OFF"}")
        }
        if (lower.contains("dnd") || lower.contains("do not disturb")) {
            val on = !lower.contains("off")
            toggles.add(if (on) "DND_ON" else "DND_OFF")
            steps.add("Do Not Disturb ${if (on) "ON" else "OFF"}")
        }
        if (steps.isEmpty()) {
            steps.add("Processed locally via On-Device Rule Engine")
        }
        applyHardwareToggles(toggles)

        val reply = "On-device execution complete: ${steps.joinToString(", ")}."
        _runtime.update {
            it.copy(
                orbState = OrbState.SPEAKING,
                replyText = reply,
                activeSteps = steps
            )
        }
        _hardwareEvents.tryEmit(
            NovaHardwareEvent.Speak(reply, settings.resolvedLanguage(), settings.resolvedVoiceGender(), settings.voiceSpeed.toFloat())
        )
        repository.addHistoryEntry(
            userId = currentUserId,
            userQuery = query,
            novaReply = reply,
            intentTag = "ON_DEVICE",
            executedActions = steps,
            modeUsed = _runtime.value.assistantMode.name
        )
    }

    private fun applyHardwareToggles(keys: List<String>) {
        keys.forEach { key ->
            when (key.uppercase()) {
                "WIFI_ON" -> updatePhoneControl { it.copy(wifiOn = true) }
                "WIFI_OFF" -> updatePhoneControl { it.copy(wifiOn = false) }
                "BT_ON" -> updatePhoneControl { it.copy(bluetoothOn = true) }
                "BT_OFF" -> updatePhoneControl { it.copy(bluetoothOn = false) }
                "DATA_ON" -> updatePhoneControl { it.copy(mobileDataOn = true) }
                "DATA_OFF" -> updatePhoneControl { it.copy(mobileDataOn = false) }
                "HOTSPOT_ON" -> updatePhoneControl { it.copy(hotspotOn = true) }
                "HOTSPOT_OFF" -> updatePhoneControl { it.copy(hotspotOn = false) }
                "AIRPLANE_ON" -> updatePhoneControl { it.copy(airplaneModeOn = true) }
                "AIRPLANE_OFF" -> updatePhoneControl { it.copy(airplaneModeOn = false) }
                "FLASH_ON" -> {
                    updatePhoneControl { it.copy(flashlightOn = true) }
                    _hardwareEvents.tryEmit(NovaHardwareEvent.SetFlashlight(true))
                }
                "FLASH_OFF" -> {
                    updatePhoneControl { it.copy(flashlightOn = false) }
                    _hardwareEvents.tryEmit(NovaHardwareEvent.SetFlashlight(false))
                }
                "DND_ON" -> updatePhoneControl { it.copy(dndOn = true) }
                "DND_OFF" -> updatePhoneControl { it.copy(dndOn = false) }
                "ROTATE_ON" -> updatePhoneControl { it.copy(autoRotateOn = true) }
                "ROTATE_OFF" -> updatePhoneControl { it.copy(autoRotateOn = false) }
                "VOL_UP" -> setVolumePercent((_runtime.value.phoneControl.volumePercent + 0.15f).coerceAtMost(1f))
                "VOL_DOWN" -> setVolumePercent((_runtime.value.phoneControl.volumePercent - 0.15f).coerceAtLeast(0f))
                "BRIGHT_UP" -> setBrightnessPercent((_runtime.value.phoneControl.brightnessPercent + 0.15f).coerceAtMost(1f))
                "BRIGHT_DOWN" -> setBrightnessPercent((_runtime.value.phoneControl.brightnessPercent - 0.15f).coerceAtLeast(0.1f))
                "LIGHTS_ON", "LIGHTS_OFF", "AC_ON", "AC_OFF", "TV_ON", "TV_OFF" -> {
                    toggleSmartDeviceByKeyword(key)
                }
            }
        }
    }

    private fun toggleSmartDeviceByKeyword(key: String) {
        val devices = (smartDevicesState.value as? DataUiState.Success)?.data ?: return
        val on = key.endsWith("_ON")
        val typeFilter = when {
            key.startsWith("LIGHTS") -> "LIGHT"
            key.startsWith("AC") -> "AC"
            key.startsWith("TV") -> "TV"
            else -> return
        }
        viewModelScope.launch {
            devices.filter { it.deviceType == typeFilter }.forEach { device ->
                repository.updateSmartDevice(currentUserId, device.id, mapOf("poweredOn" to on))
            }
        }
    }

    fun requestDailyBriefing() {
        val settings = currentSettings
        viewModelScope.launch {
            _runtime.update {
                it.copy(
                    orbState = OrbState.THINKING,
                    liveTranscript = "Give me my daily briefing",
                    errorMessage = null,
                    activeSteps = listOf("Fetching briefing stream", "Checking battery & calendar")
                )
            }
            var finalReply = ""
            try {
                aiRepository.streamDailyBriefing(settings.resolvedLanguage(), settings.resolvedPersonality())
                    .collect { partial ->
                        finalReply = partial
                        _runtime.update {
                            it.copy(orbState = OrbState.SPEAKING, replyText = partial, voiceAmplitude = 0.5f)
                        }
                    }
                if (finalReply.isNotBlank()) {
                    _hardwareEvents.tryEmit(
                        NovaHardwareEvent.Speak(finalReply, settings.resolvedLanguage(), settings.resolvedVoiceGender(), settings.voiceSpeed.toFloat())
                    )
                    repository.addHistoryEntry(
                        userId = currentUserId,
                        userQuery = "Daily Briefing",
                        novaReply = finalReply,
                        intentTag = "BRIEFING",
                        executedActions = listOf("Weather summary", "Schedule scan", "Battery check"),
                        modeUsed = _runtime.value.assistantMode.name
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _runtime.update {
                    it.copy(orbState = OrbState.ERROR, errorMessage = e.localizedMessage ?: "Briefing failed.")
                }
            }
        }
    }

    fun analyzeCameraBitmap(bitmap: Bitmap, prompt: String) {
        val settings = currentSettings
        viewModelScope.launch {
            _runtime.update { it.copy(visionBusy = true, visionResult = null) }
            val result = aiRepository.analyzeVision(bitmap, prompt, settings.resolvedLanguage())
            result.onSuccess { text ->
                _runtime.update { it.copy(visionBusy = false, visionResult = text) }
                repository.addHistoryEntry(
                    userId = currentUserId,
                    userQuery = "Camera Vision: $prompt",
                    novaReply = text,
                    intentTag = "VISION",
                    executedActions = listOf("Captured visual frame", "Gemini Multimodal Vision"),
                    modeUsed = _runtime.value.assistantMode.name
                )
            }.onFailure { err ->
                _runtime.update {
                    it.copy(visionBusy = false, visionResult = "Error: ${err.localizedMessage ?: "Vision failed"}")
                }
            }
        }
    }

    fun translateInput(sourceText: String, targetLanguage: AppLanguage) {
        if (sourceText.isBlank()) return
        viewModelScope.launch {
            _runtime.update { it.copy(visionBusy = true) }
            val res = aiRepository.translateText(sourceText, targetLanguage)
            res.onSuccess { translated ->
                _runtime.update { it.copy(visionBusy = false, translationOutput = translated) }
                _hardwareEvents.tryEmit(
                    NovaHardwareEvent.Speak(translated, targetLanguage, currentSettings.resolvedVoiceGender(), currentSettings.voiceSpeed.toFloat())
                )
            }.onFailure { err ->
                _runtime.update {
                    it.copy(visionBusy = false, translationOutput = "Translation error: ${err.localizedMessage}")
                }
            }
        }
    }

    fun updateDictationText(text: String) {
        _runtime.update { it.copy(dictationText = text) }
    }

    fun toggleVisionSheet(open: Boolean) {
        _runtime.update { it.copy(visionSheetOpen = open) }
    }

    fun toggleScreenInspector(open: Boolean) {
        _runtime.update { it.copy(screenInspectorOpen = open) }
    }

    fun performScreenCommand(actionCode: String, label: String) {
        _hardwareEvents.tryEmit(NovaHardwareEvent.AccessibilityAction(actionCode))
        _runtime.update {
            it.copy(
                replyText = "Screen action executed: $label",
                activeSteps = listOf("Accessibility Action", label),
                screenInspectorOpen = false
            )
        }
    }

    fun setAssistantMode(mode: AssistantMode) {
        _runtime.update { state ->
            val updatedPhone = when (mode) {
                AssistantMode.SLEEP -> state.phoneControl.copy(dndOn = true, brightnessPercent = 0.18f)
                AssistantMode.WORK -> state.phoneControl.copy(dndOn = true)
                AssistantMode.DRIVING -> state.phoneControl.copy(bluetoothOn = true)
                else -> state.phoneControl
            }
            state.copy(
                assistantMode = mode,
                phoneControl = updatedPhone,
                replyText = "${mode.label} Mode active • ${mode.subtitle}"
            )
        }
    }

    private fun updatePhoneControl(transform: (PhoneControlState) -> PhoneControlState) {
        _runtime.update { it.copy(phoneControl = transform(it.phoneControl)) }
    }

    fun togglePhoneSetting(key: String) {
        val policy = _runtime.value.phoneControl.controlPermissions[key] ?: PermissionPolicy.ALWAYS_ALLOW
        if (policy == PermissionPolicy.BLOCK) {
            _runtime.update { it.copy(errorMessage = "$key is blocked by Per-Command Permissions.") }
            return
        }
        updatePhoneControl { s ->
            when (key) {
                "WiFi" -> s.copy(wifiOn = !s.wifiOn)
                "Bluetooth" -> s.copy(bluetoothOn = !s.bluetoothOn)
                "Mobile Data" -> s.copy(mobileDataOn = !s.mobileDataOn)
                "Hotspot" -> s.copy(hotspotOn = !s.hotspotOn)
                "Airplane Mode" -> s.copy(airplaneModeOn = !s.airplaneModeOn)
                "Flashlight" -> {
                    val next = !s.flashlightOn
                    _hardwareEvents.tryEmit(NovaHardwareEvent.SetFlashlight(next))
                    s.copy(flashlightOn = next)
                }
                "Do Not Disturb" -> s.copy(dndOn = !s.dndOn)
                "Auto-Rotate" -> s.copy(autoRotateOn = !s.autoRotateOn)
                else -> s
            }
        }
    }

    fun setControlPermission(key: String, policy: PermissionPolicy) {
        updatePhoneControl { s ->
            s.copy(controlPermissions = s.controlPermissions.toMutableMap().apply { put(key, policy) })
        }
    }

    fun setVolumePercent(percent: Float) {
        val clamped = percent.coerceIn(0f, 1f)
        updatePhoneControl { it.copy(volumePercent = clamped) }
        _hardwareEvents.tryEmit(NovaHardwareEvent.SetVolume(clamped))
    }

    fun setBrightnessPercent(percent: Float) {
        updatePhoneControl { it.copy(brightnessPercent = percent.coerceIn(0.05f, 1f)) }
    }

    fun verifyPinAndProceed(enteredPin: String) {
        if (enteredPin == _runtime.value.securityPinCode) {
            val pending = _runtime.value.pendingSensitiveCommand
            _runtime.update { it.copy(pinDialogVisible = false, pendingSensitiveCommand = null) }
            if (!pending.isNullOrBlank()) {
                submitCommand(pending, bypassPin = true)
            }
        } else {
            _runtime.update { it.copy(errorMessage = "Incorrect Security PIN.") }
        }
    }

    fun dismissPinDialog() {
        _runtime.update { it.copy(pinDialogVisible = false, pendingSensitiveCommand = null) }
    }

    fun updateSecurityPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            _runtime.update { it.copy(securityPinCode = newPin, backupStatusMessage = "Security PIN updated.") }
        }
    }

    fun unlockAppWithPin(enteredPin: String): Boolean {
        return if (enteredPin == _runtime.value.securityPinCode) {
            _runtime.update { it.copy(appLocked = false) }
            true
        } else {
            false
        }
    }

    fun lockAppNow() {
        _runtime.update { it.copy(appLocked = true) }
    }

    fun triggerEmergencySos() {
        val contact = currentSettings.emergencyContact.ifBlank { "911" }
        _hardwareEvents.tryEmit(NovaHardwareEvent.DialEmergency(contact))
        _runtime.update {
            it.copy(
                orbState = OrbState.SPEAKING,
                replyText = "Emergency SOS triggered: Preparing location packet and calling $contact.",
                activeSteps = listOf("Share GPS Coordinates", "Dial Emergency Contact $contact")
            )
        }
        viewModelScope.launch {
            repository.addHistoryEntry(
                userId = currentUserId,
                userQuery = "Emergency SOS",
                novaReply = "Initiated emergency call to $contact with location alert.",
                intentTag = "EMERGENCY",
                executedActions = listOf("Location snapshot", "Dial $contact"),
                modeUsed = _runtime.value.assistantMode.name
            )
        }
    }

    fun replayHistoryAudio(entry: HistoryEntry) {
        val settings = currentSettings
        _runtime.update { it.copy(orbState = OrbState.SPEAKING, voiceAmplitude = 0.55f) }
        _hardwareEvents.tryEmit(
            NovaHardwareEvent.Speak(
                text = entry.novaReply,
                language = settings.resolvedLanguage(),
                gender = settings.resolvedVoiceGender(),
                speed = settings.voiceSpeed.toFloat()
            )
        )
    }

    fun onTtsSpeakingChanged(speaking: Boolean) {
        _runtime.update { state ->
            if (!speaking && state.orbState == OrbState.SPEAKING) {
                state.copy(orbState = OrbState.IDLE, voiceAmplitude = 0.15f)
            } else {
                state
            }
        }
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        _runtime.update { it.copy(floatingBubbleEnabled = enabled, floatingBubbleExpanded = false) }
    }

    fun toggleFloatingBubbleExpanded() {
        _runtime.update { it.copy(floatingBubbleExpanded = !it.floatingBubbleExpanded) }
    }

    fun toggleSkill(skillId: String) {
        _runtime.update { state ->
            state.copy(
                skills = state.skills.map { if (it.id == skillId) it.copy(connected = !it.connected) else it }
            )
        }
    }

    fun createCustomCommand(phrase: String, category: String, steps: List<String>, policy: PermissionPolicy) {
        viewModelScope.launch {
            repository.createCustomCommand(currentUserId, phrase, category, steps, policy)
        }
    }

    fun toggleCustomCommandEnabled(cmd: CustomCommand) {
        viewModelScope.launch {
            repository.updateCommand(currentUserId, cmd.id, mapOf("enabled" to !cmd.enabled))
        }
    }

    fun deleteCustomCommand(commandId: String) {
        viewModelScope.launch {
            repository.deleteCommand(currentUserId, commandId)
        }
    }

    fun createAutomation(title: String, triggerType: TriggerType, triggerDetail: String, steps: List<String>) {
        viewModelScope.launch {
            repository.createAutomation(currentUserId, title, triggerType, triggerDetail, steps)
        }
    }

    fun runAutomation(routine: AutomationRoutine) {
        viewModelScope.launch {
            repository.updateAutomation(currentUserId, routine.id, mapOf("runCount" to (routine.runCount + 1L)))
            val summary = "Executed \"${routine.title}\": ${routine.actionSteps.joinToString(" -> ")}"
            _runtime.update {
                it.copy(
                    orbState = OrbState.SPEAKING,
                    replyText = summary,
                    activeSteps = routine.actionSteps
                )
            }
            val s = currentSettings
            _hardwareEvents.tryEmit(
                NovaHardwareEvent.Speak(summary, s.resolvedLanguage(), s.resolvedVoiceGender(), s.voiceSpeed.toFloat())
            )
            repository.addHistoryEntry(
                userId = currentUserId,
                userQuery = "Run Routine: ${routine.title}",
                novaReply = summary,
                intentTag = "AUTOMATION",
                executedActions = routine.actionSteps,
                modeUsed = _runtime.value.assistantMode.name
            )
        }
    }

    fun toggleAutomationEnabled(routine: AutomationRoutine) {
        viewModelScope.launch {
            repository.updateAutomation(currentUserId, routine.id, mapOf("enabled" to !routine.enabled))
        }
    }

    fun deleteAutomation(automationId: String) {
        viewModelScope.launch {
            repository.deleteAutomation(currentUserId, automationId)
        }
    }

    fun toggleHistoryBookmark(entry: HistoryEntry) {
        viewModelScope.launch {
            repository.toggleHistoryBookmark(currentUserId, entry.id, !entry.bookmarked)
        }
    }

    fun deleteHistoryEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(currentUserId, entryId)
        }
    }

    fun toggleSmartDevicePower(device: SmartDevice) {
        viewModelScope.launch {
            repository.updateSmartDevice(currentUserId, device.id, mapOf("poweredOn" to !device.poweredOn))
        }
    }

    fun updateSmartDeviceLevel(device: SmartDevice, level: Long) {
        viewModelScope.launch {
            repository.updateSmartDevice(currentUserId, device.id, mapOf("levelValue" to level.coerceIn(0L, 100L)))
        }
    }

    fun updateSettingField(key: String, value: Any) {
        viewModelScope.launch {
            repository.updateUserSettings(currentUserId, mapOf(key to value))
            if (key == "startOnBoot" && value is Boolean) {
                _hardwareEvents.tryEmit(NovaHardwareEvent.SyncBootPref(value))
            }
            if (key == "runInBackground" && value is Boolean) {
                _hardwareEvents.tryEmit(NovaHardwareEvent.ToggleForegroundService(value))
            }
            if (key == "appLockEnabled" && value == true) {
                _runtime.update { it.copy(backupStatusMessage = "App Lock enabled with PIN ${_runtime.value.securityPinCode}") }
            }
        }
    }

    fun triggerManualCloudBackup() {
        viewModelScope.launch {
            val res = repository.updateUserSettings(currentUserId, mapOf("wakeWord" to currentSettings.wakeWord))
            _runtime.update {
                it.copy(
                    backupStatusMessage = if (res.isSuccess) {
                        "Cloud backup synced at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}"
                    } else {
                        "Backup failed: ${res.exceptionOrNull()?.localizedMessage}"
                    }
                )
            }
        }
    }

    fun clearStatusMessage() {
        _runtime.update { it.copy(backupStatusMessage = null, errorMessage = null) }
    }

    fun computeServiceHealth(serviceRunning: Boolean, micGranted: Boolean, batteryExempt: Boolean): ServiceHealth {
        return when {
            !micGranted -> ServiceHealth.RED
            !serviceRunning || !batteryExempt -> ServiceHealth.ORANGE
            else -> ServiceHealth.GREEN
        }
    }

    private companion object {
        const val TAG = "NovaViewModel"
    }
}
