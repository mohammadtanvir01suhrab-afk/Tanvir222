package com.example.data.model

data class LevelConfig(
    val levelNumber: Int,
    val title: String,
    val targetScore: Int,
    val moves: Int,
    val numColors: Int,
    val rows: Int = 8,
    val cols: Int = 8,
    val coinReward: Int = 30
) {
    val star1Score: Int get() = (targetScore * 0.7).toInt()
    val star2Score: Int get() = targetScore
    val star3Score: Int get() = (targetScore * 1.4).toInt()

    companion object {
        private val worldThemes = listOf(
            "Sugar Valley",
            "Candy Cloud",
            "Gummy Grove",
            "Chocolate Creek",
            "Caramel Coast",
            "Marshmallow Mountain",
            "Lollipop Lake",
            "Jelly Jungle",
            "Toffee Tunnels",
            "Royal Candy Kingdom"
        )

        fun getLevel(level: Int): LevelConfig {
            val clampedLevel = level.coerceIn(1, 100)
            val worldIndex = ((clampedLevel - 1) / 10).coerceIn(0, worldThemes.size - 1)
            val world = worldThemes[worldIndex]

            // Progressive difficulty curve
            val baseTarget = 1000 + (clampedLevel - 1) * 650 + ((clampedLevel * clampedLevel) * 6)
            val moveCount = (28 - (clampedLevel / 7)).coerceIn(15, 28)
            val colors = when {
                clampedLevel <= 5 -> 4
                clampedLevel <= 25 -> 5
                else -> 6
            }
            val reward = 25 + (clampedLevel / 5) * 5

            return LevelConfig(
                levelNumber = clampedLevel,
                title = "$world - Level $clampedLevel",
                targetScore = baseTarget,
                moves = moveCount,
                numColors = colors,
                rows = 8,
                cols = 8,
                coinReward = reward
            )
        }

        fun getAllLevels(): List<LevelConfig> {
            return (1..100).map { getLevel(it) }
        }
    }
}
