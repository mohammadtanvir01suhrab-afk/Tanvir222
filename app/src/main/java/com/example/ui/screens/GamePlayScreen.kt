package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Position
import com.example.game.ActiveBooster
import com.example.game.GameStatus
import com.example.game.GameViewModel
import com.example.ui.components.CandyView
import com.example.ui.components.CoinBadge
import com.example.ui.theme.CandyBackground
import com.example.ui.theme.CandyPrimary
import com.example.ui.theme.CandySecondary
import com.example.ui.theme.CandyStar
import com.example.ui.theme.CoinGold

@Composable
fun GamePlayScreen(
    viewModel: GameViewModel,
    coins: Long,
    onBack: () -> Unit,
    onNextLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var soundEnabled by remember { mutableStateOf(viewModel.soundPlayer.soundEnabled) }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF20073B),
                        CandyBackground,
                        Color(0xFF38006B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Back, Level Title, CoinBadge, Sound
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1654))
                        .testTag("game_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Level ${state.levelConfig.levelNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = state.levelConfig.title,
                        color = CandySecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinBadge(coins = coins)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            soundEnabled = !soundEnabled
                            viewModel.soundPlayer.soundEnabled = soundEnabled
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C1654))
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Sound Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Score & Moves Objective Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("stats_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E104D).copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Moves Remaining Circular Indicator
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (state.remainingMoves <= 5) Color(0xFFFF1744) else Color(0xFFFF9800),
                                        if (state.remainingMoves <= 5) Color(0xFFD50000) else Color(0xFFE65100)
                                    )
                                )
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.remainingMoves}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "MOVES",
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Score Progress & Stars
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Score: ${state.score}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Target: ${state.levelConfig.targetScore}",
                                color = CoinGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val progress = (state.score.toFloat() / state.levelConfig.targetScore.toFloat()).coerceIn(0f, 1f)
                        val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = CandyPrimary,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stars row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(3) { index ->
                                val active = state.stars > index
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (active) CandyStar else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Match-3 Grid Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1E0638).copy(alpha = 0.85f))
                    .border(3.dp, Color(0xFFD81B60).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    state.board.forEachIndexed { r, rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowList.forEachIndexed { c, candy ->
                                val pos = Position(r, c)
                                val isSelected = state.selectedPosition == pos
                                CandyView(
                                    candy = candy,
                                    isSelected = isSelected,
                                    onClick = { viewModel.onCandyClicked(pos) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Combo Floating Text
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.comboText != null,
                    enter = scaleIn(tween(200, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = scaleOut(tween(250)) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF007F), Color(0xFFFF9900))))
                            .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = state.comboText ?: "",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Boosters Tray
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("boosters_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1448).copy(alpha = 0.90f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoosterButton(
                        icon = Icons.Default.Build,
                        title = "Hammer",
                        cost = 30,
                        isActive = state.activeBooster == ActiveBooster.HAMMER,
                        onClick = { viewModel.activateBooster(ActiveBooster.HAMMER, 30L) }
                    )
                    BoosterButton(
                        icon = Icons.Default.Star,
                        title = "Rainbow",
                        cost = 60,
                        isActive = state.activeBooster == ActiveBooster.COLOR_BOMB,
                        onClick = { viewModel.activateBooster(ActiveBooster.COLOR_BOMB, 60L) }
                    )
                    BoosterButton(
                        icon = Icons.Default.Refresh,
                        title = "Shuffle",
                        cost = 20,
                        isActive = false,
                        onClick = { viewModel.shuffleBoardWithCoins() }
                    )
                    BoosterButton(
                        icon = Icons.Default.Add,
                        title = "+5 Moves",
                        cost = 50,
                        isActive = false,
                        onClick = { viewModel.buyExtraMoves() }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // WIN MODAL
        AnimatedVisibility(
            visible = state.gameStatus == GameStatus.LEVEL_WON,
            enter = fadeIn() + scaleIn(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                        .testTag("win_modal"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E104D)),
                    elevation = CardDefaults.cardElevation(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SWEET VICTORY!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = CoinGold
                        )
                        Text(
                            text = "Level ${state.levelConfig.levelNumber} Completed",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Star display
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            repeat(3) { idx ->
                                val earned = state.stars > idx
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (earned) CandyStar else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Final Score: ${state.score}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "+${state.levelConfig.coinReward} Coins Reward",
                            color = CandySecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onNextLevel(state.levelConfig.levelNumber + 1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("next_level_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CandyPrimary)
                        ) {
                            Text("NEXT LEVEL", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("win_map_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
                        ) {
                            Text("LEVEL MAP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // LOSE MODAL
        AnimatedVisibility(
            visible = state.gameStatus == GameStatus.LEVEL_LOST,
            enter = fadeIn() + scaleIn(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                        .testTag("lose_modal"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E104D)),
                    elevation = CardDefaults.cardElevation(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "OUT OF MOVES!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252)
                        )
                        Text(
                            text = "You were so close!",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Score: ${state.score} / ${state.levelConfig.targetScore}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.buyExtraMoves() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("buy_moves_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoinGold)
                        ) {
                            Text("+5 MOVES (50 COINS)", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.initialize(
                                    userId = viewModel.soundPlayer.run { "" },
                                    levelNumber = state.levelConfig.levelNumber
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("retry_level_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CandyPrimary)
                        ) {
                            Text("RETRY", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("lose_map_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Text("LEVEL MAP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoosterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    cost: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) CandyPrimary.copy(alpha = 0.4f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .testTag("booster_$title")
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isActive) CandyPrimary else Color(0xFF4527A0))
                .border(1.dp, if (isActive) CoinGold else Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$cost c",
            color = CoinGold,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
