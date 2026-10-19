package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.User
import com.example.data.repository.UserRepository
import com.example.game.GameViewModel
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.TanvirCandyCrushTheme

enum class Screen {
    HOME,
    LEVEL_MAP,
    GAMEPLAY,
    PROFILE,
    ADMIN
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userRepository = UserRepository(this)

        setContent {
            TanvirCandyCrushTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(userRepository = userRepository)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(userRepository: UserRepository) {
    val activeUser by userRepository.activeUserFlow.collectAsStateWithLifecycle(
        initialValue = userRepository.getActiveUser()
    )

    if (activeUser == null) {
        AuthScreen(
            onAuthSuccess = { user ->
                userRepository.setActiveUser(user)
            }
        )
    } else {
        MainGameFlow(
            currentUser = activeUser!!,
            userRepository = userRepository,
            onSignOut = {
                userRepository.signOut()
            }
        )
    }
}

@Composable
fun MainGameFlow(
    currentUser: User,
    userRepository: UserRepository,
    onSignOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedLevel by remember { mutableIntStateOf(1) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.uid) {
        userRepository.createOrSyncProfile(currentUser)
    }

    val userProfile by userRepository.observeUserProfile(currentUser.uid)
        .collectAsStateWithLifecycle(initialValue = currentUser)

    val gameViewModel: GameViewModel = viewModel {
        GameViewModel(userRepository = userRepository)
    }

    var soundEnabled by remember { mutableStateOf(gameViewModel.soundPlayer.soundEnabled) }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                user = userProfile ?: currentUser,
                onPlayClicked = { level ->
                    selectedLevel = level
                    gameViewModel.initialize(userId = currentUser.uid, levelNumber = level)
                    currentScreen = Screen.GAMEPLAY
                },
                onMapClicked = { currentScreen = Screen.LEVEL_MAP },
                onProfileClicked = { currentScreen = Screen.PROFILE },
                onSettingsClicked = { showSettingsDialog = true }
            )
        }

        Screen.LEVEL_MAP -> {
            LevelMapScreen(
                highestUnlockedLevel = userProfile?.highestUnlockedLevel ?: 1,
                coins = userProfile?.coins ?: 100L,
                onLevelSelected = { level ->
                    selectedLevel = level
                    gameViewModel.initialize(userId = currentUser.uid, levelNumber = level)
                    currentScreen = Screen.GAMEPLAY
                },
                onBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.GAMEPLAY -> {
            GamePlayScreen(
                viewModel = gameViewModel,
                coins = userProfile?.coins ?: 100L,
                onBack = { currentScreen = Screen.HOME },
                onNextLevel = { nextLvl ->
                    val clamped = nextLvl.coerceIn(1, 100)
                    selectedLevel = clamped
                    gameViewModel.initialize(userId = currentUser.uid, levelNumber = clamped)
                }
            )
        }

        Screen.PROFILE -> {
            ProfileScreen(
                user = userProfile ?: currentUser,
                onBack = { currentScreen = Screen.HOME },
                onAdminPanelClicked = { currentScreen = Screen.ADMIN },
                onSignedOut = onSignOut
            )
        }

        Screen.ADMIN -> {
            AdminScreen(
                userRepository = userRepository,
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            soundEnabled = soundEnabled,
            onSoundToggled = { enabled ->
                soundEnabled = enabled
                gameViewModel.soundPlayer.soundEnabled = enabled
            },
            onOpenAdminPanel = {
                currentScreen = Screen.ADMIN
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
