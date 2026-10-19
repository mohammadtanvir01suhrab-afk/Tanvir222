package com.example.data.model

enum class CandyColor {
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE,
    PURPLE
}

enum class SpecialType {
    NONE,
    STRIPED_HORIZONTAL,
    STRIPED_VERTICAL,
    WRAPPED,      // Explodes in 3x3 radius
    COLOR_BOMB    // Clears all candies of a chosen color
}

data class CandyItem(
    val id: Long = 0L,
    val color: CandyColor,
    val special: SpecialType = SpecialType.NONE,
    val isMatched: Boolean = false,
    val isSelected: Boolean = false
)

data class Position(
    val row: Int,
    val col: Int
)
