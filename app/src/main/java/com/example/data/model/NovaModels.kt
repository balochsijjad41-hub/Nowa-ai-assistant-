package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrbState {
    IDLE, LISTENING, THINKING, SPEAKING, ERROR
}

enum class VoiceGender(val label: String) {
    FEMALE("Female (Nova Aria)"),
    MALE("Male (Nova Orion)")
}

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    EN("EN", "English", false),
    UR("UR", "اردو (Urdu)", true),
    HI("HI", "हिन्दी (Hindi)", false);

    companion object {
        fun fromCode(raw: String): AppLanguage =
            entries.firstOrNull { it.code == raw } ?: EN
    }
}

typealias LanguageOption = AppLanguage

enum class NovaPersonality(val label: String, val promptHint: String) {
    PROFESSIONAL("Professional", "concise, crisp, executive tone"),
    FRIENDLY("Friendly", "warm, encouraging, helpful tone"),
    FUNNY("Funny", "witty, clever, playful tone")
}

enum class ThemeMode(val label: String) {
    DARK("Dark OLED"),
    LIGHT("Light"),
    AUTO("Auto")
}

enum class ListeningMode(val label: String, val detail: String) {
    ALWAYS_ON("Always On", "24/7 wake-word detection"),
    SCREEN_ON("Screen On Only", "Active when display is awake"),
    CHARGING("Charging Only", "Zero battery drain on the go"),
    SCHEDULED("Scheduled Hours", "07:00 AM – 11:00 PM"),
    LOW_POWER("Low-Power Mode", "Reduced DSP sampling rate")
}

enum class TriggerType(val label: String) {
    VOICE("Voice"),
    TIME("Time"),
    LOCATION("Location"),
    CHARGING("Charging"),
    APP_OPEN("App Open")
}

enum class PermissionPolicy(val label: String) {
    ALWAYS_ALLOW("Always allow"),
    ASK_EACH_TIME("Ask each time"),
    BLOCK("Block")
}

enum class DeviceType(val label: String) {
    LIGHT("Lights"),
    AC("Climate AC"),
    TV("Smart TV"),
    LOCK("Smart Lock"),
    SPEAKER("Smart Speaker")
}

enum class AssistantMode(val label: String, val subtitle: String) {
    NORMAL("Standard", "Full autonomous assistant"),
    DRIVING("Driving", "Hands-free large-touch interface"),
    SLEEP("Sleep", "DND, dim display & soft replies"),
    WORK("Work", "Focus silence & meeting briefings"),
    KIDS_SAFE("Kids-Safe", "Filtered content & locked controls"),
    ACCESSIBILITY("Accessibility", "Large text & spoken screen guide")
}

enum class ServiceHealth {
    GREEN, ORANGE, RED
}

@Keep
@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val wakeWord: String = "Hey Nova",
    val voiceGender: String = VoiceGender.FEMALE.name,
    val voiceSpeed: Double = 1.0,
    val language: String = AppLanguage.EN.code,
    val personality: String = NovaPersonality.FRIENDLY.name,
    val themeMode: String = ThemeMode.DARK.name,
    val listeningMode: String = ListeningMode.ALWAYS_ON.name,
    val alwaysListening: Boolean = true,
    val runInBackground: Boolean = true,
    val startOnBoot: Boolean = true,
    val keepAlive: Boolean = true,
    val onDeviceProcessing: Boolean = false,
    val speakerRecognition: Boolean = true,
    val autoDeleteDays: Long = 30L,
    val pinConfirmationEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val emergencyContact: String = "+1 (555) 019-9110",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedLanguage(): AppLanguage = AppLanguage.fromCode(language)
    fun resolvedTheme(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == themeMode } ?: ThemeMode.DARK
    fun resolvedPersonality(): NovaPersonality = NovaPersonality.entries.firstOrNull { it.name == personality } ?: NovaPersonality.FRIENDLY
    fun resolvedVoiceGender(): VoiceGender = VoiceGender.entries.firstOrNull { it.name == voiceGender } ?: VoiceGender.FEMALE
    fun resolvedListeningMode(): ListeningMode = ListeningMode.entries.firstOrNull { it.name == listeningMode } ?: ListeningMode.ALWAYS_ON
}

@Keep
@IgnoreExtraProperties
data class CustomCommand(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phrase: String = "",
    val category: String = "CUSTOM",
    val actionsSummary: String = "",
    val actionSteps: List<String> = emptyList(),
    val permissionPolicy: String = PermissionPolicy.ALWAYS_ALLOW.name,
    val usageCount: Long = 0L,
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedPolicy(): PermissionPolicy =
        PermissionPolicy.entries.firstOrNull { it.name == permissionPolicy } ?: PermissionPolicy.ALWAYS_ALLOW
}

@Keep
@IgnoreExtraProperties
data class AutomationRoutine(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val triggerType: String = TriggerType.VOICE.name,
    val triggerDetail: String = "",
    val actionSteps: List<String> = emptyList(),
    val enabled: Boolean = true,
    val runCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedTrigger(): TriggerType =
        TriggerType.entries.firstOrNull { it.name == triggerType } ?: TriggerType.VOICE
}

@Keep
@IgnoreExtraProperties
data class HistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userQuery: String = "",
    val novaReply: String = "",
    val intentTag: String = "ASSISTANT",
    val executedActions: List<String> = emptyList(),
    val modeUsed: String = AssistantMode.NORMAL.name,
    val bookmarked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SmartDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val room: String = "",
    val deviceType: String = DeviceType.LIGHT.name,
    val poweredOn: Boolean = false,
    val levelValue: Long = 70L,
    val voiceEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedType(): DeviceType =
        DeviceType.entries.firstOrNull { it.name == deviceType } ?: DeviceType.LIGHT
}

data class PhoneControlState(
    val wifiOn: Boolean = true,
    val bluetoothOn: Boolean = true,
    val mobileDataOn: Boolean = true,
    val hotspotOn: Boolean = false,
    val airplaneModeOn: Boolean = false,
    val flashlightOn: Boolean = false,
    val dndOn: Boolean = false,
    val autoRotateOn: Boolean = true,
    val volumePercent: Float = 0.65f,
    val brightnessPercent: Float = 0.72f,
    val controlPermissions: Map<String, PermissionPolicy> = mapOf(
        "WiFi" to PermissionPolicy.ALWAYS_ALLOW,
        "Bluetooth" to PermissionPolicy.ALWAYS_ALLOW,
        "Mobile Data" to PermissionPolicy.ASK_EACH_TIME,
        "Hotspot" to PermissionPolicy.ASK_EACH_TIME,
        "Airplane Mode" to PermissionPolicy.ASK_EACH_TIME,
        "Flashlight" to PermissionPolicy.ALWAYS_ALLOW,
        "Do Not Disturb" to PermissionPolicy.ALWAYS_ALLOW,
        "Auto-Rotate" to PermissionPolicy.ALWAYS_ALLOW
    )
)

data class MicAccessLogEntry(
    val id: String,
    val source: String,
    val reason: String,
    val timestampLabel: String
)

data class ScreenHighlightNode(
    val title: String,
    val description: String,
    val actionHint: String,
    val xFraction: Float,
    val yFraction: Float
)

data class SkillAppItem(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val samplePhrase: String,
    val connected: Boolean
)

data class VoiceCategoryItem(
    val id: String,
    val title: String,
    val examples: List<String>
)

data class ProactiveInsight(
    val id: String,
    val title: String,
    val detail: String,
    val commandPrompt: String,
    val badge: String
)

data class NovaCommandPlan(
    val replyText: String,
    val intentTag: String,
    val actionSteps: List<String>,
    val toggleKeys: List<String>,
    val targetApp: String?,
    val requiresSensitivePin: Boolean
)
