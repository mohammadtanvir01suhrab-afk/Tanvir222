package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LevelConfig
import com.example.ui.components.CoinBadge
import com.example.ui.theme.CandyBackground
import com.example.ui.theme.CandyPrimary
import com.example.ui.theme.CandySecondary
import com.example.ui.theme.CandyStar
import com.example.ui.theme.CoinGold

@Composable
fun LevelMapScreen(
    highestUnlockedLevel: Int,
    coins: Long,
    onLevelSelected: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val allLevels = remember { LevelConfig.getAllLevels() }
    var selectedLevelForPreview by remember { mutableStateOf<LevelConfig?>(null) }
    val gridState = rememberLazyGridState()

    // Scroll automatically to current unlocked level
    LaunchedEffect(highestUnlockedLevel) {
        val targetIndex = (highestUnlockedLevel - 1).coerceIn(0, 99)
        gridState.animateScrollToItem((targetIndex - 4).coerceAtLeast(0))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF19062D),
                        CandyBackground,
                        Color(0xFF38006B)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1654))
                        .testTag("map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "100 Candy Levels",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Unlocked: $highestUnlockedLevel / 100",
                        color = CandySecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                CoinBadge(coins = coins)
            }

            // Grid of 100 Levels
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("levels_grid")
            ) {
                items(allLevels) { config ->
                    val isUnlocked = config.levelNumber <= highestUnlockedLevel
                    val isCurrent = config.levelNumber == highestUnlockedLevel

                    LevelNode(
                        levelNumber = config.levelNumber,
                        isUnlocked = isUnlocked,
                        isCurrent = isCurrent,
                        onClick = {
                            if (isUnlocked) {
                                selectedLevelForPreview = config
                            }
                        }
                    )
                }
            }
        }

        // Preview Card Modal
        AnimatedVisibility(
            visible = selectedLevelForPreview != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { selectedLevelForPreview = null },
                contentAlignment = Alignment.Center
            ) {
                selectedLevelForPreview?.let { level ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clickable(enabled = false) {}
                            .testTag("level_preview_card"),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2F0C52)
                        ),
                        elevation = CardDefaults.cardElevation(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LEVEL ${level.levelNumber}",
                                    color = CoinGold,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                                IconButton(
                                    onClick = { selectedLevelForPreview = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }

                            Text(
                                text = level.title,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Target Score", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("${level.targetScore}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Moves", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("${level.moves}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Colors", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("${level.numColors}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val lvl = level.levelNumber
                                    selectedLevelForPreview = null
                                    onLevelSelected(lvl)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_level_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CandyPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PLAY LEVEL", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelNode(
    levelNumber: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isCurrent -> Brush.radialGradient(listOf(Color(0xFFFF4081), CandyPrimary))
        isUnlocked -> Brush.radialGradient(listOf(Color(0xFF29B6F6), Color(0xFF0288D1)))
        else -> Brush.radialGradient(listOf(Color(0xFF424242), Color(0xFF212121)))
    }

    val borderColor = when {
        isCurrent -> CoinGold
        isUnlocked -> Color.White.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.15f)
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(if (isCurrent) 3.dp else 1.5.dp, borderColor, CircleShape)
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_node_$levelNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "$levelNumber",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                if (!isCurrent) {
                    Row {
                        repeat(3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = CandyStar,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "PLAY",
                        color = CoinGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
