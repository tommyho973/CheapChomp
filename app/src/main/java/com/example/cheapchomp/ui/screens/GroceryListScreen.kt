package com.example.cheapchomp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cheapchomp.network.LocationService
import com.example.cheapchomp.repository.DatabaseRepository
import com.example.cheapchomp.repository.OfflineDatabase
import com.example.cheapchomp.repository.SyncWorker
import com.example.cheapchomp.ui.state.GroceryListUiState
import com.example.cheapchomp.viewmodel.GroceryListViewModel
import com.example.cheapchomp.viewmodel.GroceryListViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun GroceryListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    auth: FirebaseAuth,
    room_db: OfflineDatabase
) {
    val viewModel: GroceryListViewModel = viewModel(
        factory = GroceryListViewModelFactory(auth, room_db = room_db)
    )
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                Button(
                    onClick = { viewModel.displayCachedProducts() },
                ) { Text("Offline Grocery List") }
                Button(
                    onClick = {
                        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                            )
                            .build()

                        WorkManager.getInstance(context).enqueue(syncWorkRequest)

                    },
                ) { Text("Sync Online List to Offline") }

                when (uiState) {
                    is GroceryListUiState.Success -> {
                        val items = (uiState as GroceryListUiState.Success).groceryItems

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { item ->
                                GroceryListItem(
                                    item = item,
                                    onQuantityChange = { newQuantity ->
                                        viewModel.updateItemQuantity(item.id, newQuantity)
                                    },
                                    onDelete = {
                                        viewModel.deleteItem(item)
                                    },
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState,
                                    scope = scope
                                )
                            }
                        }
                    }

                    is GroceryListUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
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
                    label = { Text("Search") },
                    selected = false,
                    onClick = {
                        val locationService = LocationService(context)
                        scope.launch {
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
}

@Composable
fun GroceryListItem(
    item: DatabaseRepository.GroceryItem,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    viewModel: GroceryListViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showQuantityDialog by remember { mutableStateOf(false) }
    var removeQuantity by remember { mutableStateOf("1") }
    val swipeThreshold = -150f

    // Helper function for showing snackbar
    fun showUndoSnackbar(message: String) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreRecentlyDeletedItem()
            }
        }
    }

    if (showQuantityDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuantityDialog = false
                showMenu = false
                removeQuantity = "1"
            },
            title = { Text("Remove Multiple Items") },
            text = {
                Column {
                    Text("Enter quantity to remove (max ${item.quantity}):")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = removeQuantity,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || (newValue.toIntOrNull() != null &&
                                        newValue.toInt() <= item.quantity && newValue.toInt() > 0)
                            ) {
                                removeQuantity = newValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        removeQuantity.toIntOrNull()?.let { quantity ->
                            if (quantity > 0 && quantity <= item.quantity) {
                                viewModel.cacheItem(item)  // Just cache, don't delete
                                onQuantityChange(item.quantity - quantity)
                                showUndoSnackbar("Removed $quantity ${if (quantity == 1) "item" else "items"}")
                            }
                        }
                        showQuantityDialog = false
                        showMenu = false
                        removeQuantity = "1"
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showQuantityDialog = false
                        showMenu = false
                        removeQuantity = "1"
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX <= swipeThreshold) {
                            viewModel.deleteItem(item)  // Cache the item first
                            onDelete()
                            showUndoSnackbar("Item deleted")
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(swipeThreshold, 0f)
                    }
                )
            }
    ) {
        // Delete background
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Red.copy(alpha = 0.8f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Item",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .scale(1.2f),
                tint = Color.White
            )
        }

        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
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

                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                if (item.quantity > 1) {
                                    viewModel.cacheItem(item)  // Just cache, don't delete
                                    onQuantityChange(item.quantity - 1)
                                    showUndoSnackbar("Removed 1 item")
                                }
                                showMenu = false
                            }
                        ) {
                            Text("Remove 1")
                        }
                        DropdownMenuItem(
                            onClick = {
                                showQuantityDialog = true
                            }
                        ) {
                            Text("Remove multiple")
                        }
                        DropdownMenuItem(
                            onClick = {
                                viewModel.deleteItem(item)  // Cache the item first
                                onDelete()
                                showMenu = false
                                showUndoSnackbar("Item deleted")
                            }
                        ) {
                            Text("Remove item")
                        }
                    }
                }
            }
        }
    }
}