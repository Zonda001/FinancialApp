package com.example.financegame.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.financegame.ui.theme.*

val avatarsList = listOf(
    "👨", "👩", "🧑", "👦", "👧",
    "🧔", "👴", "👵", "🦸", "🦹",
    "🧙", "🧚", "🧛", "🧜", "🧝",
    "🐱", "🐶", "🐼", "🐨", "🦁",
    "🦊", "🐯", "🐸", "🐷", "🐮",
    "🤖", "👽", "👾", "🎭", "🎪"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onRegistrationComplete: (nickname: String, avatar: String, password: String, useBiometric: Boolean) -> Unit,
    onGuestMode: () -> Unit,
    biometricAvailable: Boolean
) {
    var nickname by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("👨") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var useBiometric by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) } // 0 = привітання, 1 = нік, 2 = аватар, 3 = пароль

    val isNicknameValid = nickname.isNotBlank() && nickname.length >= 3
    val isPasswordValid = password.length >= 4
    val passwordsMatch = password == confirmPassword && confirmPassword.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentStep) {
                // 0️⃣ Привітання
                0 -> {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = TextLight
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Finance Game",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Керуй фінансами як у грі!",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextLight.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = { currentStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextLight,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Почати", fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onGuestMode, modifier = Modifier.fillMaxWidth()) {
                        Text("Продовжити як гість", color = TextLight.copy(alpha = 0.8f))
                    }
                }

                // 1️⃣ Нікнейм
                1 -> {
                    Text("Як тебе звати?", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Це ім'я бачитимеш тільки ти", color = TextLight.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { if (it.length <= 20) nickname = it },
                        label = { Text("Нікнейм", color = TextLight.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = TextLight) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = TextLight,
                            unfocusedBorderColor = TextLight.copy(alpha = 0.5f),
                            cursorColor = TextLight
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${nickname.length}/20 символів (мінімум 3)", color = TextLight.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { currentStep = 2 },
                        enabled = isNicknameValid,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextLight)
                    ) { Text("Далі", color = MaterialTheme.colorScheme.primary) }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { currentStep = 0 }) { Text("Назад", color = TextLight.copy(alpha = 0.8f)) }
                }

                // 2️⃣ Аватар
                2 -> {
                    Text("Обери аватар", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(TextLight.copy(alpha = 0.2f))
                            .border(4.dp, TextLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedAvatar, style = MaterialTheme.typography.displayLarge)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        colors = CardDefaults.cardColors(containerColor = TextLight.copy(alpha = 0.15f))
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(avatarsList) { avatar ->
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (avatar == selectedAvatar)
                                                TextLight.copy(alpha = 0.3f)
                                            else TextLight.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            width = if (avatar == selectedAvatar) 2.dp else 0.dp,
                                            color = TextLight,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatar = avatar },
                                    contentAlignment = Alignment.Center
                                ) { Text(avatar) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { currentStep = 3 },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextLight)
                    ) { Text("Далі", color = MaterialTheme.colorScheme.primary) }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { currentStep = 1 }) { Text("Назад", color = TextLight.copy(alpha = 0.8f)) }
                }

                // 3️⃣ Пароль + біометрія
                3 -> {
                    Text("Захисти профіль", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Створи пароль або використовуй біометрію", color = TextLight.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль", color = TextLight.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextLight) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = TextLight
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = TextLight,
                            unfocusedBorderColor = TextLight.copy(alpha = 0.5f),
                            cursorColor = TextLight
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Підтвердження", color = TextLight.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextLight) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = TextLight
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = if (passwordsMatch && confirmPassword.isNotEmpty())
                                QuestCompletedColor else TextLight,
                            unfocusedBorderColor = TextLight.copy(alpha = 0.5f),
                            cursorColor = TextLight
                        ),
                        singleLine = true
                    )

                    AnimatedVisibility(visible = confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Паролі не збігаються", color = AccentRed)
                        }
                    }

                    if (biometricAvailable) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = useBiometric, onCheckedChange = { useBiometric = it })
                            Text("Увімкнути біометрію", color = TextLight)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            onRegistrationComplete(nickname, selectedAvatar, password, useBiometric)
                        },
                        enabled = isPasswordValid && passwordsMatch,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextLight)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Завершити реєстрацію", color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { currentStep = 2 }) { Text("Назад", color = TextLight.copy(alpha = 0.8f)) }
                }
            }
        }

        // Прогрес-індикатор
        if (currentStep > 0) {
            LinearProgressIndicator(
                progress = currentStep / 3f,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = TextLight,
                trackColor = TextLight.copy(alpha = 0.3f)
            )
        }
    }
}
