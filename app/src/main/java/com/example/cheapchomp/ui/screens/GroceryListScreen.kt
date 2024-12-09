package com.example.cheapchomp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.ui.state.GroceryListUiState
import com.example.cheapchomp.viewmodel.GroceryListViewModel
import com.example.cheapchomp.viewmodel.GroceryListViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun GroceryListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    auth: FirebaseAuth
) {
    val viewModel: GroceryListViewModel = viewModel(
        factory = GroceryListViewModelFactory(auth)
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Check if user is logged in
    LaunchedEffect(auth) {
        if (auth.currentUser == null) {
            navController.navigate("LoginScreen") {
                popUpTo("GroceryListScreen") { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Text(
                "Grocery List",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (uiState) {
                is GroceryListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is GroceryListUiState.Success -> {
                    val items = (uiState as GroceryListUiState.Success).groceryItems
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { item ->
                            GroceryListItem(
                                item = item,
                                onQuantityChange = { newQuantity ->
                                    viewModel.updateItemQuantity(item.id, newQuantity)
                                },
                                onDelete = {
                                    viewModel.deleteItem(item.id)
                                }
                            )
                        }
                    }
                }

                is GroceryListUiState.Error -> {
                    Text(
                        text = (uiState as GroceryListUiState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                GroceryListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Your grocery list is empty",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        BottomNavigation(
            backgroundColor = Color(0xFF56AE57),
            elevation = 8.dp
        ) {
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Back") },
                selected = false,
                onClick = { navController.navigateUp() }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Search, contentDescription = "Product Search") },
                label = { Text("Search") },
                selected = false,
                onClick = {
                    val locationService = LocationService(context)  // Use the context from above
                    coroutineScope.launch {
                        try {
                            val location = locationService.getCurrentLocation()
                            navController.navigate(
                                "KrogerProductScreen/${location.latitude}/${location.longitude}"
                            )
                        } catch (e: Exception) {
                            // If location fails, use default San Francisco coordinates
                            navController.navigate("KrogerProductScreen/37.7749/-122.4194")
                        }
                    }
                }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Grocery List") },
                label = { Text("Grocery List") },
                selected = true,
                onClick = { /* Current screen */ }
            )
        }
    }
}

@Composable
fun GroceryListItem(
    item: DatabaseRepository.GroceryItem,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$${item.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF56AE57)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Got item",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}