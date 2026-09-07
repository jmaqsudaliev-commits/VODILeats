package com.vodileats.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.usecase.SendOtpUseCase
import com.vodileats.core.domain.usecase.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AuthEffect>()
    val effect: SharedFlow<AuthEffect> = _effect.asSharedFlow()

    private var countdownJob: Job? = null

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.OnPhoneChanged -> {
                _state.update { it.copy(phone = intent.phone, error = null) }
            }
            is AuthIntent.OnOtpChanged -> {
                if (intent.otp.length <= 6) {
                    _state.update { it.copy(otp = intent.otp, error = null) }
                    if (intent.otp.length == 6) {
                        onIntent(AuthIntent.VerifyOtp)
                    }
                }
            }
            is AuthIntent.OnFirstNameChanged -> {
                _state.update { it.copy(firstName = intent.name) }
            }
            is AuthIntent.OnLastNameChanged -> {
                _state.update { it.copy(lastName = intent.name) }
            }
            is AuthIntent.OnRoleSelected -> {
                _state.update { it.copy(role = intent.role) }
            }
            AuthIntent.SendOtp -> sendOtp()
            AuthIntent.VerifyOtp -> verifyOtp()
            AuthIntent.ResendOtp -> {
                if (_state.value.resendCountdown == 0) {
                    sendOtp()
                }
            }
            AuthIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun sendOtp() {
        val phone = _state.value.phone.trim()
        if (phone.length < 9) {
            _state.update { it.copy(error = "Telefon raqamini to'liq kiriting") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
            when (val result = sendOtpUseCase(formattedPhone)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isOtpSent = true,
                            resendCountdown = 60
                        )
                    }
                    startTimer()
                    _effect.emit(AuthEffect.ShowToast(result.data ?: "Kod yuborildi"))
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun verifyOtp() {
        val st = _state.value
        if (st.otp.length != 6) {
            _state.update { it.copy(error = "6 xonali SMS kodni kiriting") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val formattedPhone = if (st.phone.startsWith("+")) st.phone else "+${st.phone}"
            when (val result = verifyOtpUseCase(
                phone = formattedPhone,
                otp = st.otp,
                firstName = st.firstName.ifBlank { null },
                lastName = st.lastName.ifBlank { null },
                role = st.role.name
            )) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, userProfile = result.data) }
                    countdownJob?.cancel()
                    result.data?.let {
                        _effect.emit(AuthEffect.NavigateToHome(it.role))
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun startTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.resendCountdown > 0) {
                delay(1000)
                _state.update { it.copy(resendCountdown = it.resendCountdown - 1) }
            }
        }
    }
}
