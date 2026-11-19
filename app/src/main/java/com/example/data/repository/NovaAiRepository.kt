package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.AssistantMode
import com.example.data.model.NovaCommandPlan
import com.example.data.model.NovaPersonality
import com.example.data.model.PhoneControlState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class NovaAiRepository {

    private val commandSchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(description = "Spoken natural response from Nova in the user's selected language and personality"),
            "intentTag" to Schema.string(description = "Short uppercase tag like MULTI_STEP, CONNECTIVITY, CALL, MESSAGE, BRIEFING, NAVIGATION, SMART_HOME, SCREEN_CONTROL, TRANSLATE, EMERGENCY, CHAT"),
            "actionSteps" to Schema.array(
                items = Schema.string(),
                description = "Concrete human-readable steps Nova executes in order (1 to 6 items)"
            ),
            "toggleKeys" to Schema.array(
                items = Schema.string(),
                description = "Any phone toggles affected from: WIFI_ON, WIFI_OFF, BT_ON, BT_OFF, DATA_ON, DATA_OFF, HOTSPOT_ON, HOTSPOT_OFF, AIRPLANE_ON, AIRPLANE_OFF, FLASH_ON, FLASH_OFF, DND_ON, DND_OFF, ROTATE_ON, ROTATE_OFF, VOL_UP, VOL_DOWN, BRIGHT_UP, BRIGHT_DOWN, LIGHTS_ON, LIGHTS_OFF, AC_ON, AC_OFF, TV_ON, TV_OFF"
            ),
            "targetApp" to Schema.string(
                description = "Optional app or target action such as YOUTUBE, SPOTIFY, MAPS, CAMERA, PHONE, WHATSAPP, ALARM, SCREEN_BACK, SCREEN_HOME, SCREEN_RECENTS, SCREEN_SHOT, NONE",
                nullable = true
            ),
            "requiresSensitivePin" to Schema.boolean(
                description = "True if the action involves making a call, payment, deleting data, or emergency SOS"
            )
        ),
        optionalProperties = listOf("targetApp")
    )

    private val commandModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = commandSchema
                temperature = 0.4f
            }
        )
    }

    private val conversationalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.65f
            }
        )
    }

    suspend fun interpretVoiceCommand(
        userPrompt: String,
        recentMemory: List<Pair<String, String>>,
        language: AppLanguage,
        personality: NovaPersonality,
        mode: AssistantMode,
        phoneState: PhoneControlState
    ): Result<NovaCommandPlan> {
        return try {
            val memoryContext = if (recentMemory.isEmpty()) {
                "No prior turns yet."
            } else {
                recentMemory.takeLast(4).joinToString("\n") { (q, a) -> "User: $q\nNova: $a" }
            }

            val fullPrompt = """
                You are Nova, a flagship autonomous Android AI voice assistant.
                Language to reply in: ${language.displayName}.
                Personality: ${personality.label} (${personality.promptHint}).
                Active Assistant Mode: ${mode.label} (${mode.subtitle}).
                Current Phone State: WiFi=${phoneState.wifiOn}, Bluetooth=${phoneState.bluetoothOn}, DND=${phoneState.dndOn}, Flashlight=${phoneState.flashlightOn}, Volume=${(phoneState.volumePercent * 100).toInt()}%, Brightness=${(phoneState.brightnessPercent * 100).toInt()}%.
                Recent Conversation Memory (use this for pronouns like "call him again" or follow-ups):
                $memoryContext

                User Request: "$userPrompt"

                Analyze the request, support multi-step chained commands in a single sentence, and return structured JSON matching the schema.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                commandModel.generateContent(fullPrompt)
            }

            val jsonText = response.text ?: throw IllegalStateException("Nova AI returned an empty response.")
            val json = JSONObject(jsonText)
            val replyText = json.optString("replyText").ifBlank { "Command executed." }
            val intentTag = json.optString("intentTag", "ASSISTANT")
            val stepsArray = json.optJSONArray("actionSteps")
            val actionSteps = if (stepsArray != null) {
                (0 until stepsArray.length()).mapNotNull { stepsArray.optString(it).takeIf(String::isNotBlank) }
            } else {
                emptyList()
            }
            val togglesArray = json.optJSONArray("toggleKeys")
            val toggleKeys = if (togglesArray != null) {
                (0 until togglesArray.length()).mapNotNull { togglesArray.optString(it).takeIf(String::isNotBlank) }
            } else {
                emptyList()
            }
            val targetApp = json.optString("targetApp").takeIf { it.isNotBlank() && it != "null" && it != "NONE" }
            val requiresPin = json.optBoolean("requiresSensitivePin", false)

            Result.success(
                NovaCommandPlan(
                    replyText = replyText,
                    intentTag = intentTag,
                    actionSteps = actionSteps.ifEmpty { listOf("Processed AI request") },
                    toggleKeys = toggleKeys,
                    targetApp = targetApp,
                    requiresSensitivePin = requiresPin
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Command interpretation timed out", e)
            Result.failure(IllegalStateException("Nova timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI command interpretation failed", e)
            Result.failure(e)
        }
    }

    fun streamDailyBriefing(
        language: AppLanguage,
        personality: NovaPersonality
    ): Flow<String> = flow {
        val prompt = """
            You are Nova, a futuristic Android voice assistant.
            Generate a crisp, executive Daily Briefing in ${language.displayName} with a ${personality.promptHint} tone.
            Include:
            1. Weather & Commute readiness
            2. Priority Schedule & Reminders
            3. Device Battery & Background Health check
            4. One proactive smart suggestion for today.
            Keep it under 120 words and easy to listen to aloud.
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            conversationalModel.generateContentStream(prompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Briefing returned empty response.")
        }
    }

    suspend fun analyzeVision(
        bitmap: Bitmap,
        taskPrompt: String,
        language: AppLanguage
    ): Result<String> {
        return try {
            val response = withTimeout(60_000L) {
                conversationalModel.generateContent(
                    content {
                        image(bitmap)
                        text("$taskPrompt\nRespond clearly in ${language.displayName}.")
                    }
                )
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Vision analysis returned an empty response."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vision analysis timed out", e)
            Result.failure(IllegalStateException("Vision analysis timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vision analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateText(
        sourceText: String,
        targetLanguage: AppLanguage
    ): Result<String> {
        return try {
            val response = withTimeout(45_000L) {
                conversationalModel.generateContent(
                    "Translate the following text accurately and naturally into ${targetLanguage.displayName}. Return only the translated text:\n\n$sourceText"
                )
            }
            val translated = response.text?.trim()
            if (translated.isNullOrBlank()) {
                Result.failure(IllegalStateException("Translation returned empty output."))
            } else {
                Result.success(translated)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            Result.failure(IllegalStateException("Translation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "NovaAiRepo"
    }
}
