package com.example.cheapchomp.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.cheapchomp.network.KrogerApiService
import com.example.cheapchomp.network.models.ProductPrice

class KrogerRepository @RequiresApi(Build.VERSION_CODES.O) constructor() {
    @RequiresApi(Build.VERSION_CODES.O)
    private val apiService = KrogerApiService.create()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAccessToken(): Result<String> {
        return try {
            val response = apiService.getAccessToken(
                authorization = KrogerApiService.getBasicAuthHeader()
            )

            if (response.isSuccessful) {
                response.body()?.accessToken?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Token response was null"))
            } else {
                Result.failure(Exception("Failed to get access token: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun findNearestStore(accessToken: String, latitude: Double, longitude: Double): Result<String> {
        return try {
            val response = apiService.findNearestStore(
                authorization = "Bearer $accessToken",
                latitude = latitude,
                longitude = longitude
            )

            if (response.isSuccessful) {
                val locationId = response.body()?.data?.firstOrNull()?.locationId
                if (locationId != null) {
                    Result.success(locationId)
                } else {
                    Result.failure(Exception("No store found"))
                }
            } else {
                Result.failure(Exception("Failed to find store: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProducts(accessToken: String, storeId: String, query: String): Result<List<ProductPrice>> {
        return try {
            val response = apiService.getProducts(
                authorization = "Bearer $accessToken",
                term = query,
                locationId = storeId
            )

            if (response.isSuccessful) {
                val products = response.body()?.data?.mapNotNull { product ->
                    val price = product.items?.firstOrNull()?.price?.regular
                        ?: product.items?.firstOrNull()?.price?.promo
                    val imageUrl = product.images?.firstOrNull()?.sizes?.firstOrNull { it.size == "large" }?.url

                    if (price != null) {
                        ProductPrice(
                            name = product.description ?: query,
                            price = price,
                            imageUrl = imageUrl
                        )
                    } else null
                } ?: emptyList()

                Result.success(products)
            } else {
                Result.failure(Exception("Failed to get products: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
