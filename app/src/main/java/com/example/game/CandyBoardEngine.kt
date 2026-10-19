package com.example.game

import com.example.data.model.CandyColor
import com.example.data.model.CandyItem
import com.example.data.model.Position
import com.example.data.model.SpecialType
import kotlin.random.Random

data class MatchResult(
    val matchedPositions: Set<Position>,
    val specialSpawns: Map<Position, CandyItem>,
    val points: Int,
    val comboStep: Int
)

class CandyBoardEngine(
    val rows: Int = 8,
    val cols: Int = 8,
    val numColors: Int = 5
) {
    private var nextId = 1L
    private val availableColors: List<CandyColor> = CandyColor.values().take(numColors)

    fun createInitialBoard(): Array<Array<CandyItem?>> {
        val board = Array(rows) { Array<CandyItem?>(cols) { null } }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                var color: CandyColor
                do {
                    color = availableColors.random()
                } while (
                    (r >= 2 && board[r - 1][c]?.color == color && board[r - 2][c]?.color == color) ||
                    (c >= 2 && board[r][c - 1]?.color == color && board[r][c - 2]?.color == color)
                )
                board[r][c] = CandyItem(id = nextId++, color = color)
            }
        }
        return board
    }

    fun isAdjacent(p1: Position, p2: Position): Boolean {
        val dr = kotlin.math.abs(p1.row - p2.row)
        val dc = kotlin.math.abs(p1.col - p2.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    fun canSwap(board: Array<Array<CandyItem?>>, p1: Position, p2: Position): Boolean {
        if (!isAdjacent(p1, p2)) return false
        val c1 = board[p1.row][p1.col] ?: return false
        val c2 = board[p2.row][p2.col] ?: return false

        // Color bomb swapped with anything is always a valid move
        if (c1.special == SpecialType.COLOR_BOMB || c2.special == SpecialType.COLOR_BOMB) {
            return true
        }

        // Test swap
        val copy = Array(rows) { r -> Array(cols) { c -> board[r][c] } }
        copy[p1.row][p1.col] = c2
        copy[p2.row][p2.col] = c1

        val matches = findMatches(copy, p1, p2)
        return matches.matchedPositions.isNotEmpty()
    }

    fun findMatches(
        board: Array<Array<CandyItem?>>,
        pivot1: Position? = null,
        pivot2: Position? = null
    ): MatchResult {
        val matched = mutableSetOf<Position>()
        val horizontalRuns = mutableListOf<List<Position>>()
        val verticalRuns = mutableListOf<List<Position>>()

        // Horizontal check
        for (r in 0 until rows) {
            var currentRun = mutableListOf<Position>()
            for (c in 0 until cols) {
                val item = board[r][c]
                if (item != null && item.special != SpecialType.COLOR_BOMB) {
                    if (currentRun.isEmpty()) {
                        currentRun.add(Position(r, c))
                    } else {
                        val prevItem = board[currentRun.first().row][currentRun.first().col]
                        if (prevItem != null && prevItem.color == item.color) {
                            currentRun.add(Position(r, c))
                        } else {
                            if (currentRun.size >= 3) {
                                horizontalRuns.add(currentRun.toList())
                                matched.addAll(currentRun)
                            }
                            currentRun = mutableListOf(Position(r, c))
                        }
                    }
                } else {
                    if (currentRun.size >= 3) {
                        horizontalRuns.add(currentRun.toList())
                        matched.addAll(currentRun)
                    }
                    currentRun.clear()
                }
            }
            if (currentRun.size >= 3) {
                horizontalRuns.add(currentRun.toList())
                matched.addAll(currentRun)
            }
        }

        // Vertical check
        for (c in 0 until cols) {
            var currentRun = mutableListOf<Position>()
            for (r in 0 until rows) {
                val item = board[r][c]
                if (item != null && item.special != SpecialType.COLOR_BOMB) {
                    if (currentRun.isEmpty()) {
                        currentRun.add(Position(r, c))
                    } else {
                        val prevItem = board[currentRun.first().row][currentRun.first().col]
                        if (prevItem != null && prevItem.color == item.color) {
                            currentRun.add(Position(r, c))
                        } else {
                            if (currentRun.size >= 3) {
                                verticalRuns.add(currentRun.toList())
                                matched.addAll(currentRun)
                            }
                            currentRun = mutableListOf(Position(r, c))
                        }
                    }
                } else {
                    if (currentRun.size >= 3) {
                        verticalRuns.add(currentRun.toList())
                        matched.addAll(currentRun)
                    }
                    currentRun.clear()
                }
            }
            if (currentRun.size >= 3) {
                verticalRuns.add(currentRun.toList())
                matched.addAll(currentRun)
            }
        }

        // Determine special candy creations
        val specialSpawns = mutableMapOf<Position, CandyItem>()

        // Check for 5-in-a-row (Color Bomb)
        for (run in horizontalRuns + verticalRuns) {
            if (run.size >= 5) {
                val spawnPos = selectSpawnPosition(run, pivot1, pivot2)
                val color = board[spawnPos.row][spawnPos.col]?.color ?: availableColors.first()
                specialSpawns[spawnPos] = CandyItem(
                    id = nextId++,
                    color = color,
                    special = SpecialType.COLOR_BOMB
                )
            }
        }

        // Check for T / L shapes (Wrapped Candy)
        for (hRun in horizontalRuns) {
            for (vRun in verticalRuns) {
                val intersection = hRun.intersect(vRun.toSet())
                if (intersection.isNotEmpty()) {
                    val intersectPos = intersection.first()
                    if (!specialSpawns.containsKey(intersectPos)) {
                        val color = board[intersectPos.row][intersectPos.col]?.color ?: availableColors.first()
                        specialSpawns[intersectPos] = CandyItem(
                            id = nextId++,
                            color = color,
                            special = SpecialType.WRAPPED
                        )
                    }
                }
            }
        }

        // Check for 4-in-a-row (Striped Candy)
        for (run in horizontalRuns) {
            if (run.size == 4 && run.none { specialSpawns.containsKey(it) }) {
                val spawnPos = selectSpawnPosition(run, pivot1, pivot2)
                val color = board[spawnPos.row][spawnPos.col]?.color ?: availableColors.first()
                specialSpawns[spawnPos] = CandyItem(
                    id = nextId++,
                    color = color,
                    special = SpecialType.STRIPED_VERTICAL
                )
            }
        }
        for (run in verticalRuns) {
            if (run.size == 4 && run.none { specialSpawns.containsKey(it) }) {
                val spawnPos = selectSpawnPosition(run, pivot1, pivot2)
                val color = board[spawnPos.row][spawnPos.col]?.color ?: availableColors.first()
                specialSpawns[spawnPos] = CandyItem(
                    id = nextId++,
                    color = color,
                    special = SpecialType.STRIPED_HORIZONTAL
                )
            }
        }

        // Expand matched positions if any matched candy is special
        val allAffected = mutableSetOf<Position>()
        allAffected.addAll(matched)

        for (pos in matched) {
            val item = board[pos.row][pos.col] ?: continue
            when (item.special) {
                SpecialType.STRIPED_HORIZONTAL -> {
                    for (c in 0 until cols) allAffected.add(Position(pos.row, c))
                }
                SpecialType.STRIPED_VERTICAL -> {
                    for (r in 0 until rows) allAffected.add(Position(r, pos.col))
                }
                SpecialType.WRAPPED -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = pos.row + dr
                            val nc = pos.col + dc
                            if (nr in 0 until rows && nc in 0 until cols) {
                                allAffected.add(Position(nr, nc))
                            }
                        }
                    }
                }
                SpecialType.COLOR_BOMB -> {
                    // Handled separately when swapped
                }
                SpecialType.NONE -> {}
            }
        }

        val basePoints = allAffected.size * 60 + specialSpawns.size * 200
        return MatchResult(
            matchedPositions = allAffected,
            specialSpawns = specialSpawns,
            points = basePoints,
            comboStep = 1
        )
    }

    private fun selectSpawnPosition(
        run: List<Position>,
        pivot1: Position?,
        pivot2: Position?
    ): Position {
        if (pivot1 != null && run.contains(pivot1)) return pivot1
        if (pivot2 != null && run.contains(pivot2)) return pivot2
        return run[run.size / 2]
    }

    fun applyColorBomb(
        board: Array<Array<CandyItem?>>,
        bombPos: Position,
        targetColor: CandyColor
    ): MatchResult {
        val affected = mutableSetOf<Position>()
        affected.add(bombPos)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val candy = board[r][c]
                if (candy != null && candy.color == targetColor) {
                    affected.add(Position(r, c))
                }
            }
        }

        return MatchResult(
            matchedPositions = affected,
            specialSpawns = emptyMap(),
            points = affected.size * 100,
            comboStep = 1
        )
    }

    fun applyDoubleColorBomb(
        board: Array<Array<CandyItem?>>,
        p1: Position,
        p2: Position
    ): MatchResult {
        val affected = mutableSetOf<Position>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                affected.add(Position(r, c))
            }
        }
        return MatchResult(
            matchedPositions = affected,
            specialSpawns = emptyMap(),
            points = rows * cols * 150,
            comboStep = 1
        )
    }

    fun dropAndFill(
        board: Array<Array<CandyItem?>>,
        matched: Set<Position>,
        spawns: Map<Position, CandyItem>
    ) {
        // Clear matched except where specials spawned
        for (pos in matched) {
            if (spawns.containsKey(pos)) {
                board[pos.row][pos.col] = spawns[pos]
            } else {
                board[pos.row][pos.col] = null
            }
        }

        // Collapse each column down
        for (c in 0 until cols) {
            var writeRow = rows - 1
            for (r in rows - 1 downTo 0) {
                if (board[r][c] != null) {
                    if (r != writeRow) {
                        board[writeRow][c] = board[r][c]
                        board[r][c] = null
                    }
                    writeRow--
                }
            }
            // Fill empty spaces from top
            while (writeRow >= 0) {
                board[writeRow][c] = CandyItem(
                    id = nextId++,
                    color = availableColors.random()
                )
                writeRow--
            }
        }
    }

    fun hasPossibleMoves(board: Array<Array<CandyItem?>>): Boolean {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (c + 1 < cols && canSwap(board, Position(r, c), Position(r, c + 1))) return true
                if (r + 1 < rows && canSwap(board, Position(r, c), Position(r + 1, c))) return true
            }
        }
        return false
    }

    fun shuffleBoard(board: Array<Array<CandyItem?>>) {
        val flatCandies = mutableListOf<CandyItem>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                board[r][c]?.let { flatCandies.add(it) }
            }
        }
        flatCandies.shuffle()
        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (idx < flatCandies.size) {
                    board[r][c] = flatCandies[idx++]
                }
            }
        }
    }

    fun createColorBombAt(board: Array<Array<CandyItem?>>, pos: Position) {
        board[pos.row][pos.col] = CandyItem(
            id = nextId++,
            color = board[pos.row][pos.col]?.color ?: availableColors.first(),
            special = SpecialType.COLOR_BOMB
        )
    }
}
