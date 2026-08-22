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
    fun testOpponentStraightJump_TopUnblocked() {
        // Player 0 at (5, 4), Player 1 at (4, 4) - directly above
        val state = GameState(
            mode = GameMode.DUEL,
            cols = 9,
            rows = 11,
            pawns = listOf(Position(5, 4), Position(4, 4)),
            walls = emptyList()
        )

        val moves = GameEngine.pawnMoves(state, 0)
        // Top opponent at (4, 4) allows straight jump to (3, 4).
        // Legal moves: jump top (3, 4), bottom (6, 4), left (5, 3), right (5, 5).
        assertEquals(4, moves.size)
        assertTrue(moves.contains(Position(3, 4))) // straight jump over opponent
        assertTrue(moves.contains(Position(6, 4))) // bottom
        assertTrue(moves.contains(Position(5, 3))) // left
        assertTrue(moves.contains(Position(5, 5))) // right
        assertFalse(moves.contains(Position(4, 4))) // cannot move onto opponent square
    }

    @Test
    fun testOpponentStraightJump_LeftAndRightAndBottom() {
        // Player 0 at (5, 4), Player 1 at (5, 3) - to the left
        val stateLeft = GameState(
            mode = GameMode.DUEL,
            cols = 9,
            rows = 11,
            pawns = listOf(Position(5, 4), Position(5, 3)),
            walls = emptyList()
        )
        val leftMoves = GameEngine.pawnMoves(stateLeft, 0)
        assertTrue(leftMoves.contains(Position(5, 2))) // straight jump left to (5, 2)
        assertTrue(leftMoves.contains(Position(4, 4))) // top
        assertTrue(leftMoves.contains(Position(6, 4))) // bottom
        assertTrue(leftMoves.contains(Position(5, 5))) // right

        // Player 0 at (5, 4), Player 1 at (5, 5) - to the right
        val stateRight = GameState(
            mode = GameMode.DUEL,
            cols = 9,
            rows = 11,
            pawns = listOf(Position(5, 4), Position(5, 5)),
            walls = emptyList()
        )
        val rightMoves = GameEngine.pawnMoves(stateRight, 0)
        assertTrue(rightMoves.contains(Position(5, 6))) // straight jump right to (5, 6)

        // Player 0 at (5, 4), Player 1 at (6, 4) - to the bottom
        val stateBottom = GameState(
            mode = GameMode.DUEL,
            cols = 9,
            rows = 11,
            pawns = listOf(Position(5, 4), Position(6, 4)),
            walls = emptyList()
        )
        val bottomMoves = GameEngine.pawnMoves(stateBottom, 0)
        assertTrue(bottomMoves.contains(Position(7, 4))) // straight jump bottom to (7, 4)
    }

    @Test
    fun testOpponentJump_BlockedByWallBehindOpponent() {
        // Player 0 at (5, 4), Player 1 at (4, 4). Horizontal wall at (3, 4) blocks jump from (4, 4) to (3, 4).
        val wall = Wall(r = 3, c = 4, isHorizontal = true)
        val state = GameState(
            mode = GameMode.DUEL,
            cols = 9,
            rows = 11,
            pawns = listOf(Position(5, 4), Position(4, 4)),
            walls = listOf(wall)
        )

        val moves = GameEngine.pawnMoves(state, 0)
        // Jump to (3, 4) is blocked by wall.
        assertFalse(moves.contains(Position(3, 4)))
        assertFalse(moves.contains(Position(4, 4)))
        assertTrue(moves.contains(Position(6, 4))) // bottom
        assertTrue(moves.contains(Position(5, 3))) // left
        assertTrue(moves.contains(Position(5, 5))) // right
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

    @Test
    fun testArenaWinnerRewards_ExactDoubleEntryFee() {
        for (arena in com.wallwar.data.ArenaConfig.onlineArenas) {
            assertEquals(arena.entryFee * 2, arena.winningPrize)
        }
    }
}

