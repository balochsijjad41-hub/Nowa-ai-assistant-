package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.OrbState
import com.example.i18n.localizedStringsFor
import com.example.ui.components.NovaAnimatedOrb
import com.example.ui.components.NovaGlassCard
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaOledBlack
import com.example.ui.theme.NovaViolet
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "NovaAuth"

@Composable
fun AuthOnboardingScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val strings = localizedStringsFor(AppLanguage.EN)

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Interactive Sign-In stays ready */ },
            scope = scope
        )
    }

    val pagerState = rememberPagerState(pageCount = { 3 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(NovaOledBlack, Color(0xFF0A0E1E), NovaOledBlack)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NovaCyan, NovaMagenta)))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "NOVA OS",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (pagerState.currentPage < 2) {
                    TextButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
                    ) {
                        Text("Skip", color = NovaCyan)
                    }
                }
            }

            // 3 Intro Slides
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> {
                            NovaAnimatedOrb(
                                state = OrbState.LISTENING,
                                amplitude = 0.45f,
                                size = 210.dp
                            )
                            Spacer(Modifier.height(24.dp))
                            Icon(Icons.Default.Mic, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Just speak",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Natural multi-step voice commands with real-time Gemini reasoning, streaming replies, and English, Urdu & Hindi support.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                        1 -> {
                            NovaGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_nova_hero_aura),
                                    contentDescription = "Nova phone automation showcase",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NovaViolet, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Control your whole phone",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Toggle WiFi, Bluetooth, Flashlight, read notifications aloud, click on-screen buttons, and automate routines hands-free.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            NovaGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_nova_privacy_shield),
                                    contentDescription = "Nova encrypted privacy shield",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Icon(Icons.Default.Security, contentDescription = null, tint = NovaMagenta, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Private and always ready",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Live mic-in-use badge, PIN locks for sensitive actions, on-device mode, and reliable background wake-word listening.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Pager dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val active = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) NovaCyan else Color(0xFF334155))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Google Sign-In Card
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.signInTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.signInSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )

                errorMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFEF4444)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NovaCyan,
                        contentColor = NovaOledBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NovaOledBlack,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign-in failed. Please try again.")
        }
    }
}
