package com.example.cheapchomp.ui.state

import com.example.cheapchomp.network.models.ProductPrice

sealed class KrogerProductUiState {
    object Initial : KrogerProductUiState()
    object Loading : KrogerProductUiState()
    data class Success(val products: List<ProductPrice>) : KrogerProductUiState()
    data class Error(val message: String) : KrogerProductUiState()
}