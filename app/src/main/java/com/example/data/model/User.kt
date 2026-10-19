package com.example.data.model

data class User(
    val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val coins: Long = 100L,
    val currentLevel: Int = 1,
    val highestUnlockedLevel: Int = 1,
    val totalScore: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
