package com.example.cheapchomp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.ui.state.GroceryListUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroceryListViewModel(
    private val databaseRepository: DatabaseRepository = DatabaseRepository(),
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow<GroceryListUiState>(GroceryListUiState.Empty)
    val uiState: StateFlow<GroceryListUiState> = _uiState.asStateFlow()

    init {
        loadGroceryList()
    }

    private fun loadGroceryList() {
        viewModelScope.launch {
            _uiState.value = GroceryListUiState.Loading
            try {
                // Check if user is authenticated
                if (auth.currentUser == null) {
                    _uiState.value = GroceryListUiState.Error("User not authenticated")
                    return@launch
                }

                databaseRepository.getGroceryList { listRef ->
                    Firebase.firestore.collection("items")
                        .whereEqualTo("grocery_list", listRef)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                _uiState.value = GroceryListUiState.Error(e.message ?: "Unknown error")
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val items = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        DatabaseRepository.GroceryItem(
                                            id = doc.id,
                                            name = doc.getString("name") ?: "",
                                            price = doc.getString("price") ?: "0.00",
                                            quantity = doc.getLong("quantity")?.toInt() ?: 0,
                                            storeId = doc.getString("store_id") ?: ""
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                _uiState.value = if (items.isEmpty()) {
                                    GroceryListUiState.Empty
                                } else {
                                    GroceryListUiState.Success(items)
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                _uiState.value = GroceryListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateItemQuantity(itemId: String, newQuantity: Int) {
        viewModelScope.launch {
            try {
                databaseRepository.getGroceryList { listRef ->
                    Firebase.firestore.collection("items")
                        .document(itemId)
                        .update("quantity", newQuantity)
                        .addOnFailureListener { e ->
                            _uiState.value = GroceryListUiState.Error(e.message ?: "Failed to update quantity")
                        }
                }
            } catch (e: Exception) {
                _uiState.value = GroceryListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                Firebase.firestore.collection("items")
                    .document(itemId)
                    .delete()
                    .addOnFailureListener { e ->
                        _uiState.value = GroceryListUiState.Error(e.message ?: "Failed to delete item")
                    }
            } catch (e: Exception) {
                _uiState.value = GroceryListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}