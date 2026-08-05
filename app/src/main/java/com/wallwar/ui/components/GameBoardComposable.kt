package com.wallwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.wallwar.audio.SoundManager
import com.wallwar.engine.GameEngine
import com.wallwar.model.BoardTheme
import com.wallwar.model.GameState
import com.wallwar.model.Position
import com.wallwar.model.Wall
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonMagenta
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GameBoardComposable(
    gameState: GameState,
    boardTheme: BoardTheme,
    isWallMode: Boolean,
    isWallHorizontal: Boolean,
    validHighlights: List<Position>,
    soundManager: SoundManager,
    onCellClick: (r: Int, c: Int) -> Unit,
    onPlaceWall: (r: Int, c: Int, isHorizontal: Boolean) -> Unit,
    onCancelWallMode: () -> Unit = {},
    shouldFlip: Boolean = false,
    externalDragWall: Wall? = null,
    externalIsValidDrag: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cols = gameState.cols
    val rows = gameState.rows

    val themeGridBg = Color(boardTheme.gridBg)
    val themeCellBg = Color(boardTheme.cellBg)
    val themePrimary = Color(boardTheme.primaryColor)
    val themePawn1 = Color(0xFF7C5CFF)
    val themePawn2 = Color(0xFFFFB800)

    // Internal drag-and-drop state for direct board touches
    var activeHoverWall by remember { mutableStateOf<Wall?>(null) }
    var isValidHover by remember { mutableStateOf(false) }

    val effectiveHoverWall = if (isWallMode || externalDragWall != null) (externalDragWall ?: activeHoverWall) else null
    val effectiveIsValidHover = if (externalDragWall != null) externalIsValidDrag else isValidHover

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat())
            .padding(8.dp)
            .testTag("game_board_canvas"),
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Cell dimensions accounting for wall spacing
        val gapRatio = 0.18f
        val cellW = width / (cols + (cols - 1) * gapRatio)
        val cellH = height / (rows + (rows - 1) * gapRatio)
        val gapW = cellW * gapRatio
        val gapH = cellH * gapRatio

        val stepX = cellW + gapW
        val stepY = cellH + gapH

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gameState, isWallMode, isWallHorizontal) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        var isDrag = false

                        val turn = gameState.turn
                        val fingerOffsetY = stepY * 2.5f
                        
                        fun screenToLogic(x: Float, y: Float): Pair<Int, Int> {
                            val r = (((y - fingerOffsetY) / stepY) - 0.5f).roundToInt()
                            val c = ((x / stepX) - 0.5f).roundToInt()
                            return if (shouldFlip) {
                                (rows - 2 - r) to (cols - 2 - c)
                            } else {
                                r to c
                            }
                        }

                        val (initR, initC) = screenToLogic(startPos.x, startPos.y)
                        var lastR = initR.coerceIn(0, rows - 2)
                        var lastC = initC.coerceIn(0, cols - 2)

                        // If in wall mode or AI is not thinking, initialize hover preview
                        val isTurnDisabled = gameState.isAiMatch && turn == 1 || gameState.winner != null
                        if (isWallMode && !isTurnDisabled) {
                            val (rawR, rawC) = screenToLogic(startPos.x, startPos.y)
                            if (rawR in 0..(rows - 2) && rawC in 0..(cols - 2)) {
                                lastR = rawR
                                lastC = rawC
                                val initialWall = Wall(rawR, rawC, isWallHorizontal, turn)
                                activeHoverWall = initialWall
                                isValidHover = GameEngine.canPlaceWall(gameState, turn, initialWall)
                                soundManager.vibrateShort()
                            }
                        }

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            if (change.pressed) {
                                val dist = hypot(change.position.x - startPos.x, change.position.y - startPos.y)
                                if (dist > 8f) {
                                    isDrag = true
                                }

                                if (isWallMode && !isTurnDisabled) {
                                    val (rawR, rawC) = screenToLogic(change.position.x, change.position.y)

                                    val marginX = stepX * 0.8f
                                    val marginY = stepY * 0.8f

                                    val isOutside = change.position.x < -marginX || change.position.x > width + marginX ||
                                            change.position.y < -marginY || change.position.y > height + fingerOffsetY + marginY ||
                                            rawC !in 0..(cols - 2) || rawR !in 0..(rows - 2)

                                    if (isOutside) {
                                        if (activeHoverWall != null) {
                                            activeHoverWall = null
                                            isValidHover = false
                                        }
                                    } else {
                                        if (rawR != lastR || rawC != lastC || activeHoverWall == null) {
                                            lastR = rawR
                                            lastC = rawC
                                            val candidate = Wall(rawR, rawC, isWallHorizontal, turn)
                                            activeHoverWall = candidate
                                            isValidHover = GameEngine.canPlaceWall(gameState, turn, candidate)
                                            soundManager.vibrateShort()
                                        }
                                    }
                                    change.consume()
                                }
                            } else {
                                // Pointer up / release gesture
                                val currentHover = activeHoverWall
                                if (isWallMode && !isTurnDisabled) {
                                    if (currentHover != null) {
                                        if (isValidHover) {
                                            onPlaceWall(currentHover.r, currentHover.c, currentHover.isHorizontal)
                                        } else {
                                            soundManager.playErrorSound()
                                            onCancelWallMode()
                                        }
                                    } else {
                                        onCancelWallMode()
                                    }
                                    activeHoverWall = null
                                    isValidHover = false
                                } else if (!isWallMode && !isTurnDisabled) {
                                    val logicC = if (shouldFlip) {
                                        (cols - 1) - (startPos.x / stepX).toInt()
                                    } else {
                                        (startPos.x / stepX).toInt()
                                    }
                                    val logicR = if (shouldFlip) {
                                        (rows - 1) - (startPos.y / stepY).toInt()
                                    } else {
                                        (startPos.y / stepY).toInt()
                                    }
                                    onCellClick(logicR.coerceIn(0, rows - 1), logicC.coerceIn(0, cols - 1))
                                }
                                break
                            }
                        } while (true)
                    }
                }
        ) {
            // Draw Outer Board Background & Gradient Overlays
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF151D33), Color(0xFF101628))
                ),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Outer Border
            drawRoundRect(
                color = Color(0xFF283A60),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3f)
            )

            // 1. Draw Grid Cells & Border Grid Lines
            val gridBorderColor = Color(0xFF1D2B4A)
            val cellBgColor = Color(0xFF121B30)

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val drawR = if (shouldFlip) (rows - 1 - r) else r
                    val drawC = if (shouldFlip) (cols - 1 - c) else c

                    val x = drawC * stepX
                    val y = drawR * stepY

                    drawRoundRect(
                        color = cellBgColor,
                        topLeft = Offset(x, y),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    drawRoundRect(
                        color = gridBorderColor,
                        topLeft = Offset(x, y),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1f)
                    )
                }
            }

            // 2. Draw Valid Move Candidate Cells (Border with Quick Pass & Play style from HomeScreen)
            for (pos in validHighlights) {
                val drawR = if (shouldFlip) (rows - 1 - pos.r) else pos.r
                val drawC = if (shouldFlip) (cols - 1 - pos.c) else pos.c

                val x = drawC * stepX
                val y = drawR * stepY

                val highlightColor = if (gameState.turn == 0) NeonCyan else NeonMagenta
                val cellGradientBrush = Brush.horizontalGradient(
                    colors = listOf(highlightColor.copy(alpha = 0.8f), highlightColor),
                    startX = x,
                    endX = x + cellW
                )

                // Subtle inner glow overlay
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(highlightColor.copy(alpha = 0.15f), highlightColor.copy(alpha = 0.05f)),
                        start = Offset(x, y),
                        end = Offset(x + cellW, y + cellH)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // High-visibility border
                drawRoundRect(
                    brush = cellGradientBrush,
                    topLeft = Offset(x, y),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 3. Draw Snap Grid Target Dots when in Wall Mode / Dragging
            if ((isWallMode || effectiveHoverWall != null) && gameState.winner == null) {
                val snapDotColor = if (gameState.turn == 0) Color(0xFF3B82F6) else Color(0xFFEF4444)
                for (r in 0 until rows - 1) {
                    for (c in 0 until cols - 1) {
                        val drawR = if (shouldFlip) (rows - 2 - r) else r
                        val drawC = if (shouldFlip) (cols - 2 - c) else c
                        
                        val cx = (drawC + 1) * stepX - gapW / 2f
                        val cy = (drawR + 1) * stepY - gapH / 2f

                        drawCircle(
                            color = snapDotColor.copy(alpha = 0.35f),
                            radius = gapW * 0.45f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = gapW * 0.22f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            // 4. Draw Placed Walls (Glowing Neon Pill Capsules in Player Ball Colors)
            val wallShadow = Color.Black.copy(alpha = 0.5f)

            for (wall in gameState.walls) {
                val drawR = if (shouldFlip) (rows - 2 - wall.r) else wall.r
                val drawC = if (shouldFlip) (cols - 2 - wall.c) else wall.c

                val wallGradients = if (wall.playerOwner == 0) {
                    listOf(Color(0xFF2563EB), Color(0xFF3B82F6), Color(0xFF60A5FA), Color(0xFF2563EB))
                } else {
                    listOf(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFFCA5A5), Color(0xFFDC2626))
                }

                if (wall.isHorizontal) {
                    val x = drawC * stepX
                    val y = drawR * stepY + cellH + (gapH * 0.05f)
                    val wallWidth = cellW * 2 + gapW
                    val wallHeight = gapH * 0.9f

                    // Wall Drop Shadow
                    drawRoundRect(
                        color = wallShadow,
                        topLeft = Offset(x, y + 3f),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // Wall Main Pill
                    drawRoundRect(
                        brush = Brush.horizontalGradient(colors = wallGradients),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // Top Highlight Line
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(x + 4f, y + 2f),
                        size = Size(wallWidth - 8f, wallHeight * 0.3f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                } else {
                    val x = drawC * stepX + cellW + (gapW * 0.05f)
                    val y = drawR * stepY
                    val wallWidth = gapW * 0.9f
                    val wallHeight = cellH * 2 + gapH

                    // Wall Drop Shadow
                    drawRoundRect(
                        color = wallShadow,
                        topLeft = Offset(x + 3f, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // Wall Main Pill
                    drawRoundRect(
                        brush = Brush.verticalGradient(colors = wallGradients),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // Left Highlight Line
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(x + 2f, y + 4f),
                        size = Size(wallWidth * 0.3f, wallHeight - 8f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                }
            }

            // 5. Live Drag Hover Preview Wall
            val hover = effectiveHoverWall
            if (hover != null) {
                val drawR = if (shouldFlip) (rows - 2 - hover.r) else hover.r
                val drawC = if (shouldFlip) (cols - 2 - hover.c) else hover.c

                val isP1 = hover.playerOwner == 0
                val previewFill = if (effectiveIsValidHover) {
                    if (isP1) Color(0xFF3B82F6) else Color(0xFFEF4444)
                } else {
                    Color(0xFFDC2626)
                }
                val previewBorder = if (effectiveIsValidHover) {
                    if (isP1) Color(0xFF60A5FA) else Color(0xFFFCA5A5)
                } else {
                    Color(0xFFF87171)
                }

                if (hover.isHorizontal) {
                    val x = drawC * stepX
                    val y = drawR * stepY + cellH + (gapH * 0.05f)
                    val wallWidth = cellW * 2 + gapW
                    val wallHeight = gapH * 0.9f

                    // Glow aura
                    drawRoundRect(
                        color = previewFill.copy(alpha = 0.35f),
                        topLeft = Offset(x - 4f, y - 4f),
                        size = Size(wallWidth + 8f, wallHeight + 8f),
                        cornerRadius = CornerRadius(14f, 14f)
                    )

                    drawRoundRect(
                        color = previewFill.copy(alpha = 0.85f),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    drawRoundRect(
                        color = previewBorder,
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 3.5f)
                    )
                } else {
                    val x = drawC * stepX + cellW + (gapW * 0.05f)
                    val y = drawR * stepY
                    val wallWidth = gapW * 0.9f
                    val wallHeight = cellH * 2 + gapH

                    // Glow aura
                    drawRoundRect(
                        color = previewFill.copy(alpha = 0.35f),
                        topLeft = Offset(x - 4f, y - 4f),
                        size = Size(wallWidth + 8f, wallHeight + 8f),
                        cornerRadius = CornerRadius(14f, 14f)
                    )

                    drawRoundRect(
                        color = previewFill.copy(alpha = 0.85f),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    drawRoundRect(
                        color = previewBorder,
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 3.5f)
                    )
                }
            }

            // 6. Draw Player Pawns (3D Spheres with White Ring Halo for P1)
            for (p in gameState.pawns.indices) {
                val pawnPos = gameState.pawns[p]
                val drawR = if (shouldFlip) (rows - 1 - pawnPos.r) else pawnPos.r
                val drawC = if (shouldFlip) (cols - 1 - pawnPos.c) else pawnPos.c

                val centerX = drawC * stepX + cellW / 2f
                val centerY = drawR * stepY + cellH / 2f
                val pawnRadius = minOf(cellW, cellH) * 0.38f

                val isTurn = gameState.turn == p && gameState.winner == null

                // Drop Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = pawnRadius * 1.05f,
                    center = Offset(centerX + 2f, centerY + 5f)
                )

                if (p == 0) {
                    // Player 1 (Blue Pawn): Active White Ring Halo
                    drawCircle(
                        color = Color.White,
                        radius = pawnRadius * 1.25f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3.5f)
                    )

                    // 3D Blue Sphere
                    val sphereGradients = listOf(
                        Color(0xFFE0F2FE),
                        Color(0xFF93C5FD),
                        Color(0xFF3B82F6),
                        Color(0xFF1D4ED8),
                        Color(0xFF1E3A8A)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = sphereGradients,
                            center = Offset(centerX - pawnRadius * 0.35f, centerY - pawnRadius * 0.35f),
                            radius = pawnRadius * 1.5f
                        ),
                        radius = pawnRadius,
                        center = Offset(centerX, centerY)
                    )

                    // Glossy Specular Reflection
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = pawnRadius * 0.32f,
                        center = Offset(centerX - pawnRadius * 0.32f, centerY - pawnRadius * 0.32f)
                    )
                } else {
                    // Player 2 / AI (Red Coral Pawn)
                    if (isTurn) {
                        drawCircle(
                            color = Color(0xFFEF4444).copy(alpha = 0.35f),
                            radius = pawnRadius * 1.35f,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = pawnRadius * 1.2f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 3f)
                        )
                    }

                    // 3D Red Sphere
                    val sphereGradients = listOf(
                        Color(0xFFFFE4E6),
                        Color(0xFFFCA5A5),
                        Color(0xFFE84560),
                        Color(0xFFDC2626),
                        Color(0xFF881337)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = sphereGradients,
                            center = Offset(centerX - pawnRadius * 0.35f, centerY - pawnRadius * 0.35f),
                            radius = pawnRadius * 1.5f
                        ),
                        radius = pawnRadius,
                        center = Offset(centerX, centerY)
                    )

                    // Glossy Specular Reflection
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = pawnRadius * 0.32f,
                        center = Offset(centerX - pawnRadius * 0.32f, centerY - pawnRadius * 0.32f)
                    )
                }
            }
        }
    }
}


