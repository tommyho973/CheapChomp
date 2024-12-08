package com.example.cheapchomp.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cheapchomp.network.models.ProductPrice
import com.example.cheapchomp.repository.DatabaseRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GroceryListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    auth: FirebaseAuth
) {
    data class Item(
        val item_id: Int = 0,
        val store_id: String = "",
        val name: String = "",
        val price: String = "",
        val quantity: Int = 0,
        val favorited: Boolean = false,

        ) {
        constructor() : this(0, "", "", "", 0, false)
    }
    val db = Firebase.firestore
    val itemsState = remember { mutableStateOf<List<Item>>(emptyList()) }
    val items by itemsState // Delegate to itemsState.value
    val databaseRepository = DatabaseRepository()

    databaseRepository.getGroceryList { listRef ->
        db.collection("items")
            .whereEqualTo("grocery_list", listRef)
            .get()
            .addOnSuccessListener { querySnapshot ->
                itemsState.value =
                    querySnapshot.toObjects(Item::class.java) // Update itemsState.value
            }
            .addOnFailureListener { exception ->
                Log.d("DATABASE", "get failed with ", exception)
            }
    }
    var totalPrice = 0f
    for (item in items) {
        val priceString = item.price.replace("[^\\d.]".toRegex(), "")
        val quantity = item.quantity
        val priceFloat = priceString.toFloatOrNull() ?: 0f
        totalPrice += (priceFloat * quantity)
    }
    val totalPriceStr = String.format("%.2f", totalPrice)
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Total Price: $${totalPriceStr}", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                        ,shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = item.name)
                                    Text(text = "$${item.price}")
                                    Text(text = "${item.quantity}")
                                }
                                IconButton(onClick = {
                                    val productPrice = ProductPrice(item.name, item.price, null)
                                    databaseRepository.deleteFromDatabase(
                                        productPrice,
                                        item.store_id
                                    ) { itemsState.value = itemsState.value.filter { it != item } }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }

        }
        BottomNavigation(backgroundColor = Color(0xFF56AE57),elevation = 8.dp) {
            BottomNavigationItem(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                },
                label = { Text("Back") },
                selected = false,
                onClick = { navController.navigateUp() }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Search, contentDescription = "Product Search") },
                label = { Text("Product Search") },
                selected = false,
                onClick = { navController.navigate("GoogleMapScreen") }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Grocery List") },
                label = { Text("Grocery List") },
                selected = true,
                onClick = { /* current screen! do nothing :> */  }
            )
        }
    }

}