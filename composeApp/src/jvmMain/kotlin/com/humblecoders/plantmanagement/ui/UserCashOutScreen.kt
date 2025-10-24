package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.UserBalanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserCashOutScreen(
    userBalanceViewModel: UserBalanceViewModel,
    onBack: () -> Unit
) {
    var showCashOutDialog by remember { mutableStateOf(false) }

    val userBalance = userBalanceViewModel.userBalance
    val userCashOutTransactions = userBalanceViewModel.userCashOutTransactions

    LaunchedEffect(Unit) {
        userBalanceViewModel.loadSharedUserBalance()
        // User cash out transactions are already loaded via the listener in ViewModel
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF9CA3AF)
                    )
                }
                Text(
                    text = "My Cash Out",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error message
        userBalanceViewModel.errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFDC2626),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { userBalanceViewModel.clearError() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Current Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF374151),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Shared User Balance",
                    color = Color(0xFF9CA3AF),
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "₹${String.format("%.2f", userBalance?.currentBalance ?: 0.0)}",
                    color = Color(0xFFF9FAFB),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { showCashOutDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MoneyOff, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cash Out", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Transaction History Section
        Text(
            text = "Transaction History",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF9FAFB)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (userCashOutTransactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF374151),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No transactions yet",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userCashOutTransactions) { transaction ->
                    UserCashOutTransactionCard(transaction = transaction)
                }
            }
        }
    }

    // Cash Out Dialog
    if (showCashOutDialog) {
        UserCashOutDialog(
            currentBalance = userBalance?.currentBalance ?: 0.0,
            onDismiss = { showCashOutDialog = false },
            onCashOut = { amount, notes ->
                userBalanceViewModel.processUserCashOutTransaction(amount, notes)
                showCashOutDialog = false
            }
        )
    }
}


@Composable
fun UserCashOutDialog(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onCashOut: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = Color(0xFF374151),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Cash Out",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Available Balance: ₹${String.format("%.2f", currentBalance)}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF6B7280)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF6B7280)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280))
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull() ?: 0.0
                            if (amountValue > 0) {
                                onCashOut(amountValue, notes)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF4444))
                    ) {
                        Text("Cash Out", color = Color.White)
                    }
                }
            }
        }
    }
}
