package com.example.cheapchomp

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.Base64
//import com.example.cheapchomp.BuildConfig

class KrogerApiService {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val clientId = BuildConfig.CLIENT_ID
    private val clientSecret = BuildConfig.CLIENT_SECRET

    // Moshi to parse and deserialize JSON
    private val tokenAdapter = moshi.adapter(TokenResponse::class.java)
    private val locationAdapter = moshi.adapter(LocationResponse::class.java)
    private val productAdapter = moshi.adapter(ProductResponse::class.java)

    // Method to get Kroger access token
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val credentials = "$clientId:$clientSecret"
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

        val requestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("scope", "product.compact")
            .build()

        val request = Request.Builder()
            .url("https://api.kroger.com/v1/connect/oauth2/token")
            .post(requestBody)
            .addHeader("Authorization", "Basic $encodedCredentials")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val tokenResponse = responseBody?.let { tokenAdapter.fromJson(it) }
                return@withContext tokenResponse?.access_token
            }
            null
        } catch (e: IOException) {
            Log.e("KrogerAPI", "Error getting access token", e)
            null
        }
    }

    // Method to find nearest Kroger store
    suspend fun findNearestStore(accessToken: String, latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.kroger.com/v1/locations?filter.lat.near=$latitude&filter.lon.near=$longitude&filter.limit=1")
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val locationResponse = responseBody?.let { locationAdapter.fromJson(it) }
                locationResponse?.data?.firstOrNull()?.locationId
            } else {
                Log.e("KrogerAPI", "Failed to find nearest store")
                null
            }
        } catch (e: IOException) {
            Log.e("KrogerAPI", "Error finding nearest store", e)
            null
        }
    }

    // Method to get product price
    suspend fun getProductPrice(accessToken: String, locationId: String, productName: String): ProductPrice? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.kroger.com/v1/products?filter.term=$productName&filter.locationId=$locationId&filter.limit=1")
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val productResponse = responseBody?.let { productAdapter.fromJson(it) }
                val product = productResponse?.data?.firstOrNull()

                val price = product?.items?.firstOrNull()?.price?.regular ?: product?.items?.firstOrNull()?.price?.promo

                ProductPrice(
                    name = product?.description ?: productName,
                    price = price ?: "N/A"
                )
            } else {
                Log.e("KrogerAPI", "Failed to find product")
                null
            }
        } catch (e: IOException) {
            Log.e("KrogerAPI", "Error finding product", e)
            null
        }
    }
    // Method modified getProduct func for multiple products
    suspend fun getProductPrices(
        accessToken: String,
        locationId: String,
        productName: String
    ): List<ProductPrice> = withContext(Dispatchers.IO) {
        // Adjust the URL to fetch more products, by removing the limit or increasing the number
        val request = Request.Builder()
            .url("https://api.kroger.com/v1/products?filter.term=$productName&filter.locationId=$locationId&filter.limit=50")  // Increase limit for multiple results
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val productResponse = responseBody?.let { productAdapter.fromJson(it) }

                productResponse?.data?.mapNotNull { product ->
                    val price = product.items?.firstOrNull()?.price?.regular
                        ?: product.items?.firstOrNull()?.price?.promo

                    ProductPrice(
                        name = product.description ?: productName,
                        price = price ?: "N/A"
                    )
                } ?: emptyList() // In case the list is null, return an empty list
            } else {
                Log.e("KrogerAPI", "Failed to find products")
                emptyList<ProductPrice>()
            }
        } catch (e: IOException) {
            Log.e("KrogerAPI", "Error finding products", e)
            emptyList<ProductPrice>()
        }
    }
}

data class TokenResponse(val access_token: String)

data class LocationResponse(val data: List<LocationData>?)

data class LocationData(val locationId: String)

data class ProductPrice(val name: String, val price: String)

data class ProductResponse(val data: List<ProductData>?)

data class ProductData(val description: String, val items: List<ProductItem>?)

data class ProductItem(val price: ProductItemPrice?)

data class ProductItemPrice(val regular: String?, val promo: String?)
