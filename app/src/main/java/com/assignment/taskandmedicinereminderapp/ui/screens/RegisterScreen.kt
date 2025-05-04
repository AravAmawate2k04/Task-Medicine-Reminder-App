package com.assignment.taskandmedicinereminderapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Apple Watch-inspired dark theme colors
    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF1C1C1E)
    val accentColor = Color(0xFF0A84FF) // Blue in Apple's dark palette
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFAEAEB2)
    val errorColor = Color(0xFFFF453A) // Red in Apple's dark palette

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is com.assignment.taskandmedicinereminderapp.viewmodel.AuthState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            is com.assignment.taskandmedicinereminderapp.viewmodel.AuthState.Authenticated -> {
                isLoading = false
                onNavigateToDashboard()
            }
            is com.assignment.taskandmedicinereminderapp.viewmodel.AuthState.Error -> {
                isLoading = false
                errorMessage = (authState as com.assignment.taskandmedicinereminderapp.viewmodel.AuthState.Error).message
            }
            else -> {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = darkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(darkBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = secondaryTextColor) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = accentColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = secondaryTextColor
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = secondaryTextColor) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = accentColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = secondaryTextColor
                        )
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password", color = secondaryTextColor) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = accentColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = secondaryTextColor
                        )
                    )

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = errorColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        return@Button
                    }
                    viewModel.signUp(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White,
                    disabledContainerColor = accentColor.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign Up", modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = secondaryTextColor
                )
            ) {
                Text("Already have an account? Login")
            }
        }
    }
} 