package com.example.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CandySoundEffectPlayer
import com.example.data.model.CandyColor
import com.example.data.model.CandyItem
import com.example.data.model.LevelConfig
import com.example.data.model.Position
import com.example.data.model.SpecialType
import com.example.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameStatus {
    PLAYING,
    LEVEL_WON,
    LEVEL_LOST,
    ANIMATING
}

enum class ActiveBooster {
    NONE,
    HAMMER,
    COLOR_BOMB
}

data class GameUiState(
    val levelConfig: LevelConfig = LevelConfig.getLevel(1),
    val board: List<List<CandyItem?>> = emptyList(),
    val selectedPosition: Position? = null,
    val score: Int = 0,
    val remainingMoves: Int = 25,
    val stars: Int = 0,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val comboText: String? = null,
    val activeBooster: ActiveBooster = ActiveBooster.NONE,
    val feedbackMessage: String? = null
)

class GameViewModel(
    private val userRepository: UserRepository,
    val soundPlayer: CandySoundEffectPlayer = CandySoundEffectPlayer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var engine = CandyBoardEngine(8, 8, 5)
    private var internalBoard: Array<Array<CandyItem?>> = emptyArray()
    private var currentUserId: String = ""

    fun initialize(userId: String, levelNumber: Int) {
        currentUserId = userId
        val config = LevelConfig.getLevel(levelNumber)
        engine = CandyBoardEngine(config.rows, config.cols, config.numColors)
        internalBoard = engine.createInitialBoard()

        _uiState.update {
            GameUiState(
                levelConfig = config,
                board = internalBoard.map { row -> row.toList() },
                selectedPosition = null,
                score = 0,
                remainingMoves = config.moves,
                stars = 0,
                gameStatus = GameStatus.PLAYING,
                comboText = null,
                activeBooster = ActiveBooster.NONE
            )
        }
    }

    fun onCandyClicked(pos: Position) {
        val state = _uiState.value
        if (state.gameStatus != GameStatus.PLAYING) return

        // Handle Active Booster
        if (state.activeBooster == ActiveBooster.HAMMER) {
            useHammerOn(pos)
            return
        }
        if (state.activeBooster == ActiveBooster.COLOR_BOMB) {
            useColorBombBoosterOn(pos)
            return
        }

        val selected = state.selectedPosition
        if (selected == null) {
            // Select candy
            soundPlayer.playButtonClick()
            _uiState.update { it.copy(selectedPosition = pos) }
        } else if (selected == pos) {
            // Deselect
            _uiState.update { it.copy(selectedPosition = null) }
        } else if (engine.isAdjacent(selected, pos)) {
            // Try swap
            _uiState.update { it.copy(selectedPosition = null) }
            attemptSwap(selected, pos)
        } else {
            // Select new candy
            soundPlayer.playButtonClick()
            _uiState.update { it.copy(selectedPosition = pos) }
        }
    }

    private fun attemptSwap(p1: Position, p2: Position) {
        viewModelScope.launch {
            _uiState.update { it.copy(gameStatus = GameStatus.ANIMATING) }
            soundPlayer.playSwap()

            val c1 = internalBoard[p1.row][p1.col] ?: return@launch
            val c2 = internalBoard[p2.row][p2.col] ?: return@launch

            // Check if special Color Bomb swap
            if (c1.special == SpecialType.COLOR_BOMB && c2.special == SpecialType.COLOR_BOMB) {
                soundPlayer.playColorBomb()
                val result = engine.applyDoubleColorBomb(internalBoard, p1, p2)
                applyMatchChain(result, "SUGAR CRUSH!!")
                return@launch
            } else if (c1.special == SpecialType.COLOR_BOMB) {
                soundPlayer.playColorBomb()
                val result = engine.applyColorBomb(internalBoard, p1, c2.color)
                applyMatchChain(result, "COLOR EXPLOSION!")
                return@launch
            } else if (c2.special == SpecialType.COLOR_BOMB) {
                soundPlayer.playColorBomb()
                val result = engine.applyColorBomb(internalBoard, p2, c1.color)
                applyMatchChain(result, "COLOR EXPLOSION!")
                return@launch
            }

            // Normal swap test
            internalBoard[p1.row][p1.col] = c2
            internalBoard[p2.row][p2.col] = c1
            syncBoard()

            val initialMatches = engine.findMatches(internalBoard, p1, p2)
            if (initialMatches.matchedPositions.isNotEmpty()) {
                // Successful move: decrement remaining moves
                val newMoves = _uiState.value.remainingMoves - 1
                _uiState.update { it.copy(remainingMoves = newMoves) }

                applyMatchChain(initialMatches)
            } else {
                // Revert swap
                delay(220)
                internalBoard[p1.row][p1.col] = c1
                internalBoard[p2.row][p2.col] = c2
                syncBoard()
                _uiState.update { it.copy(gameStatus = GameStatus.PLAYING) }
            }
        }
    }

    private suspend fun applyMatchChain(initialResult: MatchResult, initialBanner: String? = null) {
        var currentResult = initialResult
        var combo = 1
        var totalScoreWon = 0

        if (initialBanner != null) {
            showComboBanner(initialBanner)
        }

        while (currentResult.matchedPositions.isNotEmpty()) {
            soundPlayer.playMatch(combo)
            val stepPoints = currentResult.points * combo
            totalScoreWon += stepPoints

            val newScore = _uiState.value.score + stepPoints
            val cfg = _uiState.value.levelConfig
            val stars = when {
                newScore >= cfg.star3Score -> 3
                newScore >= cfg.star2Score -> 2
                newScore >= cfg.star1Score -> 1
                else -> 0
            }

            if (combo == 2) showComboBanner("SWEET!")
            if (combo == 3) showComboBanner("TASTY!")
            if (combo == 4) showComboBanner("DELICIOUS!!")
            if (combo >= 5) showComboBanner("DIVINE!!!")

            _uiState.update {
                it.copy(
                    score = newScore,
                    stars = stars
                )
            }

            delay(240)
            engine.dropAndFill(internalBoard, currentResult.matchedPositions, currentResult.specialSpawns)
            syncBoard()
            delay(220)

            combo++
            currentResult = engine.findMatches(internalBoard)
        }

        // Check board for valid moves
        if (!engine.hasPossibleMoves(internalBoard)) {
            showComboBanner("NO MOVES - SHUFFLING!")
            delay(400)
            engine.shuffleBoard(internalBoard)
            syncBoard()
        }

        // Check Win / Lose conditions
        val finalScore = _uiState.value.score
        val finalMoves = _uiState.value.remainingMoves
        val cfg = _uiState.value.levelConfig

        if (finalScore >= cfg.targetScore) {
            soundPlayer.playLevelComplete()
            _uiState.update { it.copy(gameStatus = GameStatus.LEVEL_WON) }
            saveProgressToFirestore(cfg.levelNumber, finalScore)
        } else if (finalMoves <= 0) {
            soundPlayer.playLevelFailed()
            _uiState.update { it.copy(gameStatus = GameStatus.LEVEL_LOST) }
        } else {
            _uiState.update { it.copy(gameStatus = GameStatus.PLAYING) }
        }
    }

    private fun showComboBanner(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(comboText = text) }
            delay(1200)
            _uiState.update { if (it.comboText == text) it.copy(comboText = null) else it }
        }
    }

    private fun syncBoard() {
        _uiState.update { it.copy(board = internalBoard.map { row -> row.toList() }) }
    }

    private fun saveProgressToFirestore(level: Int, score: Int) {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            userRepository.updateLevelProgress(currentUserId, level, score)
        }
    }

    // Boosters

    fun activateBooster(booster: ActiveBooster, coinCost: Long) {
        viewModelScope.launch {
            val result = userRepository.spendCoins(currentUserId, coinCost)
            if (result.isSuccess) {
                _uiState.update { it.copy(activeBooster = booster) }
            } else {
                _uiState.update { it.copy(feedbackMessage = "Need $coinCost coins for this booster!") }
                delay(2000)
                _uiState.update { it.copy(feedbackMessage = null) }
            }
        }
    }

    fun buyExtraMoves() {
        viewModelScope.launch {
            val result = userRepository.spendCoins(currentUserId, 50L)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        remainingMoves = it.remainingMoves + 5,
                        gameStatus = GameStatus.PLAYING
                    )
                }
            } else {
                _uiState.update { it.copy(feedbackMessage = "Need 50 coins for +5 moves!") }
                delay(2000)
                _uiState.update { it.copy(feedbackMessage = null) }
            }
        }
    }

    private fun useHammerOn(pos: Position) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeBooster = ActiveBooster.NONE, gameStatus = GameStatus.ANIMATING) }
            soundPlayer.playSpecialExplosion()
            internalBoard[pos.row][pos.col] = null
            engine.dropAndFill(internalBoard, setOf(pos), emptyMap())
            syncBoard()
            delay(200)
            val matches = engine.findMatches(internalBoard)
            applyMatchChain(matches)
        }
    }

    private fun useColorBombBoosterOn(pos: Position) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeBooster = ActiveBooster.NONE) }
            engine.createColorBombAt(internalBoard, pos)
            syncBoard()
            soundPlayer.playSpecialExplosion()
        }
    }

    fun shuffleBoardWithCoins() {
        viewModelScope.launch {
            val result = userRepository.spendCoins(currentUserId, 20L)
            if (result.isSuccess) {
                engine.shuffleBoard(internalBoard)
                syncBoard()
                soundPlayer.playSwap()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Need 20 coins to shuffle!") }
                delay(2000)
                _uiState.update { it.copy(feedbackMessage = null) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
