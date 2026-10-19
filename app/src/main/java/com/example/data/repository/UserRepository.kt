package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CoinTransaction
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class UserRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "tanvir_candy_crush_prefs",
        Context.MODE_PRIVATE
    )

    private val _usersMapFlow = MutableStateFlow<Map<String, User>>(emptyMap())
    private val _transactionsFlow = MutableStateFlow<List<CoinTransaction>>(emptyList())
    private val _adminUidsFlow = MutableStateFlow<Set<String>>(setOf("tanvir_71"))
    private val _activeUserIdFlow = MutableStateFlow<String?>("tanvir_71")

    val activeUserFlow: Flow<User?> = _usersMapFlow.map { map ->
        val activeId = _activeUserIdFlow.value
        if (activeId != null) map[activeId] else null
    }

    init {
        loadFromPrefs()
    }

    @Synchronized
    private fun loadFromPrefs() {
        val usersJsonStr = prefs.getString("saved_users", null)
        val loadedUsers = mutableMapOf<String, User>()

        if (usersJsonStr != null) {
            try {
                val array = JSONArray(usersJsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val user = User(
                        id = obj.optString("id", ""),
                        uid = obj.optString("uid", ""),
                        displayName = obj.optString("displayName", "Player"),
                        email = obj.optString("email", ""),
                        photoUrl = obj.optString("photoUrl", ""),
                        coins = obj.optLong("coins", 100L),
                        currentLevel = obj.optInt("currentLevel", 1),
                        highestUnlockedLevel = obj.optInt("highestUnlockedLevel", 1),
                        totalScore = obj.optLong("totalScore", 0L),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                    if (user.uid.isNotBlank()) {
                        loadedUsers[user.uid] = user
                    }
                }
            } catch (_: Exception) {
            }
        }

        // If empty, create initial Tanvir profile
        if (loadedUsers.isEmpty()) {
            val initial = User(
                id = "tanvir_71",
                uid = "tanvir_71",
                displayName = "Tanvir",
                email = "mohammadtanvir01suhrab@gmail.com",
                photoUrl = "",
                coins = 100L,
                currentLevel = 1,
                highestUnlockedLevel = 1,
                totalScore = 0L
            )
            loadedUsers[initial.uid] = initial
        }

        val txJsonStr = prefs.getString("saved_transactions", null)
        val loadedTx = mutableListOf<CoinTransaction>()
        if (txJsonStr != null) {
            try {
                val array = JSONArray(txJsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedTx.add(
                        CoinTransaction(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            adminUid = obj.optString("adminUid", "tanvir_71"),
                            adminEmail = obj.optString("adminEmail", "admin@tanvircandycrush.com"),
                            targetUid = obj.optString("targetUid", ""),
                            amount = obj.optLong("amount", 0L),
                            note = obj.optString("note", ""),
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        val activeUid = prefs.getString("active_uid", loadedUsers.keys.firstOrNull() ?: "tanvir_71")

        _usersMapFlow.value = loadedUsers
        _transactionsFlow.value = loadedTx.sortedByDescending { it.timestampMillis }
        _activeUserIdFlow.value = activeUid
    }

    @Synchronized
    private fun saveUsersToPrefs() {
        val array = JSONArray()
        for (u in _usersMapFlow.value.values) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("uid", u.uid)
                put("displayName", u.displayName)
                put("email", u.email)
                put("photoUrl", u.photoUrl)
                put("coins", u.coins)
                put("currentLevel", u.currentLevel)
                put("highestUnlockedLevel", u.highestUnlockedLevel)
                put("totalScore", u.totalScore)
                put("createdAtMillis", u.createdAtMillis)
                put("updatedAtMillis", u.updatedAtMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_users", array.toString()).apply()
    }

    @Synchronized
    private fun saveTransactionsToPrefs() {
        val array = JSONArray()
        for (tx in _transactionsFlow.value) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("adminUid", tx.adminUid)
                put("adminEmail", tx.adminEmail)
                put("targetUid", tx.targetUid)
                put("amount", tx.amount)
                put("note", tx.note)
                put("timestampMillis", tx.timestampMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_transactions", array.toString()).apply()
    }

    fun requireUserId(): String {
        return _activeUserIdFlow.value ?: "tanvir_71"
    }

    fun getActiveUser(): User? {
        val activeUid = _activeUserIdFlow.value ?: return null
        return _usersMapFlow.value[activeUid]
    }

    fun setActiveUser(user: User) {
        val currentMap = _usersMapFlow.value.toMutableMap()
        currentMap[user.uid] = user
        _usersMapFlow.value = currentMap
        _activeUserIdFlow.value = user.uid
        prefs.edit().putString("active_uid", user.uid).apply()
        saveUsersToPrefs()
    }

    fun signOut() {
        _activeUserIdFlow.value = null
        prefs.edit().remove("active_uid").apply()
    }

    fun observeUserProfile(uid: String): Flow<User?> {
        return _usersMapFlow.map { map -> map[uid] }
    }

    suspend fun createOrSyncProfile(user: User): Result<Unit> {
        val uid = user.uid.ifEmpty { requireUserId() }
        val currentMap = _usersMapFlow.value.toMutableMap()
        val existing = currentMap[uid]
        val updated = if (existing != null) {
            existing.copy(
                displayName = user.displayName.ifBlank { existing.displayName },
                email = user.email.ifBlank { existing.email },
                photoUrl = user.photoUrl.ifBlank { existing.photoUrl },
                updatedAtMillis = System.currentTimeMillis()
            )
        } else {
            user.copy(
                id = uid,
                uid = uid,
                displayName = user.displayName.ifBlank { "Sweet Player" },
                coins = if (user.coins <= 0) 100L else user.coins,
                highestUnlockedLevel = if (user.highestUnlockedLevel <= 0) 1 else user.highestUnlockedLevel,
                currentLevel = if (user.currentLevel <= 0) 1 else user.currentLevel,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        currentMap[uid] = updated
        _usersMapFlow.value = currentMap
        _activeUserIdFlow.value = uid
        saveUsersToPrefs()
        return Result.success(Unit)
    }

    suspend fun updateLevelProgress(
        uid: String,
        levelCompleted: Int,
        scoreEarned: Int
    ): Result<Unit> {
        val currentMap = _usersMapFlow.value.toMutableMap()
        val current = currentMap[uid] ?: return Result.failure(IllegalStateException("User not found"))

        val nextLevel = (levelCompleted + 1).coerceAtMost(100)
        val newHighest = maxOf(current.highestUnlockedLevel, nextLevel)
        // Level completion bonus coins: 15 coins per cleared level!
        val bonusCoins = 15L

        val updated = current.copy(
            currentLevel = nextLevel,
            highestUnlockedLevel = newHighest,
            totalScore = current.totalScore + scoreEarned,
            coins = current.coins + bonusCoins,
            updatedAtMillis = System.currentTimeMillis()
        )
        currentMap[uid] = updated
        _usersMapFlow.value = currentMap
        saveUsersToPrefs()
        return Result.success(Unit)
    }

    suspend fun spendCoins(uid: String, amount: Long): Result<Unit> {
        val currentMap = _usersMapFlow.value.toMutableMap()
        val current = currentMap[uid] ?: return Result.failure(IllegalStateException("User not found"))

        if (current.coins < amount) {
            return Result.failure(IllegalStateException("Not enough coins!"))
        }

        val updated = current.copy(
            coins = current.coins - amount,
            updatedAtMillis = System.currentTimeMillis()
        )
        currentMap[uid] = updated
        _usersMapFlow.value = currentMap
        saveUsersToPrefs()
        return Result.success(Unit)
    }

    suspend fun grantCoinsAsAdmin(
        targetUid: String,
        amount: Long,
        note: String
    ): Result<String> {
        if (targetUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Player UID cannot be empty"))
        }
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Coin amount must be greater than 0"))
        }

        val currentMap = _usersMapFlow.value.toMutableMap()
        val targetUser = currentMap[targetUid] ?: User(
            id = targetUid,
            uid = targetUid,
            displayName = "Player ($targetUid)",
            coins = 0L,
            highestUnlockedLevel = 1,
            currentLevel = 1
        )

        val newBalance = targetUser.coins + amount
        val updatedUser = targetUser.copy(
            coins = newBalance,
            updatedAtMillis = System.currentTimeMillis()
        )
        currentMap[targetUid] = updatedUser
        _usersMapFlow.value = currentMap
        saveUsersToPrefs()

        val tx = CoinTransaction(
            id = UUID.randomUUID().toString(),
            adminUid = requireUserId(),
            adminEmail = "mohammadtanvir01suhrab@gmail.com",
            targetUid = targetUid,
            amount = amount,
            note = note.trim(),
            timestampMillis = System.currentTimeMillis()
        )
        val updatedTxList = listOf(tx) + _transactionsFlow.value
        _transactionsFlow.value = updatedTxList
        saveTransactionsToPrefs()

        val targetName = updatedUser.displayName
        return Result.success("Successfully delivered $amount coins to $targetName! New balance: $newBalance coins.")
    }

    fun observeRecentTransactions(): Flow<List<CoinTransaction>> {
        return _transactionsFlow.asStateFlow()
    }

    suspend fun registerAdminDoc(uid: String, email: String): Result<Unit> {
        val set = _adminUidsFlow.value.toMutableSet()
        set.add(uid)
        _adminUidsFlow.value = set
        return Result.success(Unit)
    }

    suspend fun isUserAdmin(uid: String, email: String?): Boolean {
        if (email == "mohammadtanvir01suhrab@gmail.com") return true
        if (uid == "tanvir_71") return true
        return _adminUidsFlow.value.contains(uid)
    }
}
