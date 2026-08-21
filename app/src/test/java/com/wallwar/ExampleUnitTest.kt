package com.wallwar

import com.wallwar.engine.GameEngine
import com.wallwar.model.GameMode
import com.wallwar.model.GameState
import com.wallwar.model.Position
import com.wallwar.model.Wall
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testInitialMoves_OrthogonalOneStep() {
        val state = GameEngine.createInitialState(GameMode.DUEL)
        // Player 0 starts at (rows-1, cols/2) = (10, 4) in 9x11 duel
        val p0Pos = state.pawns[0]
        assertEquals(Position(10, 4), p0Pos)

        val p0Moves = GameEngine.pawnMoves(state, 0)
        // From (10, 4) on bottom row: Top (9, 4), Left (10, 3), Right (10, 5). Bottom is out of bounds.
        assertEquals(3, p0Moves.size)
        assertTrue(p0Moves.contains(Position(9, 4)))
        assertTrue(p0Moves.contains(Position(10, 3)))
        assertTrue(p0Moves.contains(Position(10, 5)))
    }

    @Test
    fun testOpponentBlocking_NoJumpNoSideStep() {
        // Player 0 at (5, 4), Player 1 at (4, 4) - directly above
        val state = GameState(
            mode = GameMode.DUEL,
            cols = 9,
            rows = 11,
            pawns = listOf(Position(5, 4), Position(4, 4)),
            walls = emptyList()
        )

        val moves = GameEngine.pawnMoves(state, 0)
        // Top is occupied by opponent (4, 4) -> top is banned.
        // No jump to (3, 4) and no diagonal sidesteps to (4, 3) or (4, 5).
        // Legal moves must ONLY be bottom (6, 4), left (5, 3), right (5, 5).
        assertEquals(3, moves.size)
        assertTrue(moves.contains(Position(6, 4)))
        assertTrue(moves.contains(Position(5, 3)))
        assertTrue(moves.contains(Position(5, 5)))
        assertFalse(moves.contains(Position(4, 4))) // cannot move onto opponent
        assertFalse(moves.contains(Position(3, 4))) // cannot jump over opponent
        assertFalse(moves.contains(Position(4, 3))) // cannot diagonal step
        assertFalse(moves.contains(Position(4, 5))) // cannot diagonal step
    }

    @Test
    fun testWallBlocking_BannedSideCannotBeMovedTo() {
        // Player 0 at (5, 4), horizontal wall at r=4, c=4 (blocks between row 4 and row 5 at cols 4, 5)
        val wall = Wall(r = 4, c = 4, isHorizontal = true)
        val state = GameState(
            mode = GameMode.DUEL,
            cols = 9,
            rows = 11,
            pawns = listOf(Position(5, 4), Position(0, 4)),
            walls = listOf(wall)
        )

        val moves = GameEngine.pawnMoves(state, 0)
        // Moving up to (4, 4) is blocked by horizontal wall at (4, 4).
        assertFalse(moves.contains(Position(4, 4)))
        assertTrue(moves.contains(Position(6, 4))) // bottom
        assertTrue(moves.contains(Position(5, 3))) // left
        assertTrue(moves.contains(Position(5, 5))) // right
    }

    @Test
    fun testBallSkinResolution_SameFreeBlueBalls_ChangesOneToRed() {
        val (p0, p1) = com.wallwar.data.BallSkinCatalog.resolveMatchBallSkins("ball_blue", "ball_blue", userPlayerIndex = 0)
        assertEquals("ball_blue", p0)
        assertEquals("ball_red", p1)
    }

    @Test
    fun testBallSkinResolution_SameFreeRedBalls_ChangesOneToBlue() {
        val (p0, p1) = com.wallwar.data.BallSkinCatalog.resolveMatchBallSkins("ball_red", "ball_red", userPlayerIndex = 0)
        assertEquals("ball_red", p0)
        assertEquals("ball_blue", p1)
    }

    @Test
    fun testBallSkinResolution_SameFreeBalls_UserAsPlayer1() {
        val (p0, p1) = com.wallwar.data.BallSkinCatalog.resolveMatchBallSkins("ball_blue", "ball_blue", userPlayerIndex = 1)
        assertEquals("ball_red", p0) // Opponent changed to red
        assertEquals("ball_blue", p1) // User keeps blue
    }

    @Test
    fun testBallSkinResolution_OtherSkinsAllowedSame() {
        // "but for other skins it does not matter."
        val (p0, p1) = com.wallwar.data.BallSkinCatalog.resolveMatchBallSkins("ball_tennis", "ball_tennis", userPlayerIndex = 0)
        assertEquals("ball_tennis", p0)
        assertEquals("ball_tennis", p1)
    }

    @Test
    fun testBallSkinResolution_DifferentFreeBalls_Unchanged() {
        val (p0, p1) = com.wallwar.data.BallSkinCatalog.resolveMatchBallSkins("ball_blue", "ball_red", userPlayerIndex = 0)
        assertEquals("ball_blue", p0)
        assertEquals("ball_red", p1)
    }
}

