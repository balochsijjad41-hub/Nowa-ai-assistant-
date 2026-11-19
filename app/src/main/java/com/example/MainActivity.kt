package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.AssistantMode
import com.example.data.model.OrbState
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.repository.NovaAiRepository
import com.example.data.repository.NovaRepository
import com.example.i18n.localizedStringsFor
import com.example.system.DeviceController
import com.example.system.NovaAccessibilityService
import com.example.system.NovaBackgroundService
import com.example.system.NovaServiceBridge
import com.example.system.configureAppCheck
import com.example.ui.components.FloatingNovaBubbleOverlay
import com.example.ui.screens.AuthOnboardingScreen
import com.example.ui.screens.AutomationsScreen
import com.example.ui.screens.CommandsScreen
import com.example.ui.screens.DrivingModeOverlay
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MicWakeWordTestDialog
import com.example.ui.screens.PermissionWizardDialog
import com.example.ui.screens.PinConfirmationDialog
import com.example.ui.screens.ScreenControlInspectorOverlay
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VisionAndTranslateDialog
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaOledBlack
import com.example.ui.theme.NovaTheme
import com.example.viewmodel.DataUiState
import com.example.viewmodel.NovaHardwareEvent
import com.example.viewmodel.NovaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

const val ROUTE_HOME = "home"
const val ROUTE_COMMANDS = "commands"
const val ROUTE_AUTOMATIONS = "automations"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val launchListen = intent?.getBooleanExtra(NovaBackgroundService.EXTRA_START_LISTENING, false) ?: false

        setContent {
            NovaAppRoot(startListeningOnLaunch = launchListen)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun NovaAppRoot(startListeningOnLaunch: Boolean = false) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        NovaTheme(themeMode = ThemeMode.DARK) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuthOnboardingScreen(onAuthSuccess = { /* authStateFlow updates automatically */ })
            }
        }
    } else {
        key(user.uid) {
            NovaAuthenticatedShell(
                currentUser = user,
                startListeningOnLaunch = startListeningOnLaunch
            )
        }
    }
}

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun NovaAuthenticatedShell(
    currentUser: FirebaseUser,
    startListeningOnLaunch: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: NovaViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NovaViewModel(
                    repository = NovaRepository(db),
                    aiRepository = NovaAiRepository(),
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val deviceController = remember(context) { DeviceController(context.applicationContext) }
    DisposableEffect(deviceController) {
        deviceController.initTts { speaking -> viewModel.onTtsSpeakingChanged(speaking) }
        onDispose { deviceController.release() }
    }

    val runtime by viewModel.runtime.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val commandsState by viewModel.commandsState.collectAsStateWithLifecycle()
    val automationsState by viewModel.automationsState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val smartDevicesState by viewModel.smartDevicesState.collectAsStateWithLifecycle()

    val serviceRunning by NovaServiceBridge.serviceRunning.collectAsStateWithLifecycle()
    val accessibilityConnected by NovaAccessibilityService.isConnected.collectAsStateWithLifecycle()
    val notificationSpeakTrigger by NovaServiceBridge.speakTriggerCount.collectAsStateWithLifecycle()

    val settings = (settingsState as? DataUiState.Success)?.data ?: UserSettings(userId = currentUser.uid)
    val language = settings.resolvedLanguage()
    val strings = localizedStringsFor(language)

    var permissionReport by remember { mutableStateOf(deviceController.inspectPermissions()) }
    var showPermissionWizard by remember { mutableStateOf(false) }
    var showMicTestDialog by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionReport = deviceController.inspectPermissions()
        if (granted) {
            viewModel.onStartListeningUi("Voice Orb")
            deviceController.startVoiceRecognition(
                language = language,
                onRmsChanged = { viewModel.onVoiceRmsChanged(it) },
                onPartialResult = { viewModel.onPartialTranscript(it) },
                onFinalResult = { final -> viewModel.submitCommand(final) },
                onError = { msg -> viewModel.onVoiceError(msg) }
            )
        } else {
            viewModel.onVoiceError("Microphone permission denied. Enable it in Setup Wizard or type a command.")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionReport = deviceController.inspectPermissions()
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionReport = deviceController.inspectPermissions()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionReport = deviceController.inspectPermissions()
    }

    val triggerOrbListening: () -> Unit = {
        if (runtime.orbState == OrbState.LISTENING) {
            deviceController.stopVoiceRecognition()
            viewModel.onOrbResetIdle()
        } else if (runtime.orbState == OrbState.SPEAKING) {
            deviceController.stopSpeaking()
            viewModel.onOrbResetIdle()
        } else {
            permissionReport = deviceController.inspectPermissions()
            if (permissionReport.microphoneGranted) {
                viewModel.onStartListeningUi("Voice Orb")
                deviceController.startVoiceRecognition(
                    language = language,
                    onRmsChanged = { viewModel.onVoiceRmsChanged(it) },
                    onPartialResult = { viewModel.onPartialTranscript(it) },
                    onFinalResult = { final -> viewModel.submitCommand(final) },
                    onError = { msg -> viewModel.onVoiceError(msg) }
                )
            } else {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Shake shortcut listener
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeMs = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                val gForce = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (gForce > 2.6f && now - lastShakeMs > 2500L) {
                    lastShakeMs = now
                    triggerOrbListening()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accel != null) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    LaunchedEffect(startListeningOnLaunch, notificationSpeakTrigger) {
        if (startListeningOnLaunch || notificationSpeakTrigger > 0) {
            triggerOrbListening()
        }
    }

    // Process hardware side-effects emitted by NovaViewModel
    LaunchedEffect(viewModel) {
        viewModel.hardwareEvents.collect { event ->
            when (event) {
                is NovaHardwareEvent.Speak -> {
                    deviceController.speak(event.text, event.language, event.gender, event.speed)
                }
                is NovaHardwareEvent.SetFlashlight -> {
                    deviceController.setFlashlight(event.enabled)
                }
                is NovaHardwareEvent.SetVolume -> {
                    deviceController.setMediaVolumePercent(event.percent)
                }
                is NovaHardwareEvent.LaunchApp -> {
                    deviceController.launchTargetAction(event.targetApp)
                }
                is NovaHardwareEvent.DialEmergency -> {
                    deviceController.dialEmergency(event.number)
                }
                is NovaHardwareEvent.SyncBootPref -> {
                    deviceController.persistBootPreference(event.startOnBoot)
                }
                is NovaHardwareEvent.ToggleForegroundService -> {
                    if (event.enabled) {
                        NovaBackgroundService.start(context)
                    } else {
                        NovaBackgroundService.stop(context)
                    }
                }
                is NovaHardwareEvent.AccessibilityAction -> {
                    val svc = NovaAccessibilityService.instance
                    when {
                        event.action == "SCREEN_BACK" -> svc?.performSystemBack()
                        event.action == "SCREEN_HOME" -> svc?.performSystemHome()
                        event.action == "SCREEN_RECENTS" -> svc?.performSystemRecents()
                        event.action == "SCREEN_SHOT" -> svc?.performSystemScreenshot()
                        event.action.startsWith("CLICK:") -> svc?.clickNodeByText(event.action.removePrefix("CLICK:"))
                    }
                }
            }
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var commandsInitialSubTab by remember { mutableIntStateOf(0) }

    BackHandler(enabled = selectedTabIndex != 0) {
        selectedTabIndex = 0
    }

    val destinations = listOf(
        NavDestination(ROUTE_HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
        NavDestination(ROUTE_COMMANDS, strings.navCommands, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
        NavDestination(ROUTE_AUTOMATIONS, strings.navAutomations, Icons.Filled.AutoAwesomeMotion, Icons.Outlined.AutoAwesomeMotion),
        NavDestination(ROUTE_HISTORY, strings.navHistory, Icons.Filled.History, Icons.Outlined.History),
        NavDestination(ROUTE_SETTINGS, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val accessibilityLarge = runtime.assistantMode == AssistantMode.ACCESSIBILITY

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        NovaTheme(
            themeMode = settings.resolvedTheme(),
            accessibilityLargeText = accessibilityLarge
        ) {
            val novaColors = LocalNovaColors.current
            val backgroundBrush = if (novaColors.isDark) {
                Brush.verticalGradient(listOf(NovaOledBlack, Color(0xFF080D1D), NovaOledBlack))
            } else {
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface))
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
            ) {
                val useRail = maxWidth >= 840.dp

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(
                                containerColor = novaColors.glassSurface.copy(alpha = 0.94f),
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                destinations.forEachIndexed { idx, dest ->
                                    NavigationBarItem(
                                        selected = selectedTabIndex == idx,
                                        onClick = {
                                            if (idx == 1) commandsInitialSubTab = 0
                                            selectedTabIndex = idx
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTabIndex == idx) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = novaColors.cyanAccent,
                                            selectedTextColor = novaColors.cyanAccent,
                                            indicatorColor = novaColors.cyanAccent.copy(alpha = 0.16f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_${dest.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail) {
                            NavigationRail(
                                containerColor = novaColors.glassSurface.copy(alpha = 0.94f)
                            ) {
                                destinations.forEachIndexed { idx, dest ->
                                    NavigationRailItem(
                                        selected = selectedTabIndex == idx,
                                        onClick = {
                                            if (idx == 1) commandsInitialSubTab = 0
                                            selectedTabIndex = idx
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTabIndex == idx) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label) }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 720.dp)
                            ) {
                                when (selectedTabIndex) {
                                    0 -> HomeScreen(
                                        viewModel = viewModel,
                                        runtime = runtime,
                                        settings = settings,
                                        strings = strings,
                                        onOrbTap = triggerOrbListening,
                                        onOpenControlPanel = {
                                            commandsInitialSubTab = 1
                                            selectedTabIndex = 1
                                        },
                                        onOpenMicLog = { selectedTabIndex = 3 },
                                        onOpenPermissionsWizard = {
                                            permissionReport = deviceController.inspectPermissions()
                                            showPermissionWizard = true
                                        }
                                    )
                                    1 -> CommandsScreen(
                                        viewModel = viewModel,
                                        runtime = runtime,
                                        commandsState = commandsState,
                                        deviceController = deviceController,
                                        initialSubTab = commandsInitialSubTab
                                    )
                                    2 -> AutomationsScreen(
                                        viewModel = viewModel,
                                        automationsState = automationsState,
                                        smartDevicesState = smartDevicesState
                                    )
                                    3 -> HistoryScreen(
                                        viewModel = viewModel,
                                        runtime = runtime,
                                        historyState = historyState,
                                        strings = strings
                                    )
                                    else -> SettingsScreen(
                                        viewModel = viewModel,
                                        runtime = runtime,
                                        settings = settings,
                                        userEmail = currentUser.email ?: currentUser.uid,
                                        serviceRunning = serviceRunning,
                                        permissionReport = permissionReport,
                                        deviceController = deviceController,
                                        onOpenPermissionWizard = {
                                            permissionReport = deviceController.inspectPermissions()
                                            showPermissionWizard = true
                                        },
                                        onOpenMicTest = { showMicTestDialog = true },
                                        onSignOut = {
                                            NovaBackgroundService.stop(context)
                                            scope.launch {
                                                withContext(NonCancellable) {
                                                    try {
                                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                                    } catch (e: Exception) {
                                                        Log.w("NovaAuth", "Clear credential failed: ${e.message}")
                                                    } finally {
                                                        Firebase.auth.signOut()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Assistant Bubble over any screen
                FloatingNovaBubbleOverlay(
                    enabled = runtime.floatingBubbleEnabled && runtime.assistantMode != AssistantMode.DRIVING,
                    expanded = runtime.floatingBubbleExpanded,
                    orbState = runtime.orbState,
                    transcript = runtime.liveTranscript.ifBlank { runtime.replyText },
                    onToggleExpand = { viewModel.toggleFloatingBubbleExpanded() },
                    onListenTap = triggerOrbListening,
                    onQuickSend = { cmd -> viewModel.submitCommand(cmd) },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                )

                // Full-Screen Driving Mode
                if (runtime.assistantMode == AssistantMode.DRIVING) {
                    DrivingModeOverlay(
                        viewModel = viewModel,
                        runtime = runtime,
                        onOrbTap = triggerOrbListening
                    )
                }

                // Visual Screen Control Inspector ("What's on my screen?")
                if (runtime.screenInspectorOpen) {
                    ScreenControlInspectorOverlay(
                        viewModel = viewModel,
                        runtime = runtime,
                        accessibilityConnected = accessibilityConnected,
                        deviceController = deviceController
                    )
                }

                // Camera Vision & Live Translation Sheet
                if (runtime.visionSheetOpen) {
                    VisionAndTranslateDialog(
                        viewModel = viewModel,
                        runtime = runtime,
                        onDismiss = { viewModel.toggleVisionSheet(false) }
                    )
                }

                // Permission Wizard Dialog
                if (showPermissionWizard) {
                    PermissionWizardDialog(
                        report = permissionReport,
                        deviceController = deviceController,
                        onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onRequestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onRequestLocation = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                        onDismiss = { showPermissionWizard = false }
                    )
                }

                // Mic & Wake-Word Test Dialog
                if (showMicTestDialog) {
                    MicWakeWordTestDialog(
                        wakeWord = settings.wakeWord,
                        amplitude = runtime.voiceAmplitude,
                        transcript = runtime.liveTranscript,
                        onStartTest = triggerOrbListening,
                        onDismiss = { showMicTestDialog = false }
                    )
                }

                // Sensitive Action PIN Confirmation Dialog
                if (runtime.pinDialogVisible) {
                    PinConfirmationDialog(
                        title = "Confirm Sensitive Action",
                        subtitle = "Enter your 4-digit Nova Security PIN to execute: \"${runtime.pendingSensitiveCommand.orEmpty()}\"",
                        onConfirmPin = { pin -> viewModel.verifyPinAndProceed(pin) },
                        onDismiss = { viewModel.dismissPinDialog() }
                    )
                }

                // App Lock Gate
                if (runtime.appLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NovaOledBlack.copy(alpha = 0.96f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PinConfirmationDialog(
                            title = "Nova App Lock",
                            subtitle = "Enter your 4-digit PIN to unlock Nova.",
                            onConfirmPin = { pin -> viewModel.unlockAppWithPin(pin) },
                            onDismiss = null
                        )
                    }
                }
            }
        }
    }
}
