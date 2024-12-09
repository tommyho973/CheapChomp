package com.example.cheapchomp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.cheapchomp.ui.state.LoginUiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = LoginUiState.Success("Login successful :)")
                    _isLoggedIn.value = true
                } else {
                    _uiState.value = LoginUiState.Error("Login failed: ${task.exception?.message}")
                }
            }
    }
}
