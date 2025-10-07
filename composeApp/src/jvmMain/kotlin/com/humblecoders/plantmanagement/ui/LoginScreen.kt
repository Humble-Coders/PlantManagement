package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit
) {
    val authState = authViewModel.authState

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Clear error when user starts typing
    LaunchedEffect(email, password) {
        if (authState.error != null) {
            authViewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(450.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF1F2937),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Plant Management System",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF06B6D4)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign In",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true,
                    enabled = !authState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !authState.isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error message
                if (authState.error != null) {
                    Text(
                        text = authState.error,
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Sign in button
                Button(
                    onClick = { authViewModel.signIn(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF06B6D4),
                        contentColor = Color(0xFF111827)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !authState.isLoading
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF111827),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign up navigation
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account?",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = onNavigateToSignUp,
                        enabled = !authState.isLoading
                    ) {
                        Text(
                            text = "Sign Up",
                            color = Color(0xFF06B6D4),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}