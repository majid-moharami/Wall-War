package com.wallwar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * High-definition cybernetic vector artwork renderer for all WallWar profile skins.
 * Uses procedural vector rendering with glowing visors, cyber helmets, robotic optics,
 * celestial wings, and sovereign crowns — strictly without emojis.
 */
@Composable
fun CyberAvatarGraphic(
    skinId: String,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Base gradient background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.95f),
                    Color(0xFF070B14)
                ),
                center = Offset(cx, cy),
                radius = w * 0.7f
            )
        )

        // Subtle tech grid / rings in the backdrop
        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = w * 0.42f,
            style = Stroke(width = 1.5f)
        )

        when (skinId) {
            "skin_cyber_ninja" -> drawCyberNinja(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_neon_knight" -> drawNeonKnight(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_ronin_ghost" -> drawRoninGhost(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_neo_valkyrie" -> drawNeoValkyrie(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_void_phantom" -> drawVoidPhantom(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_mecha_titan" -> drawMechaTitan(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_draco_cyberlord" -> drawDracoCyberlord(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_quantum_archon" -> drawQuantumArchon(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_astral_phoenix" -> drawAstralPhoenix(cx, cy, w, h, primaryColor, secondaryColor)
            "skin_apex_overlord" -> drawApexOverlord(cx, cy, w, h, primaryColor, secondaryColor)
            else -> drawDefaultDuelist(cx, cy, w, h, primaryColor, secondaryColor)
        }
    }
}

// 1. Cyber Shinobi (Stealth Ninja)
private fun DrawScope.drawCyberNinja(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Ninja Hood / Carbon Cowl
    val hoodPath = Path().apply {
        moveTo(cx, cy - h * 0.35f)
        cubicTo(cx + w * 0.35f, cy - h * 0.35f, cx + w * 0.35f, cy + h * 0.30f, cx, cy + h * 0.38f)
        cubicTo(cx - w * 0.35f, cy + h * 0.30f, cx - w * 0.35f, cy - h * 0.35f, cx, cy - h * 0.35f)
        close()
    }
    drawPath(hoodPath, Color(0xFF0F172A))
    drawPath(hoodPath, primary.copy(alpha = 0.5f), style = Stroke(width = 2f))

    // Mask Faceplate
    val maskPath = Path().apply {
        moveTo(cx - w * 0.22f, cy - h * 0.05f)
        lineTo(cx + w * 0.22f, cy - h * 0.05f)
        lineTo(cx + w * 0.16f, cy + h * 0.26f)
        lineTo(cx, cy + h * 0.32f)
        lineTo(cx - w * 0.16f, cy + h * 0.26f)
        close()
    }
    drawPath(maskPath, Color(0xFF1E293B))
    drawPath(maskPath, Color(0xFF334155), style = Stroke(width = 1.5f))

    // Glowing Cyan Stealth Visor
    val visorPath = Path().apply {
        moveTo(cx - w * 0.20f, cy - h * 0.10f)
        lineTo(cx - w * 0.05f, cy - h * 0.05f)
        lineTo(cx + w * 0.05f, cy - h * 0.05f)
        lineTo(cx + w * 0.20f, cy - h * 0.10f)
        lineTo(cx + w * 0.16f, cy - h * 0.02f)
        lineTo(cx - w * 0.16f, cy - h * 0.02f)
        close()
    }
    drawPath(visorPath, primary)
    // Inner visor glow line
    drawLine(
        color = Color.White,
        start = Offset(cx - w * 0.16f, cy - h * 0.06f),
        end = Offset(cx + w * 0.16f, cy - h * 0.06f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )

    // Forehead Shinobi Crest (Mini Shuriken Star)
    val crestPath = Path().apply {
        moveTo(cx, cy - h * 0.28f)
        lineTo(cx + w * 0.04f, cy - h * 0.22f)
        lineTo(cx + w * 0.10f, cy - h * 0.22f)
        lineTo(cx + w * 0.05f, cy - h * 0.18f)
        lineTo(cx + w * 0.07f, cy - h * 0.12f)
        lineTo(cx, cy - h * 0.16f)
        lineTo(cx - w * 0.07f, cy - h * 0.12f)
        lineTo(cx - w * 0.05f, cy - h * 0.18f)
        lineTo(cx - w * 0.10f, cy - h * 0.22f)
        lineTo(cx - w * 0.04f, cy - h * 0.22f)
        close()
    }
    drawPath(crestPath, primary)
}

// 2. Neon Paladin (Knight Defender)
private fun DrawScope.drawNeonKnight(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Great Helm Silhouette
    val helmPath = Path().apply {
        moveTo(cx, cy - h * 0.36f)
        lineTo(cx + w * 0.26f, cy - h * 0.15f)
        lineTo(cx + w * 0.22f, cy + h * 0.20f)
        lineTo(cx, cy + h * 0.35f)
        lineTo(cx - w * 0.22f, cy + h * 0.20f)
        lineTo(cx - w * 0.26f, cy - h * 0.15f)
        close()
    }
    drawPath(helmPath, Color(0xFF0F2027))
    drawPath(helmPath, primary.copy(alpha = 0.7f), style = Stroke(width = 2.5f))

    // Vertical & Horizontal T-Visor Cross (Glowing Emerald)
    val hVisor = Path().apply {
        moveTo(cx - w * 0.18f, cy - h * 0.08f)
        lineTo(cx + w * 0.18f, cy - h * 0.08f)
        lineTo(cx + w * 0.18f, cy - h * 0.02f)
        lineTo(cx - w * 0.18f, cy - h * 0.02f)
        close()
    }
    drawPath(hVisor, primary)

    val vVisor = Path().apply {
        moveTo(cx - w * 0.04f, cy - h * 0.08f)
        lineTo(cx + w * 0.04f, cy - h * 0.08f)
        lineTo(cx + w * 0.03f, cy + h * 0.22f)
        lineTo(cx - w * 0.03f, cy + h * 0.22f)
        close()
    }
    drawPath(vVisor, primary)

    // Paladin Top Crest
    val crestPath = Path().apply {
        moveTo(cx, cy - h * 0.42f)
        lineTo(cx + w * 0.08f, cy - h * 0.34f)
        lineTo(cx - w * 0.08f, cy - h * 0.34f)
        close()
    }
    drawPath(crestPath, primary)
    drawLine(Color.White, Offset(cx, cy - h * 0.08f), Offset(cx, cy + h * 0.18f), strokeWidth = 2f)
}

// 3. Ronin Ghost (Cyber Samurai / Oni)
private fun DrawScope.drawRoninGhost(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Samurai Horns / Kabuto crest
    val leftHorn = Path().apply {
        moveTo(cx - w * 0.10f, cy - h * 0.18f)
        quadraticBezierTo(cx - w * 0.35f, cy - h * 0.42f, cx - w * 0.28f, cy - h * 0.46f)
        quadraticBezierTo(cx - w * 0.18f, cy - h * 0.30f, cx - w * 0.05f, cy - h * 0.22f)
        close()
    }
    val rightHorn = Path().apply {
        moveTo(cx + w * 0.10f, cy - h * 0.18f)
        quadraticBezierTo(cx + w * 0.35f, cy - h * 0.42f, cx + w * 0.28f, cy - h * 0.46f)
        quadraticBezierTo(cx + w * 0.18f, cy - h * 0.30f, cx + w * 0.05f, cy - h * 0.22f)
        close()
    }
    drawPath(leftHorn, primary)
    drawPath(rightHorn, primary)

    // Mask Face
    val maskPath = Path().apply {
        moveTo(cx, cy - h * 0.22f)
        lineTo(cx + w * 0.24f, cy - h * 0.10f)
        lineTo(cx + w * 0.20f, cy + h * 0.18f)
        lineTo(cx, cy + h * 0.34f)
        lineTo(cx - w * 0.20f, cy + h * 0.18f)
        lineTo(cx - w * 0.24f, cy - h * 0.10f)
        close()
    }
    drawPath(maskPath, Color(0xFF2B090A))
    drawPath(maskPath, primary.copy(alpha = 0.8f), style = Stroke(width = 2f))

    // Angled Glowing Crimson Eyes
    val leftEye = Path().apply {
        moveTo(cx - w * 0.16f, cy - h * 0.06f)
        lineTo(cx - w * 0.05f, cy - h * 0.02f)
        lineTo(cx - w * 0.14f, cy + h * 0.02f)
        close()
    }
    val rightEye = Path().apply {
        moveTo(cx + w * 0.16f, cy - h * 0.06f)
        lineTo(cx + w * 0.05f, cy - h * 0.02f)
        lineTo(cx + w * 0.14f, cy + h * 0.02f)
        close()
    }
    drawPath(leftEye, primary)
    drawPath(rightEye, primary)
    drawPath(leftEye, Color.White.copy(alpha = 0.8f), style = Stroke(width = 1f))
    drawPath(rightEye, Color.White.copy(alpha = 0.8f), style = Stroke(width = 1f))

    // Oni Grill Teeth
    for (i in -2..2) {
        drawLine(
            color = primary.copy(alpha = 0.7f),
            start = Offset(cx + i * w * 0.05f, cy + h * 0.14f),
            end = Offset(cx + i * w * 0.05f, cy + h * 0.22f),
            strokeWidth = 2f
        )
    }
}

// 4. Neo Valkyrie (Celestial Wings & Tiara)
private fun DrawScope.drawNeoValkyrie(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Winged Helm Left
    val leftWing = Path().apply {
        moveTo(cx - w * 0.15f, cy - h * 0.05f)
        lineTo(cx - w * 0.42f, cy - h * 0.32f)
        lineTo(cx - w * 0.32f, cy - h * 0.12f)
        lineTo(cx - w * 0.40f, cy - h * 0.02f)
        lineTo(cx - w * 0.22f, cy + h * 0.08f)
        close()
    }
    // Winged Helm Right
    val rightWing = Path().apply {
        moveTo(cx + w * 0.15f, cy - h * 0.05f)
        lineTo(cx + w * 0.42f, cy - h * 0.32f)
        lineTo(cx + w * 0.32f, cy - h * 0.12f)
        lineTo(cx + w * 0.40f, cy - h * 0.02f)
        lineTo(cx + w * 0.22f, cy + h * 0.08f)
        close()
    }
    drawPath(leftWing, primary)
    drawPath(rightWing, primary)

    // Center Valkyrie Head Diadem
    val diademPath = Path().apply {
        moveTo(cx, cy - h * 0.32f)
        lineTo(cx + w * 0.18f, cy - h * 0.10f)
        lineTo(cx + w * 0.14f, cy + h * 0.25f)
        lineTo(cx, cy + h * 0.32f)
        lineTo(cx - w * 0.14f, cy + h * 0.25f)
        lineTo(cx - w * 0.18f, cy - h * 0.10f)
        close()
    }
    drawPath(diademPath, Color(0xFF1E1B18))
    drawPath(diademPath, primary, style = Stroke(width = 2f))

    // Glowing Golden Eyes / Photon Visor
    drawLine(
        color = primary,
        start = Offset(cx - w * 0.12f, cy - h * 0.02f),
        end = Offset(cx + w * 0.12f, cy - h * 0.02f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawCircle(Color.White, radius = w * 0.04f, center = Offset(cx, cy - h * 0.18f))
}

// 5. Void Phantom (Abyssal Core & Fractures)
private fun DrawScope.drawVoidPhantom(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Dark Astral Cloak
    val cloakPath = Path().apply {
        moveTo(cx, cy - h * 0.36f)
        quadraticBezierTo(cx + w * 0.32f, cy - h * 0.20f, cx + w * 0.25f, cy + h * 0.32f)
        lineTo(cx, cy + h * 0.38f)
        lineTo(cx - w * 0.25f, cy + h * 0.32f)
        quadraticBezierTo(cx - w * 0.32f, cy - h * 0.20f, cx, cy - h * 0.36f)
        close()
    }
    drawPath(cloakPath, Color(0xFF0B0414))
    drawPath(cloakPath, primary.copy(alpha = 0.6f), style = Stroke(width = 2f))

    // Void Core (Glowing Purple Eye of the Abyss)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primary, Color.Transparent),
            center = Offset(cx, cy - h * 0.04f),
            radius = w * 0.18f
        ),
        radius = w * 0.18f,
        center = Offset(cx, cy - h * 0.04f)
    )

    // Dimensional Fracture Lines
    val fColor = primary.copy(alpha = 0.9f)
    drawLine(fColor, Offset(cx - w * 0.20f, cy - h * 0.20f), Offset(cx - w * 0.06f, cy - h * 0.06f), strokeWidth = 2f)
    drawLine(fColor, Offset(cx + w * 0.20f, cy - h * 0.20f), Offset(cx + w * 0.06f, cy - h * 0.06f), strokeWidth = 2f)
    drawLine(fColor, Offset(cx, cy + h * 0.08f), Offset(cx, cy + h * 0.28f), strokeWidth = 2f)
}

// 6. Mecha Titan (Heavy Cyborg Robot)
private fun DrawScope.drawMechaTitan(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Angular Plating Chassis
    val chassisPath = Path().apply {
        moveTo(cx - w * 0.20f, cy - h * 0.30f)
        lineTo(cx + w * 0.20f, cy - h * 0.30f)
        lineTo(cx + w * 0.26f, cy - h * 0.05f)
        lineTo(cx + w * 0.18f, cy + h * 0.30f)
        lineTo(cx - w * 0.18f, cy + h * 0.30f)
        lineTo(cx - w * 0.26f, cy - h * 0.05f)
        close()
    }
    drawPath(chassisPath, Color(0xFF1E293B))
    drawPath(chassisPath, primary, style = Stroke(width = 2.5f))

    // Dual Sensor Eyes
    drawRoundRect(
        color = primary,
        topLeft = Offset(cx - w * 0.16f, cy - h * 0.10f),
        size = Size(w * 0.12f, h * 0.06f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = primary,
        topLeft = Offset(cx + w * 0.04f, cy - h * 0.10f),
        size = Size(w * 0.12f, h * 0.06f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )

    // Reinforced Jaw Plates
    for (i in 0..2) {
        val yOffset = cy + h * (0.05f + i * 0.07f)
        drawLine(
            color = primary.copy(alpha = 0.7f),
            start = Offset(cx - w * 0.12f, yOffset),
            end = Offset(cx + w * 0.12f, yOffset),
            strokeWidth = 2.5f
        )
    }
}

// 7. Draco Cyberlord (Plasma Dragon Visage)
private fun DrawScope.drawDracoCyberlord(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Fierce Dragon Horns
    val leftDragonHorn = Path().apply {
        moveTo(cx - w * 0.08f, cy - h * 0.20f)
        cubicTo(cx - w * 0.25f, cy - h * 0.35f, cx - w * 0.38f, cy - h * 0.40f, cx - w * 0.32f, cy - h * 0.46f)
        cubicTo(cx - w * 0.22f, cy - h * 0.38f, cx - w * 0.14f, cy - h * 0.28f, cx - w * 0.02f, cy - h * 0.16f)
        close()
    }
    val rightDragonHorn = Path().apply {
        moveTo(cx + w * 0.08f, cy - h * 0.20f)
        cubicTo(cx + w * 0.25f, cy - h * 0.35f, cx + w * 0.38f, cy - h * 0.40f, cx + w * 0.32f, cy - h * 0.46f)
        cubicTo(cx + w * 0.22f, cy - h * 0.38f, cx + w * 0.14f, cy - h * 0.28f, cx + w * 0.02f, cy - h * 0.16f)
        close()
    }
    drawPath(leftDragonHorn, primary)
    drawPath(rightDragonHorn, primary)

    // Dragon Snout and Faceplate
    val snoutPath = Path().apply {
        moveTo(cx, cy - h * 0.25f)
        lineTo(cx + w * 0.20f, cy - h * 0.08f)
        lineTo(cx + w * 0.12f, cy + h * 0.28f)
        lineTo(cx, cy + h * 0.36f)
        lineTo(cx - w * 0.12f, cy + h * 0.28f)
        lineTo(cx - w * 0.20f, cy - h * 0.08f)
        close()
    }
    drawPath(snoutPath, Color(0xFF260D05))
    drawPath(snoutPath, primary.copy(alpha = 0.85f), style = Stroke(width = 2f))

    // Slit Dragon Optics
    val eyeL = Path().apply {
        moveTo(cx - w * 0.14f, cy - h * 0.06f)
        lineTo(cx - w * 0.04f, cy - h * 0.02f)
        lineTo(cx - w * 0.12f, cy + h * 0.02f)
        close()
    }
    val eyeR = Path().apply {
        moveTo(cx + w * 0.14f, cy - h * 0.06f)
        lineTo(cx + w * 0.04f, cy - h * 0.02f)
        lineTo(cx + w * 0.12f, cy + h * 0.02f)
        close()
    }
    drawPath(eyeL, primary)
    drawPath(eyeR, primary)
    // Plasma Glow in Throat
    drawCircle(primary, radius = w * 0.04f, center = Offset(cx, cy + h * 0.20f))
}

// 8. Quantum Archon (Cosmic Reality Core)
private fun DrawScope.drawQuantumArchon(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Concentric Quantum Orbital Rings
    drawCircle(primary.copy(alpha = 0.4f), radius = w * 0.36f, style = Stroke(width = 1.5f))
    drawCircle(primary.copy(alpha = 0.7f), radius = w * 0.26f, style = Stroke(width = 2f))
    drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.14f, style = Stroke(width = 2f))

    // Levitating Diamond Archon Crystal
    val diamondPath = Path().apply {
        moveTo(cx, cy - h * 0.32f)
        lineTo(cx + w * 0.18f, cy)
        lineTo(cx, cy + h * 0.32f)
        lineTo(cx - w * 0.18f, cy)
        close()
    }
    drawPath(diamondPath, primary.copy(alpha = 0.35f))
    drawPath(diamondPath, primary, style = Stroke(width = 2.5f))

    // Central Radiant Singularity
    drawCircle(Color.White, radius = w * 0.06f, center = Offset(cx, cy))
}

// 9. Astral Phoenix (Blazing Solar Wings)
private fun DrawScope.drawAstralPhoenix(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Solar Flame Wings
    val pWingL = Path().apply {
        moveTo(cx - w * 0.05f, cy + h * 0.10f)
        quadraticBezierTo(cx - w * 0.38f, cy - h * 0.05f, cx - w * 0.36f, cy - h * 0.36f)
        quadraticBezierTo(cx - w * 0.22f, cy - h * 0.18f, cx - w * 0.05f, cy - h * 0.12f)
        close()
    }
    val pWingR = Path().apply {
        moveTo(cx + w * 0.05f, cy + h * 0.10f)
        quadraticBezierTo(cx + w * 0.38f, cy - h * 0.05f, cx + w * 0.36f, cy - h * 0.36f)
        quadraticBezierTo(cx + w * 0.22f, cy - h * 0.18f, cx + w * 0.05f, cy - h * 0.12f)
        close()
    }
    drawPath(pWingL, primary)
    drawPath(pWingR, primary)

    // Phoenix Head & Beak
    val beakPath = Path().apply {
        moveTo(cx, cy - h * 0.28f)
        lineTo(cx + w * 0.12f, cy - h * 0.08f)
        lineTo(cx, cy + h * 0.25f)
        lineTo(cx - w * 0.12f, cy - h * 0.08f)
        close()
    }
    drawPath(beakPath, Color(0xFF3E1F00))
    drawPath(beakPath, primary, style = Stroke(width = 2f))

    // Fiery Crown Feathers
    drawLine(primary, Offset(cx, cy - h * 0.28f), Offset(cx, cy - h * 0.42f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(primary, Offset(cx - w * 0.06f, cy - h * 0.24f), Offset(cx - w * 0.14f, cy - h * 0.38f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawLine(primary, Offset(cx + w * 0.06f, cy - h * 0.24f), Offset(cx + w * 0.14f, cy - h * 0.38f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawCircle(Color.White, radius = w * 0.035f, center = Offset(cx, cy - h * 0.04f))
}

// 10. Apex Overlord (Supreme Sovereign Crown)
private fun DrawScope.drawApexOverlord(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Multi-Pointed Sovereign Crown
    val crownPath = Path().apply {
        moveTo(cx - w * 0.25f, cy - h * 0.10f)
        lineTo(cx - w * 0.28f, cy - h * 0.38f) // Left point
        lineTo(cx - w * 0.14f, cy - h * 0.20f)
        lineTo(cx, cy - h * 0.45f)             // Center Apex Point
        lineTo(cx + w * 0.14f, cy - h * 0.20f)
        lineTo(cx + w * 0.28f, cy - h * 0.38f) // Right point
        lineTo(cx + w * 0.25f, cy - h * 0.10f)
        close()
    }
    drawPath(crownPath, primary)
    drawPath(crownPath, Color(0xFFFFF9C4), style = Stroke(width = 1.5f))

    // Overlord Royal Faceplate
    val royalMask = Path().apply {
        moveTo(cx - w * 0.22f, cy - h * 0.10f)
        lineTo(cx + w * 0.22f, cy - h * 0.10f)
        lineTo(cx + w * 0.16f, cy + h * 0.24f)
        lineTo(cx, cy + h * 0.34f)
        lineTo(cx - w * 0.16f, cy + h * 0.24f)
        close()
    }
    drawPath(royalMask, Color(0xFF190638))
    drawPath(royalMask, primary, style = Stroke(width = 2.5f))

    // Glowing Diamond Core
    val coreGem = Path().apply {
        moveTo(cx, cy - h * 0.04f)
        lineTo(cx + w * 0.08f, cy + h * 0.06f)
        lineTo(cx, cy + h * 0.16f)
        lineTo(cx - w * 0.08f, cy + h * 0.06f)
        close()
    }
    drawPath(coreGem, Color.White)
    drawPath(coreGem, primary, style = Stroke(width = 1.5f))
}

// Default Duelist (Standard Issue Combat Helmet)
private fun DrawScope.drawDefaultDuelist(
    cx: Float, cy: Float, w: Float, h: Float,
    primary: Color, secondary: Color
) {
    // Combat Helmet
    val helmPath = Path().apply {
        moveTo(cx, cy - h * 0.32f)
        cubicTo(cx + w * 0.26f, cy - h * 0.32f, cx + w * 0.24f, cy + h * 0.20f, cx, cy + h * 0.32f)
        cubicTo(cx - w * 0.24f, cy + h * 0.20f, cx - w * 0.26f, cy - h * 0.32f, cx, cy - h * 0.32f)
        close()
    }
    drawPath(helmPath, Color(0xFF131B2E))
    drawPath(helmPath, primary.copy(alpha = 0.7f), style = Stroke(width = 2f))

    // Glowing Cyan Visor Line
    val visorPath = Path().apply {
        moveTo(cx - w * 0.16f, cy - h * 0.06f)
        lineTo(cx + w * 0.16f, cy - h * 0.06f)
        lineTo(cx + w * 0.12f, cy + h * 0.02f)
        lineTo(cx - w * 0.12f, cy + h * 0.02f)
        close()
    }
    drawPath(visorPath, primary)
    drawLine(
        color = Color.White,
        start = Offset(cx - w * 0.12f, cy - h * 0.03f),
        end = Offset(cx + w * 0.12f, cy - h * 0.03f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
}
