package com.example.cheapchomp.viewmodel


import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import com.example.cheapchomp.ui.state.LoginUiState
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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


    // google oauth
    sealed class LoginState {
        object Idle : LoginState() // Initial state
        object Loading : LoginState() // When a sign-in attempt is in progress
        data class Success(val email: String?) : LoginState() // On successful sign-in
        data class Error(val message: String) : LoginState() // On sign-in failure
    }


    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun startGoogleSignIn(
        oneTapClient: SignInClient,
        signInRequest: BeginSignInRequest,
        onSuccess: (IntentSender) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                onSuccess(result.pendingIntent.intentSender)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun handleSignInResult(oneTapClient: SignInClient, data: Intent?) {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                _loginState.value = LoginState.Error("No ID token found")
            }
        } catch (e: ApiException) {
            _loginState.value = LoginState.Error("Error retrieving sign-in credential")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    _loginState.value = LoginState.Success(user?.email)
                } else {
                    _loginState.value = LoginState.Error("Firebase sign-in failed")
                }
            }
    }


}
