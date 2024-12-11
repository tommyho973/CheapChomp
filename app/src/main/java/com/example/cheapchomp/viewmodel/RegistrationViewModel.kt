package com.example.cheapchomp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.ui.state.RegistrationUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// RegistrationViewModel.kt
class RegistrationViewModel(
    private val auth: FirebaseAuth,  // This needs to be FirebaseAuth, not just Auth
    private val db: FirebaseFirestore = Firebase.firestore,
    private val databaseRepository: DatabaseRepository = DatabaseRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Initial)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun register(email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _uiState.value = RegistrationUiState.Error("Passwords do not match!")
            return
        }

        _uiState.value = RegistrationUiState.Loading

        auth.createUserWithEmailAndPassword(email, password)  // Now this should work
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    createUserDocument(email)
                } else {
                    _uiState.value = RegistrationUiState.Error("Error creating account: ${task.exception?.message}")
                }
            }
    }

    private fun createUserDocument(email: String) {
        val user = hashMapOf("email" to email)

        db.collection("users")
            .add(user)
            .addOnSuccessListener {
                createGroceryList()
            }
            .addOnFailureListener { e ->
                _uiState.value = RegistrationUiState.Error("Error creating user document: ${e.message}")
            }
    }

    private fun createGroceryList() {
        databaseRepository.getUserRef { userRef ->
            val groceryList = hashMapOf(
                "favorited" to false,
                "user" to userRef
            )

            db.collection("grocery_list")
                .add(groceryList)
                .addOnSuccessListener {
                    _uiState.value = RegistrationUiState.Success("Account created successfully!")
                }
                .addOnFailureListener { e ->
                    _uiState.value = RegistrationUiState.Error("Error creating grocery list: ${e.message}")
                }
        }
    }
}