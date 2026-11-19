package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbState
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaCrimson
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaViolet
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun NovaGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val novaColors = LocalNovaColors.current
    val shape = RoundedCornerShape(24.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(novaColors.glassSurface)
        .border(width = 1.dp, brush = novaColors.glassBorder, shape = shape)
        .let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        }
        .padding(contentPadding)

    Column(modifier = baseModifier, content = content)
}

@Composable
fun NovaAnimatedOrb(
    state: OrbState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onClick: () -> Unit = {}
) {
    val novaColors = LocalNovaColors.current
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "nova_orb")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.THINKING -> 1600
                    OrbState.LISTENING -> 3800
                    OrbState.SPEAKING -> 2800
                    OrbState.ERROR -> 1200
                    OrbState.IDLE -> 9000
                },
                easing = LinearEasing
            )
        ),
        label = "orb_rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.IDLE -> 2400
                    OrbState.LISTENING -> 650
                    OrbState.THINKING -> 550
                    OrbState.SPEAKING -> 450
                    OrbState.ERROR -> 350
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0.05f, 1.0f),
        animationSpec = spring(dampingRatio = 0.6f),
        label = "orb_amp"
    )

    val primaryColor = when (state) {
        OrbState.IDLE -> novaColors.cyanAccent
        OrbState.LISTENING -> NovaCyan
        OrbState.THINKING -> NovaViolet
        OrbState.SPEAKING -> NovaMagenta
        OrbState.ERROR -> NovaCrimson
    }
    val secondaryColor = when (state) {
        OrbState.IDLE -> novaColors.violetAccent
        OrbState.LISTENING -> NovaViolet
        OrbState.THINKING -> NovaMagenta
        OrbState.SPEAKING -> NovaCyan
        OrbState.ERROR -> Color(0xFFF97316)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { contentDescription = "Nova voice orb, state ${state.name}" }
            .testTag("nova_voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val dynamicScale = pulse + (animatedAmp * 0.14f)

            // Outer ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.38f),
                        secondaryColor.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * dynamicScale
                ),
                radius = radius * dynamicScale,
                center = center
            )

            // Outer orbital telemetry ring
            rotate(degrees = rotation, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.9f),
                            secondaryColor.copy(alpha = 0.85f),
                            NovaMagenta.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
                    size = Size(radius * 1.56f, radius * 1.56f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Counter-rotating inner plasma ring
            rotate(degrees = -rotation * 1.4f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryColor,
                            primaryColor,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 60f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.64f, center.y - radius * 0.64f),
                    size = Size(radius * 1.28f, radius * 1.28f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3D-shaded spherical core
            val coreRadius = radius * (0.48f + animatedAmp * 0.08f) * (if (state == OrbState.IDLE) 1f else pulse * 0.98f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor,
                        secondaryColor,
                        Color(0xFF0A0F24)
                    ),
                    center = Offset(center.x - coreRadius * 0.28f, center.y - coreRadius * 0.28f),
                    radius = coreRadius * 1.35f
                ),
                radius = coreRadius,
                center = center
            )

            // Specular highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.32f),
                radius = coreRadius * 0.24f,
                center = Offset(center.x - coreRadius * 0.34f, center.y - coreRadius * 0.34f)
            )
        }
    }
}

@Composable
fun NovaWaveformVisualizer(
    state: OrbState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "wave_phase"
    )
    val novaColors = LocalNovaColors.current

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val barCount = 28
        val spacing = size.width / (barCount * 1.45f)
        val barWidth = spacing * 0.68f
        val startX = (size.width - (barCount * spacing)) / 2f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val norm = i.toFloat() / barCount
            val envelope = sin(norm * PI).toFloat()
            val motion = when (state) {
                OrbState.IDLE -> 0.18f + 0.12f * sin(phase + i * 0.45f)
                OrbState.LISTENING -> 0.25f + (amplitude.coerceIn(0.1f, 1f) * 0.72f) * kotlin.math.abs(sin(phase * 2f + i * 0.55f))
                OrbState.THINKING -> 0.35f + 0.45f * kotlin.math.abs(sin(phase * 3f + i * 0.7f))
                OrbState.SPEAKING -> 0.30f + 0.65f * kotlin.math.abs(sin(phase * 2.5f + i * 0.4f))
                OrbState.ERROR -> 0.22f
            }
            val barHeight = (size.height * 0.88f * envelope * motion).coerceAtLeast(5.dp.toPx())
            val color = when {
                state == OrbState.ERROR -> NovaCrimson
                norm < 0.33f -> novaColors.cyanAccent
                norm < 0.66f -> novaColors.violetAccent
                else -> novaColors.magentaAccent
            }
            val x = startX + i * spacing
            drawRoundRect(
                color = color.copy(alpha = 0.9f),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun MicPrivacyIndicatorPill(
    isListening: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isListening) NovaEmerald.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(
            1.dp,
            if (isListening) NovaEmerald.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isListening) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isListening) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isListening) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FloatingNovaBubbleOverlay(
    enabled: Boolean,
    expanded: Boolean,
    orbState: OrbState,
    transcript: String,
    onToggleExpand: () -> Unit,
    onListenTap: () -> Unit,
    onQuickSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    var offsetX by remember { mutableFloatStateOf(24f) }
    var offsetY by remember { mutableFloatStateOf(260f) }
    var miniText by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, 820f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(60f, 1700f)
                    }
                }
        ) {
            AnimatedVisibility(
                visible = !expanded,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    onClick = onToggleExpand,
                    shape = CircleShape,
                    color = Color(0xEB0B1021),
                    border = BorderStroke(1.5.dp, LocalNovaColors.current.orbGradient),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("floating_nova_bubble")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Open floating Nova mini assistant",
                            tint = NovaCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                NovaGlassCard(
                    modifier = Modifier
                        .width(296.dp)
                        .testTag("floating_nova_card"),
                    contentPadding = PaddingValues(14.dp)
                ) {
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
                                    .background(if (orbState == OrbState.LISTENING) NovaEmerald else NovaCyan)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Nova Mini Bubble",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Minimize bubble", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = transcript.ifBlank { "Floating overlay active. Speak or type a quick command." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onListenTap,
                            colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Speak", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedTextField(
                            value = miniText,
                            onValueChange = { miniText = it },
                            placeholder = { Text("Quick cmd...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        IconButton(
                            onClick = {
                                if (miniText.isNotBlank()) {
                                    onQuickSend(miniText)
                                    miniText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send mini command", tint = NovaCyan)
                        }
                    }
                }
            }
        }
    }
}
