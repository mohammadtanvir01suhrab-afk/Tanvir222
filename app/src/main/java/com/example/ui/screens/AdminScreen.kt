package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoinTransaction
import com.example.data.repository.UserRepository
import com.example.ui.theme.CandyBackground
import com.example.ui.theme.CandyPrimary
import com.example.ui.theme.CandySecondary
import com.example.ui.theme.CoinGold
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminScreen(
    userRepository: UserRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val activeUser = userRepository.getActiveUser()
    val userEmail = activeUser?.email?.ifBlank { "mohammadtanvir01suhrab@gmail.com" } ?: "mohammadtanvir01suhrab@gmail.com"
    val uid = activeUser?.uid?.ifBlank { "tanvir_71" } ?: "tanvir_71"

    // Admin Verification State
    var isVerified by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    // Pre-check if already recognized as admin
    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            val isAdmin = userRepository.isUserAdmin(uid, userEmail)
            if (isAdmin) {
                isVerified = true
            }
        }
    }

    // Send Coins Form State
    var targetUidInput by remember { mutableStateOf("") }
    var coinAmountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var isSendingCoins by remember { mutableStateOf(false) }
    var deliverySuccessMsg by remember { mutableStateOf<String?>(null) }
    var deliveryErrorMsg by remember { mutableStateOf<String?>(null) }

    // Recent Transactions stream
    val transactions by userRepository.observeRecentTransactions().collectAsState(initial = emptyList())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E0736),
                        CandyBackground,
                        Color(0xFF38006B)
                    )
                )
            )
    ) {
        if (!isVerified) {
            // Password Verification Screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .testTag("admin_login_card"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E104D)),
                    elevation = CardDefaults.cardElevation(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Admin Verification",
                                color = CoinGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A148C))
                                .border(2.dp, CoinGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = CoinGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Please enter the administrative password to access coin management and game controls.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = null
                            },
                            label = { Text("Admin Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoinGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                focusedLabelColor = CoinGold,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Exact red error message "Wrong Password!"
                        if (passwordError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = passwordError ?: "",
                                color = Color(0xFFFF1744),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("admin_password_error")
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isVerifying = true
                                passwordError = null
                                // Admin password: "Tanvir71"
                                if (passwordInput == "Tanvir71") {
                                    coroutineScope.launch {
                                        // Register admin doc on server/database
                                        userRepository.registerAdminDoc(uid, userEmail)
                                        isVerified = true
                                        isVerifying = false
                                    }
                                } else {
                                    isVerifying = false
                                    // Exact red error message "Wrong Password!"
                                    passwordError = "Wrong Password!"
                                }
                            },
                            enabled = !isVerifying && passwordInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("verify_admin_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CandyPrimary)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "VERIFY ACCESS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Admin Dashboard Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C1654))
                            .testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = CoinGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin Panel",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = "Tanvir Candy Crush Management",
                            color = CandySecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.size(40.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Send Coins Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_coins_card"),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0F4A))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = CoinGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Send Coins to Any Player",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Player UID Input
                                OutlinedTextField(
                                    value = targetUidInput,
                                    onValueChange = {
                                        targetUidInput = it
                                        deliveryErrorMsg = null
                                        deliverySuccessMsg = null
                                    },
                                    label = { Text("Player UID") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = CoinGold)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("target_uid_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CoinGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = CoinGold,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Fill My UID ($uid)",
                                        color = CoinGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF381559))
                                            .border(1.dp, CoinGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                targetUidInput = uid
                                                deliveryErrorMsg = null
                                                deliverySuccessMsg = null
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Coin Amount Input
                                OutlinedTextField(
                                    value = coinAmountInput,
                                    onValueChange = {
                                        coinAmountInput = it
                                        deliveryErrorMsg = null
                                        deliverySuccessMsg = null
                                    },
                                    label = { Text("Coin Amount (e.g. 500)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = CoinGold)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("coin_amount_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CoinGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = CoinGold,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Optional Note Input
                                OutlinedTextField(
                                    value = noteInput,
                                    onValueChange = { noteInput = it },
                                    label = { Text("Optional Note (e.g. Tournament Reward)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("coin_note_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CoinGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = CoinGold,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )

                                // Success or Error Banner
                                if (deliverySuccessMsg != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFF00E676), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                            .testTag("coin_delivery_success")
                                    ) {
                                        Text(
                                            text = deliverySuccessMsg ?: "",
                                            color = Color(0xFF00E676),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (deliveryErrorMsg != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFF1744).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                            .testTag("coin_delivery_error")
                                    ) {
                                        Text(
                                            text = deliveryErrorMsg ?: "",
                                            color = Color(0xFFFF1744),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Green Checkmark Confirmation Button
                                Button(
                                    onClick = {
                                        val targetUid = targetUidInput.trim()
                                        val amount = coinAmountInput.trim().toLongOrNull() ?: 0L

                                        if (targetUid.isBlank()) {
                                            deliveryErrorMsg = "Please enter a valid player UID"
                                            return@Button
                                        }
                                        if (amount <= 0L) {
                                            deliveryErrorMsg = "Please enter a positive coin amount"
                                            return@Button
                                        }

                                        isSendingCoins = true
                                        deliveryErrorMsg = null
                                        deliverySuccessMsg = null

                                        coroutineScope.launch {
                                            val result = userRepository.grantCoinsAsAdmin(
                                                targetUid = targetUid,
                                                amount = amount,
                                                note = noteInput
                                            )
                                            isSendingCoins = false
                                            if (result.isSuccess) {
                                                deliverySuccessMsg = result.getOrNull()
                                                targetUidInput = ""
                                                coinAmountInput = ""
                                                noteInput = ""
                                            } else {
                                                deliveryErrorMsg = result.exceptionOrNull()?.message ?: "Failed to deliver coins"
                                            }
                                        }
                                    },
                                    enabled = !isSendingCoins,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("confirm_send_coins_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                ) {
                                    if (isSendingCoins) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Confirm",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CONFIRM & DELIVER COINS",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent Coin Transactions Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recent Coin Transactions",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${transactions.size} records",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Transaction Items
                    if (transactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF220A3B))
                            ) {
                                Text(
                                    text = "No recent coin transactions recorded yet.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(20.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(transactions) { tx ->
                            TransactionItemCard(tx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemCard(tx: CoinTransaction) {
    val dateStr = tx.timestamp?.toDate()?.let {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(it)
    } ?: "Just now"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${tx.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF220A3B).copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+${tx.amount} Coins",
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = dateStr,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Target UID: ${tx.targetUid}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "By Admin: ${tx.adminEmail}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )

            if (tx.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: \"${tx.note}\"",
                    color = CandySecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
