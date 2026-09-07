package com.vodileats.feature.auth

import com.vodileats.core.domain.model.UserProfile
import com.vodileats.core.domain.model.UserRole

data class AuthState(
    val phone: String = "",
    val otp: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOtpSent: Boolean = false,
    val resendCountdown: Int = 60,
    val userProfile: UserProfile? = null
)

sealed interface AuthIntent {
    data class OnPhoneChanged(val phone: String) : AuthIntent
    data class OnOtpChanged(val otp: String) : AuthIntent
    data class OnFirstNameChanged(val name: String) : AuthIntent
    data class OnLastNameChanged(val name: String) : AuthIntent
    data class OnRoleSelected(val role: UserRole) : AuthIntent
    object SendOtp : AuthIntent
    object VerifyOtp : AuthIntent
    object ResendOtp : AuthIntent
    object ClearError : AuthIntent
}

sealed interface AuthEffect {
    data class ShowToast(val message: String) : AuthEffect
    data class NavigateToHome(val role: UserRole) : AuthEffect
}
