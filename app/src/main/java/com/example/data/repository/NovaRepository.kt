package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.AutomationRoutine
import com.example.data.model.CustomCommand
import com.example.data.model.DeviceType
import com.example.data.model.HistoryEntry
import com.example.data.model.ListeningMode
import com.example.data.model.NovaPersonality
import com.example.data.model.PermissionPolicy
import com.example.data.model.SmartDevice
import com.example.data.model.ThemeMode
import com.example.data.model.TriggerType
import com.example.data.model.UserSettings
import com.example.data.model.VoiceGender
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NovaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    fun observeUserSettings(userId: String): Flow<UserSettings?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, "users/$userId", userId)
                throw error
            }

    fun observeCommands(userId: String): Flow<List<CustomCommand>> =
        db.collection("users").document(userId).collection("commands")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustomCommand::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "users/$userId/commands", userId)
                throw error
            }

    fun observeAutomations(userId: String): Flow<List<AutomationRoutine>> =
        db.collection("users").document(userId).collection("automations")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AutomationRoutine::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "users/$userId/automations", userId)
                throw error
            }

    fun observeHistory(userId: String): Flow<List<HistoryEntry>> =
        db.collection("users").document(userId).collection("history")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(HistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "users/$userId/history", userId)
                throw error
            }

    fun observeSmartDevices(userId: String): Flow<List<SmartDevice>> =
        db.collection("users").document(userId).collection("smartDevices")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SmartDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "users/$userId/smartDevices", userId)
                throw error
            }

    private fun logStreamErrorIfActive(
        error: Throwable,
        op: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun seedDefaultsIfNeeded(userId: String): Result<Unit> {
        val userPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val userDoc = db.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                val settingsPayload = mapOf(
                    "userId" to uid,
                    "wakeWord" to "Hey Nova",
                    "voiceGender" to VoiceGender.FEMALE.name,
                    "voiceSpeed" to 1.0,
                    "language" to AppLanguage.EN.code,
                    "personality" to NovaPersonality.FRIENDLY.name,
                    "themeMode" to ThemeMode.DARK.name,
                    "listeningMode" to ListeningMode.ALWAYS_ON.name,
                    "alwaysListening" to true,
                    "runInBackground" to true,
                    "startOnBoot" to true,
                    "keepAlive" to true,
                    "onDeviceProcessing" to false,
                    "speakerRecognition" to true,
                    "autoDeleteDays" to 30L,
                    "pinConfirmationEnabled" to true,
                    "appLockEnabled" to false,
                    "emergencyContact" to "+1 (555) 019-9110",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).set(settingsPayload).await()
            }

            val existingAutomations = db.collection("users").document(uid)
                .collection("automations").limit(1).get().await()
            if (existingAutomations.isEmpty) {
                val defaults = listOf(
                    Triple(
                        "Good Morning",
                        TriggerType.TIME.name to "07:00 AM Daily",
                        listOf("Disable Sleep DND", "Read daily briefing", "Set media volume 65%", "Turn on kitchen lights")
                    ),
                    Triple(
                        "Bedtime",
                        TriggerType.VOICE.name to "\"Hey Nova, goodnight\"",
                        listOf("Enable Do Not Disturb", "Dim brightness to 15%", "Lock front door", "Set 7:00 AM alarm")
                    ),
                    Triple(
                        "Driving",
                        TriggerType.LOCATION.name to "Car Bluetooth connected",
                        listOf("Launch Driving Mode", "Read incoming SMS aloud", "Open Maps navigation", "Play commute mix")
                    ),
                    Triple(
                        "Work",
                        TriggerType.TIME.name to "09:00 AM Weekdays",
                        listOf("Enable Work Mode", "Silence social alerts", "Read calendar events", "Set volume to 35%")
                    )
                )
                defaults.forEach { (title, trigger, steps) ->
                    val docRef = db.collection("users").document(uid).collection("automations").document()
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "title" to title,
                            "triggerType" to trigger.first,
                            "triggerDetail" to trigger.second,
                            "actionSteps" to steps,
                            "enabled" to true,
                            "runCount" to 1L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val existingCommands = db.collection("users").document(uid)
                .collection("commands").limit(1).get().await()
            if (existingCommands.isEmpty) {
                val starterCommands = listOf(
                    mapOf(
                        "phrase" to "Turn on WiFi, open YouTube and play music",
                        "category" to "CONNECTIVITY",
                        "actionsSummary" to "Enable WiFi -> Launch YouTube -> Play chill mix",
                        "actionSteps" to listOf("Enable WiFi", "Open YouTube", "Play Music"),
                        "permissionPolicy" to PermissionPolicy.ALWAYS_ALLOW.name,
                        "usageCount" to 4L
                    ),
                    mapOf(
                        "phrase" to "Focus desk mode",
                        "category" to "VOLUME_BRIGHTNESS",
                        "actionsSummary" to "Set DND ON -> Brightness 60% -> Open Spotify",
                        "actionSteps" to listOf("Do Not Disturb ON", "Set Brightness 60%", "Open Spotify"),
                        "permissionPolicy" to PermissionPolicy.ALWAYS_ALLOW.name,
                        "usageCount" to 2L
                    ),
                    mapOf(
                        "phrase" to "Read my unread messages",
                        "category" to "CALLS_SMS",
                        "actionsSummary" to "Scan notifications -> Speak WhatsApp & SMS summary",
                        "actionSteps" to listOf("Read WhatsApp", "Read SMS", "Prompt voice reply"),
                        "permissionPolicy" to PermissionPolicy.ASK_EACH_TIME.name,
                        "usageCount" to 5L
                    )
                )
                starterCommands.forEach { cmd ->
                    val docRef = db.collection("users").document(uid).collection("commands").document()
                    docRef.set(
                        cmd + mapOf(
                            "userId" to uid,
                            "enabled" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val existingDevices = db.collection("users").document(uid)
                .collection("smartDevices").limit(1).get().await()
            if (existingDevices.isEmpty) {
                val devices = listOf(
                    mapOf("name" to "Studio Ceiling Lights", "room" to "Living Room", "deviceType" to DeviceType.LIGHT.name, "poweredOn" to true, "levelValue" to 82L),
                    mapOf("name" to "Bedroom Climate AC", "room" to "Master Suite", "deviceType" to DeviceType.AC.name, "poweredOn" to true, "levelValue" to 22L),
                    mapOf("name" to "OLED Cinema TV", "room" to "Living Room", "deviceType" to DeviceType.TV.name, "poweredOn" to false, "levelValue" to 40L),
                    mapOf("name" to "Front Door Smart Lock", "room" to "Entryway", "deviceType" to DeviceType.LOCK.name, "poweredOn" to true, "levelValue" to 100L)
                )
                devices.forEach { device ->
                    val docRef = db.collection("users").document(uid).collection("smartDevices").document()
                    docRef.set(
                        device + mapOf(
                            "userId" to uid,
                            "voiceEnabled" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserSettings(userId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val sanitized = updates.toMutableMap().apply {
                remove("userId")
                remove("createdAt")
                remove("id")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(sanitized).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createCustomCommand(
        userId: String,
        phrase: String,
        category: String,
        actionSteps: List<String>,
        permissionPolicy: PermissionPolicy
    ): Result<Unit> {
        val path = "users/$userId/commands"
        return try {
            val uid = requireUserId(userId)
            val cleanPhrase = phrase.trim().take(120)
            val cleanSteps = actionSteps.map { it.trim() }.filter { it.isNotBlank() }.take(10)
            require(cleanPhrase.isNotEmpty()) { "Command phrase is required." }
            require(cleanSteps.isNotEmpty()) { "At least one action step is required." }
            val summary = cleanSteps.joinToString(" -> ").take(300)
            val docRef = db.collection("users").document(uid).collection("commands").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "phrase" to cleanPhrase,
                    "category" to category.take(40),
                    "actionsSummary" to summary,
                    "actionSteps" to cleanSteps,
                    "permissionPolicy" to permissionPolicy.name,
                    "usageCount" to 0L,
                    "enabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCommand(userId: String, commandId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "users/$userId/commands/$commandId"
        return try {
            val uid = requireUserId(userId)
            require(commandId.isNotBlank())
            val sanitized = updates.toMutableMap().apply {
                remove("userId")
                remove("createdAt")
                remove("id")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).collection("commands").document(commandId)
                .update(sanitized).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCommand(userId: String, commandId: String): Result<Unit> {
        val path = "users/$userId/commands/$commandId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).collection("commands").document(commandId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createAutomation(
        userId: String,
        title: String,
        triggerType: TriggerType,
        triggerDetail: String,
        actionSteps: List<String>
    ): Result<Unit> {
        val path = "users/$userId/automations"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim().take(80)
            val cleanTrigger = triggerDetail.trim().ifBlank { triggerType.label }.take(120)
            val cleanSteps = actionSteps.map { it.trim() }.filter { it.isNotBlank() }.take(12)
            require(cleanTitle.isNotEmpty() && cleanSteps.isNotEmpty())
            val docRef = db.collection("users").document(uid).collection("automations").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "triggerType" to triggerType.name,
                    "triggerDetail" to cleanTrigger,
                    "actionSteps" to cleanSteps,
                    "enabled" to true,
                    "runCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAutomation(userId: String, automationId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "users/$userId/automations/$automationId"
        return try {
            val uid = requireUserId(userId)
            val sanitized = updates.toMutableMap().apply {
                remove("userId")
                remove("createdAt")
                remove("id")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).collection("automations").document(automationId)
                .update(sanitized).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAutomation(userId: String, automationId: String): Result<Unit> {
        val path = "users/$userId/automations/$automationId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).collection("automations").document(automationId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addHistoryEntry(
        userId: String,
        userQuery: String,
        novaReply: String,
        intentTag: String,
        executedActions: List<String>,
        modeUsed: String
    ): Result<Unit> {
        val path = "users/$userId/history"
        return try {
            val uid = requireUserId(userId)
            val cleanQuery = userQuery.trim().take(1000).ifEmpty { "Voice input" }
            val cleanReply = novaReply.trim().take(4000).ifEmpty { "Done." }
            val cleanTag = intentTag.trim().take(60).ifEmpty { "ASSISTANT" }
            val cleanActions = executedActions.map { it.trim() }.filter { it.isNotEmpty() }.take(12)
            val docRef = db.collection("users").document(uid).collection("history").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "userQuery" to cleanQuery,
                    "novaReply" to cleanReply,
                    "intentTag" to cleanTag,
                    "executedActions" to cleanActions,
                    "modeUsed" to modeUsed.take(40),
                    "bookmarked" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleHistoryBookmark(userId: String, entryId: String, bookmarked: Boolean): Result<Unit> {
        val path = "users/$userId/history/$entryId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).collection("history").document(entryId)
                .update(
                    mapOf(
                        "bookmarked" to bookmarked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteHistoryEntry(userId: String, entryId: String): Result<Unit> {
        val path = "users/$userId/history/$entryId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).collection("history").document(entryId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSmartDevice(userId: String, deviceId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "users/$userId/smartDevices/$deviceId"
        return try {
            val uid = requireUserId(userId)
            val sanitized = updates.toMutableMap().apply {
                remove("userId")
                remove("createdAt")
                remove("id")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).collection("smartDevices").document(deviceId)
                .update(sanitized).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
