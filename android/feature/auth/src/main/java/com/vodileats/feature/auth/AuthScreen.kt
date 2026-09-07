package com.vodileats.feature.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vodileats.core.domain.model.UserRole
import com.vodileats.core.ui.component.VodilButton
import com.vodileats.core.ui.component.VodilTextField
import com.vodileats.core.ui.theme.Gray500
import com.vodileats.core.ui.theme.OrangePrimary

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (UserRole) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToHome -> onAuthSuccess(effect.role)
                is AuthEffect.ShowToast -> {
                    // toast or snackbar
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo / Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "VODIL EATS",
                    style = MaterialTheme.typography.displayLarge,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = if (!state.isOtpSent) "Xush kelibsiz! Raqamingizni kiriting" else "SMS orqali kelgan kodni kiriting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                AnimatedContent(
                    targetState = state.isOtpSent,
                    label = "auth_step"
                ) { isOtp ->
                    if (!isOtp) {
                        // Phone input step
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            VodilTextField(
                                value = state.phone,
                                onValueChange = { viewModel.onIntent(AuthIntent.OnPhoneChanged(it)) },
                                label = "Telefon raqami",
                                placeholder = "+998 90 123 45 67",
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = state.error != null,
                                errorMessage = state.error
                            )

                            // Role selection chips (for demo/multi-app role testing)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                RoleChip("Mijoz", state.role == UserRole.CUSTOMER) {
                                    viewModel.onIntent(AuthIntent.OnRoleSelected(UserRole.CUSTOMER))
                                }
                                RoleChip("Kuryer", state.role == UserRole.COURIER) {
                                    viewModel.onIntent(AuthIntent.OnRoleSelected(UserRole.COURIER))
                                }
                                RoleChip("Restoran", state.role == UserRole.RESTAURANT) {
                                    viewModel.onIntent(AuthIntent.OnRoleSelected(UserRole.RESTAURANT))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            VodilButton(
                                text = "Kodni olish",
                                onClick = { viewModel.onIntent(AuthIntent.SendOtp) },
                                isLoading = state.isLoading
                            )
                        }
                    } else {
                        // OTP verification step
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VodilTextField(
                                value = state.otp,
                                onValueChange = { viewModel.onIntent(AuthIntent.OnOtpChanged(it)) },
                                label = "SMS Tasdiqlash Kodi",
                                placeholder = "000000",
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                isError = state.error != null,
                                errorMessage = state.error
                            )

                            if (state.resendCountdown > 0) {
                                Text(
                                    text = "Qayta yuborish: ${state.resendCountdown}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray500
                                )
                            } else {
                                Text(
                                    text = "Kodni qayta yuborish",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.onIntent(AuthIntent.ResendOtp)
                                    }
                                )
                            }

                            VodilButton(
                                text = "Tasdiqlash",
                                onClick = { viewModel.onIntent(AuthIntent.VerifyOtp) },
                                isLoading = state.isLoading
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
