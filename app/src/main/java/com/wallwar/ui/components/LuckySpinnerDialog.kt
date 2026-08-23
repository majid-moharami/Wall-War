package com.wallwar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wallwar.data.DailySpinnerManager
import com.wallwar.data.SpinOutcome
import com.wallwar.data.SpinnerState
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LuckySpinnerDialog(
    userCoins: Int,
    spinnerState: SpinnerState,
    onSpin: (isFree: Boolean) -> SpinOutcome,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var currentOutcome by remember { mutableStateOf<SpinOutcome?>(null) }
    var showOutcomeDialog by remember { mutableStateOf(false) }

    val rotationAngle = remember { Animatable(0f) }
    val segments = DailySpinnerManager.SEGMENTS
    val spinFee = DailySpinnerManager.SPIN_FEE_COINS

    // Subtle ambient rotation glow
    val infiniteTransition = rememberInfiniteTransition(label = "spinner_ambient")
    val bulbPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bulb_pulse"
    )

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101A)),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonMagenta, NeonAmber))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("lucky_spinner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sleek Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lucky Wheel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (spinnerState.canSpinToday) "Cost: 🪙 $spinFee • 1 Spin / Day" else "Already Spun Today • 1 Spin / Day",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (spinnerState.canSpinToday) NeonAmber else Color(0xFFA0ACCC)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSpinning,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Clean Minimalist Wheel
                Box(
                    modifier = Modifier.size(270.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val radius = (canvasWidth / 2f) - 10.dp.toPx()
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val anglePerSegment = 360f / segments.size

                        // 1. Sleek Outer Rim
                        drawCircle(
                            color = Color(0xFF131A2A),
                            radius = radius + 6.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(NeonCyan, NeonMagenta, NeonAmber, NeonEmerald, NeonCyan)),
                            radius = radius + 6.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // 2. Wheel Segments
                        rotate(rotationAngle.value, pivot = center) {
                            segments.forEachIndexed { i, segment ->
                                val startAngle = i * anglePerSegment - 90f - (anglePerSegment / 2f)
                                val segmentColor = Color(segment.colorHex)

                                // Slice
                                drawArc(
                                    color = segmentColor,
                                    startAngle = startAngle,
                                    sweepAngle = anglePerSegment,
                                    useCenter = true,
                                    size = Size(radius * 2f, radius * 2f),
                                    topLeft = Offset(center.x - radius, center.y - radius)
                                )

                                // Slice Border Divider
                                val angleRad = Math.toRadians((startAngle + anglePerSegment).toDouble())
                                val lineEnd = Offset(
                                    (center.x + radius * cos(angleRad)).toFloat(),
                                    (center.y + radius * sin(angleRad)).toFloat()
                                )
                                drawLine(
                                    color = Color(0xFF090D18),
                                    start = center,
                                    end = lineEnd,
                                    strokeWidth = 2.5.dp.toPx()
                                )

                                // Simple Crisp Segment Content (Icon + Value)
                                val midAngle = Math.toRadians((startAngle + anglePerSegment / 2f).toDouble())
                                val textDistance = radius * 0.64f
                                val textX = (center.x + textDistance * cos(midAngle)).toFloat()
                                val textY = (center.y + textDistance * sin(midAngle)).toFloat()

                                drawContext.canvas.nativeCanvas.apply {
                                    save()
                                    rotate((startAngle + anglePerSegment / 2f + 90f), textX, textY)
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 30f
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    drawText("${segment.icon} ${segment.label}", textX, textY, paint)
                                    restore()
                                }
                            }
                        }

                        // 3. Center Hub
                        drawCircle(
                            color = Color(0xFF0C101A),
                            radius = radius * 0.22f,
                            center = center
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(NeonCyan, NeonMagenta, NeonAmber, NeonCyan)),
                            radius = radius * 0.22f,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = NeonAmber,
                            radius = radius * 0.07f,
                            center = center
                        )
                    }

                    // Center Hub Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C101A))
                            .border(1.5.dp, NeonAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⚡", fontSize = 18.sp)
                    }

                    // Pointer Needle at top center
                    Canvas(
                        modifier = Modifier
                            .size(28.dp, 28.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(2f, 0f)
                            lineTo(size.width - 2f, 0f)
                            close()
                        }
                        drawPath(path, color = NeonAmber)
                        drawPath(path, color = Color.White, style = Stroke(width = 1.5.dp.toPx()))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Drop Probability Notice Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131B2E))
                        .border(1.dp, Color(0xFF233252), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚠️ Notice: Exclusive Ball Skins (Cyber Core, Quantum Energy, Micro Blackhole) have rare drop chance in the Wheel.",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF93C5FD),
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // User Balance Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Balance: 🪙 $userCoins",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0ACCC)
                    )
                    Text(
                        text = "Cost: 🪙 $spinFee",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonAmber
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spin Action Button
                val canSpinToday = spinnerState.canSpinToday
                val canAffordPaid = userCoins >= spinFee
                val canSpin = canSpinToday && canAffordPaid && !isSpinning

                Button(
                    onClick = {
                        if (!canSpin) return@Button
                        isSpinning = true
                        val outcome = onSpin(false)
                        currentOutcome = outcome

                        coroutineScope.launch {
                            val target = rotationAngle.value + outcome.targetAngleDegrees
                            rotationAngle.animateTo(
                                targetValue = target,
                                animationSpec = tween(
                                    durationMillis = 4000,
                                    easing = FastOutSlowInEasing
                                )
                            )
                            isSpinning = false
                            showOutcomeDialog = true
                        }
                    },
                    enabled = canSpin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        disabledContainerColor = Color(0xFF1B2232),
                        disabledContentColor = Color(0xFF6B7A99)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_spin_wheel")
                ) {
                    if (isSpinning) {
                        Text(
                            text = "Spinning...",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    } else if (!canSpinToday) {
                        Text(
                            text = "Already Spun Today (1/Day)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF8895AE)
                        )
                    } else if (canAffordPaid) {
                        Text(
                            text = "Spin (🪙 $spinFee)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = "Need 🪙 $spinFee",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFF8895AE)
                        )
                    }
                }
            }
        }
    }

    // Celebration Result Popup
    if (showOutcomeDialog && currentOutcome != null) {
        val outcome = currentOutcome!!
        AlertDialog(
            onDismissRequest = { showOutcomeDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (outcome.isJackpot) "🎉 Grand Jackpot!" else if (outcome.isCosmetic) "✨ Special Skin!" else "🎉 You Won!",
                    color = if (outcome.isJackpot) NeonAmber else if (outcome.isCosmetic) NeonMagenta else NeonCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(outcome.winningSegment.colorHex).copy(alpha = 0.2f))
                            .border(2.dp, Color(outcome.winningSegment.colorHex), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = outcome.winningSegment.icon,
                            fontSize = 32.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = outcome.rewardSummary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOutcomeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Collect", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        )
    }
}
