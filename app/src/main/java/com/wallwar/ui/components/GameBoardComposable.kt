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
import com.wallwar.model.RadarType
import com.wallwar.model.TilePattern
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

    val isHighTier = boardTheme == BoardTheme.HIGH_ROLLER ||
            boardTheme == BoardTheme.MASTER ||
            boardTheme == BoardTheme.GRAND_CHAMPION

    // Internal drag-and-drop state for direct board touches
    var activeHoverWall by remember { mutableStateOf<Wall?>(null) }
    var isValidHover by remember { mutableStateOf(false) }

    val effectiveHoverWall = if (isWallMode || externalDragWall != null) (externalDragWall ?: activeHoverWall) else null
    val effectiveIsValidHover = if (externalDragWall != null) externalIsValidDrag else isValidHover

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat())
            .padding(16.dp)
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
                        val fingerOffsetY = stepY * 1.0f
                        
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
            // Dynamic theme colors derived from selected BoardTheme
            val topGlow = Color(boardTheme.topGlowColor)
            val bottomGlow = Color(boardTheme.bottomGlowColor)

            val topPlayerColor = if (shouldFlip) bottomGlow else topGlow
            val bottomPlayerColor = if (shouldFlip) topGlow else bottomGlow

            // Combined vertical gradient brush for board border & glow
            val verticalGlowBrush = Brush.verticalGradient(
                colors = listOf(topPlayerColor, bottomPlayerColor)
            )

            // 1. OUTSIDE AMBIENT SHINING GLOW
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        topPlayerColor.copy(alpha = 0.55f),
                        topPlayerColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.25f, -8f),
                    radius = width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        topPlayerColor.copy(alpha = 0.55f),
                        topPlayerColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.75f, -8f),
                    radius = width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bottomPlayerColor.copy(alpha = 0.55f),
                        bottomPlayerColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.25f, height + 8f),
                    radius = width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bottomPlayerColor.copy(alpha = 0.55f),
                        bottomPlayerColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.75f, height + 8f),
                    radius = width * 0.65f
                )
            )

            // Outward Expanding Gradient Blur Rings
            for (step in 14 downTo 1) {
                val spread = step * 1.5f
                val alphaVal = (0.32f * (1f - (step / 15f))).coerceIn(0.01f, 0.35f)
                val stepBrush = Brush.verticalGradient(
                    colors = listOf(
                        topPlayerColor.copy(alpha = alphaVal * 1.25f),
                        bottomPlayerColor.copy(alpha = alphaVal * 1.25f)
                    )
                )
                drawRoundRect(
                    brush = stepBrush,
                    topLeft = Offset(-spread, -spread),
                    size = Size(width + (spread * 2f), height + (spread * 2f)),
                    cornerRadius = CornerRadius(24f + spread, 24f + spread),
                    style = Stroke(width = 2.5f)
                )
            }

            // 2. Solid Outer Board Background
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(boardTheme.outerBgTop), Color(boardTheme.outerBgBottom))
                ),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // 3. Sharp High-Contrast Neon Edge Border Line
            drawRoundRect(
                brush = verticalGlowBrush,
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = if (isHighTier) 3.5f else 2.5f)
            )

            // 4. Center Radar Circles & Crosshair Elements
            val boardCenterX = width / 2f
            val boardCenterY = height / 2f
            val radarRadius = minOf(width, height) * 0.22f
            val centerRingColor = Color(boardTheme.centerRingColor)

            val drawRadarAction = {
                when (boardTheme.radarType) {
                    RadarType.SIMPLE_CROSSHAIR -> {
                        drawLine(
                            color = themePrimary.copy(alpha = 0.25f),
                            start = Offset(boardCenterX - radarRadius, boardCenterY),
                            end = Offset(boardCenterX + radarRadius, boardCenterY),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = themePrimary.copy(alpha = 0.25f),
                            start = Offset(boardCenterX, boardCenterY - radarRadius),
                            end = Offset(boardCenterX, boardCenterY + radarRadius),
                            strokeWidth = 1.5f
                        )
                    }
                    RadarType.METALLIC_RADAR -> {
                        drawCircle(
                            color = centerRingColor,
                            radius = radarRadius,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = centerRingColor.copy(alpha = 0.25f),
                            radius = radarRadius * 0.5f,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 1f)
                        )
                        drawLine(
                            color = themePrimary.copy(alpha = 0.3f),
                            start = Offset(boardCenterX - radarRadius * 1.2f, boardCenterY),
                            end = Offset(boardCenterX + radarRadius * 1.2f, boardCenterY),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = themePrimary.copy(alpha = 0.3f),
                            start = Offset(boardCenterX, boardCenterY - radarRadius * 1.2f),
                            end = Offset(boardCenterX, boardCenterY + radarRadius * 1.2f),
                            strokeWidth = 1.5f
                        )
                    }
                    RadarType.CYBER_CROSSHAIR -> {
                        drawCircle(
                            color = centerRingColor,
                            radius = radarRadius,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = themePrimary.copy(alpha = 0.15f),
                            radius = radarRadius * 0.35f,
                            center = Offset(boardCenterX, boardCenterY)
                        )
                        drawLine(
                            color = themePrimary.copy(alpha = 0.4f),
                            start = Offset(boardCenterX - radarRadius * 1.3f, boardCenterY),
                            end = Offset(boardCenterX + radarRadius * 1.3f, boardCenterY),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = themePrimary.copy(alpha = 0.4f),
                            start = Offset(boardCenterX, boardCenterY - radarRadius * 1.3f),
                            end = Offset(boardCenterX, boardCenterY + radarRadius * 1.3f),
                            strokeWidth = 1.5f
                        )
                    }
                    RadarType.MATRIX_GRID -> {
                        drawCircle(
                            color = centerRingColor,
                            radius = radarRadius,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = centerRingColor.copy(alpha = 0.5f),
                            radius = radarRadius * 0.65f,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 1f)
                        )
                        drawCircle(
                            color = centerRingColor.copy(alpha = 0.3f),
                            radius = radarRadius * 0.3f,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 1f)
                        )
                    }
                    RadarType.VOLCANIC_CORE -> {
                        drawCircle(
                            color = Color(0xFFFF5500).copy(alpha = 0.35f),
                            radius = radarRadius * 1.1f,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            radius = radarRadius * 0.6f,
                            center = Offset(boardCenterX, boardCenterY)
                        )
                    }
                    RadarType.CRYSTAL_ORB -> {
                        drawCircle(
                            color = Color(0xFFC77DFF).copy(alpha = 0.4f),
                            radius = radarRadius,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = Color(0xFF3A0CA3).copy(alpha = 0.25f),
                            radius = radarRadius * 0.5f,
                            center = Offset(boardCenterX, boardCenterY)
                        )
                    }
                    RadarType.ROYAL_COSMIC_RING -> {
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = 0.5f),
                            radius = radarRadius * 1.25f,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f),
                            radius = radarRadius * 0.85f,
                            center = Offset(boardCenterX, boardCenterY),
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            radius = radarRadius * 0.4f,
                            center = Offset(boardCenterX, boardCenterY)
                        )
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = 0.5f),
                            start = Offset(boardCenterX - radarRadius * 1.5f, boardCenterY),
                            end = Offset(boardCenterX + radarRadius * 1.5f, boardCenterY),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = 0.5f),
                            start = Offset(boardCenterX, boardCenterY - radarRadius * 1.5f),
                            end = Offset(boardCenterX, boardCenterY + radarRadius * 1.5f),
                            strokeWidth = 2f
                        )
                    }
                    else -> {}
                }
            }

            drawRadarAction()

            // 5. Draw Grid Cells & Border Grid Lines with Tile Patterns
            val gridBorderColor = Color(boardTheme.gridBorderColor)
            val cellBgColor = Color(boardTheme.cellBg)

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val drawR = if (shouldFlip) (rows - 1 - r) else r
                    val drawC = if (shouldFlip) (cols - 1 - c) else c

                    val x = drawC * stepX
                    val y = drawR * stepY

                    // Surface
                    drawRoundRect(
                        color = cellBgColor,
                        topLeft = Offset(x, y),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Tile Pattern Overlays
                    when (boardTheme.tilePattern) {
                        TilePattern.MATTE_DARK -> {
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.25f),
                                topLeft = Offset(x + 1f, y + 1f),
                                size = Size(cellW - 2f, cellH - 2f),
                                cornerRadius = CornerRadius(7f, 7f),
                                style = Stroke(width = 1f)
                            )
                        }
                        TilePattern.METALLIC_GRID -> {
                            drawLine(
                                color = Color.White.copy(alpha = 0.18f),
                                start = Offset(x + 3f, y + cellH - 3f),
                                end = Offset(x + 3f, y + 3f),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.18f),
                                start = Offset(x + 3f, y + 3f),
                                end = Offset(x + cellW - 3f, y + 3f),
                                strokeWidth = 1.5f
                            )
                        }
                        TilePattern.CARBON_FIBER -> {
                            val stripeStep = cellW * 0.25f
                            for (s in 1..3) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f),
                                    start = Offset(x + s * stripeStep, y + 2f),
                                    end = Offset(x + 2f, y + s * stripeStep),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        TilePattern.MATRIX_CIRCUIT -> {
                            if ((r + c) % 2 == 0) {
                                drawCircle(
                                    color = themePrimary.copy(alpha = 0.35f),
                                    radius = 2.5f,
                                    center = Offset(x + cellW * 0.2f, y + cellH * 0.2f)
                                )
                                drawLine(
                                    color = themePrimary.copy(alpha = 0.25f),
                                    start = Offset(x + cellW * 0.2f, y + cellH * 0.2f),
                                    end = Offset(x + cellW * 0.5f, y + cellH * 0.2f),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        TilePattern.VOLCANIC_ROCK -> {
                            if ((r * 3 + c * 7) % 5 == 0) {
                                drawLine(
                                    color = Color(0xFFFF5500).copy(alpha = 0.45f),
                                    start = Offset(x + cellW * 0.2f, y + cellH * 0.8f),
                                    end = Offset(x + cellW * 0.8f, y + cellH * 0.2f),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                        TilePattern.DARK_CRYSTAL -> {
                            drawLine(
                                color = Color(0xFFC77DFF).copy(alpha = 0.3f),
                                start = Offset(x + cellW * 0.1f, y + cellH * 0.1f),
                                end = Offset(x + cellW * 0.9f, y + cellH * 0.9f),
                                strokeWidth = 1f
                            )
                        }
                        TilePattern.OBSIDIAN_GOLD -> {
                            drawRoundRect(
                                color = Color(0xFFFFD700).copy(alpha = 0.25f),
                                topLeft = Offset(x + 2f, y + 2f),
                                size = Size(cellW - 4f, cellH - 4f),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 1f)
                            )
                        }
                    }

                    // Border Grid Line
                    drawRoundRect(
                        color = gridBorderColor,
                        topLeft = Offset(x, y),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1f)
                    )
                }
            }

            // 6. Draw Valid Move Candidate Cells (Player Turn Interactive Tiles)
            for (pos in validHighlights) {
                val drawR = if (shouldFlip) (rows - 1 - pos.r) else pos.r
                val drawC = if (shouldFlip) (cols - 1 - pos.c) else pos.c

                val x = drawC * stepX
                val y = drawR * stepY

                val highlightColor = if (gameState.turn == 0) NeonCyan else NeonMagenta

                // Outer ambient glow aura around valid target tile
                drawRoundRect(
                    color = highlightColor.copy(alpha = 0.25f),
                    topLeft = Offset(x - 2f, y - 2f),
                    size = Size(cellW + 4f, cellH + 4f),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Soft semi-transparent overlay fill
                drawRoundRect(
                    color = highlightColor.copy(alpha = 0.2f),
                    topLeft = Offset(x, y),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Simple, clean, static highlighted border
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(x, y),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // 7. Draw Snap Grid Target Dots when in Wall Mode / Dragging
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

            // 8. Draw Placed Walls
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

                    drawRoundRect(
                        color = wallShadow,
                        topLeft = Offset(x, y + 3f),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    drawRoundRect(
                        brush = Brush.horizontalGradient(colors = wallGradients),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

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

                    drawRoundRect(
                        color = wallShadow,
                        topLeft = Offset(x + 3f, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    drawRoundRect(
                        brush = Brush.verticalGradient(colors = wallGradients),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(x + 2f, y + 4f),
                        size = Size(wallWidth * 0.3f, wallHeight - 8f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                }
            }

            // 9. Live Drag Hover Preview Wall
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

            // 10. Draw Player Pawns
            for (p in gameState.pawns.indices) {
                val pawnPos = gameState.pawns[p]
                val drawR = if (shouldFlip) (rows - 1 - pawnPos.r) else pawnPos.r
                val drawC = if (shouldFlip) (cols - 1 - pawnPos.c) else pawnPos.c

                val centerX = drawC * stepX + cellW / 2f
                val centerY = drawR * stepY + cellH / 2f
                val pawnRadius = minOf(cellW, cellH) * 0.38f

                val isTurn = gameState.turn == p && gameState.winner == null

                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = pawnRadius * 1.05f,
                    center = Offset(centerX + 2f, centerY + 5f)
                )

                if (p == 0) {
                    drawCircle(
                        color = Color.White,
                        radius = pawnRadius * 1.25f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3.5f)
                    )

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

                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = pawnRadius * 0.32f,
                        center = Offset(centerX - pawnRadius * 0.32f, centerY - pawnRadius * 0.32f)
                    )
                } else {
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



