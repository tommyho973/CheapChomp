package com.example.cheapchomp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.repository.OfflineDatabase
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
    private val auth: FirebaseAuth,
    private val room_db: OfflineDatabase
) : ViewModel() {
    private val _uiState = MutableStateFlow<GroceryListUiState>(GroceryListUiState.Empty)
    val uiState: StateFlow<GroceryListUiState> = _uiState.asStateFlow()

    init {
        loadGroceryList()
    }
    // Cache structure to hold deleted items temporarily
    private data class DeletedItemCache(
        val id: String,
        val name: String,
        val price: String,
        val quantity: Int,
        val storeId: String,
        val isQuantityUpdate: Boolean = false
    )

    fun cacheItem(item: DatabaseRepository.GroceryItem, isQuantityUpdate: Boolean = true) {
        recentlyDeletedItem = DeletedItemCache(
            id = item.id,
            name = item.name,
            price = item.price,
            quantity = item.quantity,
            storeId = item.storeId,
            isQuantityUpdate = isQuantityUpdate
        )
    }

    private var recentlyDeletedItem: DeletedItemCache? = null

    fun deleteItem(item: DatabaseRepository.GroceryItem) {
        // Cache first
        cacheItem(item, isQuantityUpdate = false)

        // Then delete
        viewModelScope.launch {
            try {
                Firebase.firestore.collection("items")
                    .document(item.id)
                    .delete()
                    .addOnFailureListener { e ->
                        _uiState.value = GroceryListUiState.Error(e.message ?: "Failed to delete item")
                    }
                databaseRepository.removeFromOfflineGroceryList(item.name, room_db)
            } catch (e: Exception) {
                _uiState.value = GroceryListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Function to restore the recently deleted item
    fun restoreRecentlyDeletedItem() {
        val itemToRestore = recentlyDeletedItem ?: return

        viewModelScope.launch {
            try {
                if (itemToRestore.isQuantityUpdate) {
                    // For quantity changes, update the existing item
                    updateItemQuantity(itemToRestore.id, itemToRestore.quantity)
                } else {
                    // For full deletions, create new item
                    databaseRepository.getGroceryList { listRef ->
                        Firebase.firestore.collection("items")
                            .add(hashMapOf(
                                "name" to itemToRestore.name,
                                "price" to itemToRestore.price,
                                "quantity" to itemToRestore.quantity,
                                "store_id" to itemToRestore.storeId,
                                "grocery_list" to listRef
                            ))
                    }
                }
                recentlyDeletedItem = null
            } catch (e: Exception) {
                _uiState.value = GroceryListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadGroceryList() {
        viewModelScope.launch {
            _uiState.value = GroceryListUiState.Loading
            try {
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
                databaseRepository.getGroceryList {
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

    // room database method
    fun displayCachedProducts() {
        viewModelScope.launch {
            databaseRepository.displayOfflineGroceryList(room_db)
                .onSuccess { products ->
                    if (products.isEmpty()) {
                        _uiState.value = GroceryListUiState.Error("Your grocery list is empty")
                    } else {
                        _uiState.value = GroceryListUiState.Success(products)
                    }
                }
                .onFailure { exception ->
                    // Handle the exception, e.g., log it or update the UI state with an error
                    Log.e("displayOfflineGroceryList", "Error fetching grocery list", exception)
                    _uiState.value = GroceryListUiState.Error("Error fetching grocery list")
                }
        }
    }

    fun checkItem(item: DatabaseRepository.GroceryItem) {
        val price = item.price.toFloatOrNull() ?: 0f
        val formattedPrice = String.format("%.2f", price)
        databaseRepository.addToExpenses(formattedPrice.toFloat())
        deleteItem(item)
    }

}
