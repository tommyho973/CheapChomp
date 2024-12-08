package com.example.cheapchomp.network

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.cheapchomp.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.Base64
import java.util.concurrent.TimeUnit
import com.example.cheapchomp.network.models.TokenResponse
import com.example.cheapchomp.network.models.LocationResponse
import com.example.cheapchomp.network.models.ProductResponse

interface KrogerApiService {
    @FormUrlEncoded
    @POST("v1/connect/oauth2/token")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "product.compact"
    ): Response<TokenResponse>

    @GET("v1/locations")
    suspend fun findNearestStore(
        @Header("Authorization") authorization: String,
        @Query("filter.lat.near") latitude: Double,
        @Query("filter.lon.near") longitude: Double,
        @Query("filter.limit") limit: Int = 1
    ): Response<LocationResponse>

    @GET("v1/products")
    suspend fun getProducts(
        @Header("Authorization") authorization: String,
        @Query("filter.term") term: String,
        @Query("filter.locationId") locationId: String,
        @Query("filter.limit") limit: Int = 50
    ): Response<ProductResponse>

    companion object {
        private const val BASE_URL = "https://api.kroger.com/"

        @RequiresApi(Build.VERSION_CODES.O)
        fun create(): KrogerApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(KrogerApiService::class.java)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getBasicAuthHeader(): String {
            val credentials = "${BuildConfig.CLIENT_ID}:${BuildConfig.CLIENT_SECRET}"
            return "Basic ${Base64.getEncoder().encodeToString(credentials.toByteArray())}"
        }
    }
}