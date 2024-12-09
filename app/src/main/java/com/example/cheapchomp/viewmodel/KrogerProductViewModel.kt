package com.example.cheapchomp.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.cheapchomp.repository.KrogerRepository
import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.network.models.ProductPrice
import com.example.cheapchomp.ui.state.KrogerProductUiState

class KrogerProductViewModel(
    private val krogerRepository: KrogerRepository,
    private val databaseRepository: DatabaseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<KrogerProductUiState>(KrogerProductUiState.Initial)
    val uiState: StateFlow<KrogerProductUiState> = _uiState.asStateFlow()

    private val _nearestStoreId = MutableStateFlow("")
    val nearestStoreId: StateFlow<String> = _nearestStoreId.asStateFlow()

    private var accessToken: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun searchProducts(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = KrogerProductUiState.Loading

            try {
                // Get or refresh access token if needed
                if (accessToken == null) {
                    krogerRepository.getAccessToken()
                        .onSuccess { token -> accessToken = token }
                        .onFailure { throw it }
                }

                // Use the token and store ID to search products
                accessToken?.let { token ->
                    if (_nearestStoreId.value.isNotEmpty()) {
                        krogerRepository.searchProducts(token, _nearestStoreId.value, query)
                            .onSuccess { products ->
                                _uiState.value = if (products.isEmpty()) {
                                    KrogerProductUiState.Error("No products found")
                                } else {
                                    KrogerProductUiState.Success(products)
                                }
                            }
                            .onFailure { throw it }
                    } else {
                        _uiState.value = KrogerProductUiState.Error("Store not initialized")
                    }
                } ?: throw Exception("Access token not available")
            } catch (e: Exception) {
                _uiState.value = KrogerProductUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initializeStore(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                krogerRepository.getAccessToken()
                    .onSuccess { token ->
                        accessToken = token
                        krogerRepository.findNearestStore(token, latitude, longitude)
                            .onSuccess { storeId ->
                                _nearestStoreId.value = storeId
                            }
                            .onFailure { e ->
                                _nearestStoreId.value = "70400357" // Fallback store ID
                                _uiState.value = KrogerProductUiState.Error(
                                    "Using fallback store: ${e.message}"
                                )
                            }
                    }
                    .onFailure { e ->
                        _uiState.value = KrogerProductUiState.Error(
                            "Failed to get access token: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = KrogerProductUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Add methods for database operations
    @RequiresApi(Build.VERSION_CODES.O)
    fun addToDatabase(product: ProductPrice, quantity: Int) {
        databaseRepository.addToDatabase(
            product = product,
            storeId = _nearestStoreId.value,
            quantity = quantity
        )
    }

    fun deleteFromDatabase(product: ProductPrice, storeId: String, onSuccess: () -> Unit) {
        databaseRepository.deleteFromDatabase(
            product = product,
            storeId = storeId,
            onSuccess = onSuccess
        )
    }
}