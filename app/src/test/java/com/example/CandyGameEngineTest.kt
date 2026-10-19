package com.example

import com.example.data.model.CandyColor
import com.example.data.model.CandyItem
import com.example.data.model.LevelConfig
import com.example.data.model.Position
import com.example.data.model.SpecialType
import com.example.game.CandyBoardEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CandyGameEngineTest {

    @Test
    fun testAll100LevelsExistAndProgress() {
        val levels = LevelConfig.getAllLevels()
        assertEquals(100, levels.size)
        assertEquals(1, levels.first().levelNumber)
        assertEquals(100, levels.last().levelNumber)

        // Verify difficulty progression
        assertTrue(levels[99].targetScore > levels[0].targetScore)
        assertTrue(levels[99].moves <= levels[0].moves)
    }

    @Test
    fun testBoardInitialization() {
        val engine = CandyBoardEngine(8, 8, 5)
        val board = engine.createInitialBoard()
        assertEquals(8, board.size)
        assertEquals(8, board[0].size)

        for (r in 0 until 8) {
            for (c in 0 until 8) {
                assertNotNull(board[r][c])
            }
        }
    }

    @Test
    fun testMatchDetection() {
        val engine = CandyBoardEngine(8, 8, 5)
        val board = Array(8) { Array<CandyItem?>(8) { null } }

        // Populate empty board with non-matching background
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val color = if ((r + c) % 2 == 0) CandyColor.YELLOW else CandyColor.PURPLE
                board[r][c] = CandyItem(id = (r * 8 + c).toLong(), color = color)
            }
        }

        // Place 3 RED in a row
        board[3][2] = CandyItem(100, CandyColor.RED)
        board[3][3] = CandyItem(101, CandyColor.RED)
        board[3][4] = CandyItem(102, CandyColor.RED)

        val result = engine.findMatches(board)
        assertTrue(result.matchedPositions.contains(Position(3, 2)))
        assertTrue(result.matchedPositions.contains(Position(3, 3)))
        assertTrue(result.matchedPositions.contains(Position(3, 4)))
        assertTrue(result.points >= 180)
    }

    @Test
    fun testStripedCandyCreation() {
        val engine = CandyBoardEngine(8, 8, 5)
        val board = Array(8) { Array<CandyItem?>(8) { null } }
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val color = if ((r + c) % 2 == 0) CandyColor.YELLOW else CandyColor.PURPLE
                board[r][c] = CandyItem((r * 8 + c).toLong(), color)
            }
        }
        // 4 in a row horizontally
        board[2][1] = CandyItem(201, CandyColor.GREEN)
        board[2][2] = CandyItem(202, CandyColor.GREEN)
        board[2][3] = CandyItem(203, CandyColor.GREEN)
        board[2][4] = CandyItem(204, CandyColor.GREEN)

        val result = engine.findMatches(board, Position(2, 2))
        assertFalse(result.specialSpawns.isEmpty())
        val spawned = result.specialSpawns.values.first()
        assertEquals(SpecialType.STRIPED_VERTICAL, spawned.special)
    }
}
