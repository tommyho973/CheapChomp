package com.example.cheapchomp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigation
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cheapchomp.ui.theme.CheapChompTheme
import com.google.android.engage.shopping.datamodel.ShoppingCart
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.time.Instant

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            CheapChompTheme {
                //GoogleMapScreen()
                mainScreen()
                //RegistrationScreen()
            }
        }
    }
}

@Composable
fun GoogleMapScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    latitude: Double = 0.0,
    longitude: Double = 0.0
) {
    val context = LocalContext.current
    val fusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Remember currentLocation coordinates
    var currentLocation by remember { mutableStateOf(LatLng(latitude, longitude)) }

    // List of permissions to request
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Permission launcher to request location permissions
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted ->
        if (permissionsGranted[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Permission granted, fetch the location
            fetchLocation(fusedLocationProviderClient) { location ->
                currentLocation = location // Update the state with the fetched location

                // Navigate to Kroger Product Screen with current location
                navController.navigate("KrogerProductScreen/${location.latitude}/${location.longitude}")
            }
        } else {
            Log.d("Permissions", "Location permission denied")
        }
    }

    // Check permissions before fetching the location
    LaunchedEffect(Unit) {
        // Check if permissions are already granted and remembered
        if (permissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        ) {
            // Otherwise request permissions
            launcher.launch(permissions)
        } else {
            // Skip permission check and just fetch location
            fetchLocation(fusedLocationProviderClient) { location ->
                currentLocation = location // Update state after location is fetched

                // Navigate to Kroger Product Screen with current location
                navController.navigate("KrogerProductScreen/${location.latitude}/${location.longitude}")
            }
        }
    }

    // Substitute location in case we cannot use the API
    val fallbackLocation = LatLng(37.7749, -122.4194) //San Francisco

    // Update cameraPositionState on location change
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation.takeIf { it != LatLng(0.0, 0.0) } ?: fallbackLocation, 15f
        )
    }

    // If currentLocation is updated, make sure camera is updated as well
    LaunchedEffect(currentLocation) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
    }

    // Creating a MarkerState for the current location or fallback location
    val markerState = remember { mutableStateOf(MarkerState(position = fallbackLocation)) }

    // Update marker position when location is updated
    LaunchedEffect(currentLocation) {
        markerState.value = MarkerState(position = currentLocation)
    }

    // Display Google Map with Marker
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = markerState.value,  // This marker pinpoints the current location (it's the red arrow)
            title = "Current Location",
            snippet = "You are here!"
        )
    }
}

// private function to get the currentLocation using the API and pass it back to Composable, if it fails then we use a predetermined location
private fun fetchLocation(
    fusedLocationProviderClient: FusedLocationProviderClient,
    onLocationFetched: (LatLng) -> Unit
) {
    try {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                Log.d("Location", "Fetched Location: $latLng")
                onLocationFetched(latLng) // Pass the location back to Composable state
            } ?: run {
                // If lastLocation is null, use fallback location
                val fallbackLatLng = LatLng(37.7749, -122.4194) // San Francisco coordinates
                Log.d("Location", "Location is null, using fallback location")
                onLocationFetched(fallbackLatLng)
            }
        }
    } catch (e: SecurityException) {
        Log.e("Location", "Location permission is not granted", e)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun mainScreen() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    NavHost(navController = navController, startDestination = "LoginScreen") {
        composable("LoginScreen") {
            LoginScreen(navController = navController, auth = auth)
        }
        composable("RegistrationScreen") {
            RegistrationScreen(navController = navController, auth = auth)
        }
        composable("GroceryListScreen") {
            GroceryListScreen(navController = navController, auth = auth)
        }
        composable("GoogleMapScreen") { backStackEntry ->
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0
            GoogleMapScreen(navController = navController, latitude = latitude, longitude = longitude)
        }
        composable("KrogerProductScreen/{latitude}/{longitude}") { backStackEntry ->
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0
            KrogerProductScreen(navController = navController, latitude = latitude, longitude = longitude)
        }
    }
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier, navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") } // display whether login was successful

    Column (
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ){
        Text("Sign in to CheapChomp")
        Spacer(modifier = Modifier.height(16.dp))
        // email textfield
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        // password textfield
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            // sign in with email & password
            Button(onClick = {
                auth.signInWithEmailAndPassword(email, password) // firebase authentication
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            message = "Login successful :)"
                            isLoggedIn = true
                        } else {
                            message = "Login failed: ${task.exception?.message}"
                        }
                    }
            }) {
                Text("Submit")
            }
            Spacer(modifier = Modifier.width(16.dp))
            // sign in with google
            Button(onClick = { /*TODO*/ }) {
                Text("Sign in with Google")
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))

        // navigate to registration screen
        Button(onClick = { navController.navigate("RegistrationScreen") }) {
            Text("New user? Create an account")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, modifier = Modifier.widthIn(max = 250.dp)) // display success or fail

    }
    // Add LaunchedEffect for navigation
    LaunchedEffect(key1 = isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("GoogleMapScreen") { // or "KrogerProductScreen/{latitude}/{longitude}"
                // Pass arguments if needed
                popUpTo("LoginScreen") { inclusive = true } // Optional: Remove LoginScreen from back stack
            }
        }
    }

}

@Composable
fun RegistrationScreen(modifier: Modifier = Modifier, navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") } // display whether registration was successful

    Column (
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ){
        Text("Create an Account")
        Spacer(modifier = Modifier.height(16.dp))
        // email textfield
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        // password textfield
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        // confirm password textfield
        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(
                onClick = {
                    if (password != confirmPassword) {
                        message = "Passwords do not match!"
                    } else {
                        auth.createUserWithEmailAndPassword(email, password) // firebase authentication
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    message = "Account created successfully!"
                                } else {
                                    message = "Error creating account: ${task.exception?.message}"
                                }
                            }
                    }
                }) {
                Text("Create Account")
            }

        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, modifier = Modifier.widthIn(max = 250.dp)) // display success or fail
    }
}

@Composable
fun GroceryListScreen(modifier: Modifier = Modifier, navController: NavController, auth: FirebaseAuth) {

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
    val docRef = db.collection("items")
    docRef.get()
        .addOnSuccessListener { querySnapshot ->
            itemsState.value = querySnapshot.toObjects(Item::class.java) // Update itemsState.value
        }
        .addOnFailureListener { exception ->
            Log.d("DATABASE", "get failed with ", exception)
        }
    var totalPrice = 0f
    for (item in items) {
        val priceString = item.price.replace("[^\\d.]".toRegex(), "")
        val priceFloat = priceString.toFloatOrNull() ?: 0f
        totalPrice += priceFloat
    }
    val totalPriceStr = String.format("%.2f", totalPrice)

    Column {
        BottomNavigation(elevation = 8.dp) {
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Back") },
                selected = false,
                onClick = { navController.navigateUp() }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Grocery List") },
                selected = false,
                onClick = { navController.navigate("GroceryListScreen") }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Product Search") },
                selected = false,
                onClick = { navController.navigate("GoogleMapScreen") }
            )

        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Total Price: $${totalPriceStr}")
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = item.name)
                        Text(text = "$${item.price}")
                    }
                }
            }
        }
    }
    
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KrogerProductScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    latitude: Double,
    longitude: Double
) {
    val krogerApiService = remember { KrogerApiService() }
    var nearestStoreId by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf<ProductPrice?>(null) }
    var productList by remember { mutableStateOf<List<ProductPrice?>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Initial store and product lookup
    LaunchedEffect(latitude, longitude) {
        coroutineScope.launch {
            try {
                val accessToken = krogerApiService.getAccessToken()
                if (accessToken != null) {
                    val storeId = krogerApiService.findNearestStore(accessToken, latitude, longitude)
                    if (storeId != null) {
                        nearestStoreId = storeId
                    } else {
                        //errorMessage = "Could not find nearest store"
                        nearestStoreId = "70400357"
                    }
                } else {
                    errorMessage = "Could not obtain access token"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }

    // Search function
    fun performSearch() {
        if (nearestStoreId.isNotEmpty() && searchQuery.isNotEmpty()) {
            coroutineScope.launch {
                isSearching = true
                errorMessage = ""
                productPrice = null
                try {
                    val accessToken = krogerApiService.getAccessToken()
                    if (accessToken != null) {
                        //val price = krogerApiService.getProductPrice(accessToken, nearestStoreId, searchQuery)
                        val products = krogerApiService.getProductPrices(accessToken, nearestStoreId, searchQuery)
                        productList = products
                        //productPrice = price
                    } else {
                        errorMessage = "Could not obtain access token"
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                } finally {
                    isSearching = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kroger Product Lookup", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Search TextField
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                label = { Text("Enter product name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { performSearch() }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { performSearch() },
                enabled = !isSearching && nearestStoreId.isNotEmpty()
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black
                    )
                } else {
                    Text("Search")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display store and location info
        Text("Latitude: $latitude")
        Text("Longitude: $longitude")

        if (nearestStoreId.isNotEmpty()) {
            Text("Nearest Store ID: $nearestStoreId")
        }

        // Display product info
       /* productPrice?.let { price ->
            Spacer(modifier = Modifier.height(16.dp))
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Product: ${price.name}", style = MaterialTheme.typography.titleMedium)
                    Text("Price: ${price.price}", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }*/
        if (productList.isNotEmpty()) {
            val db = Firebase.firestore
            Spacer(modifier = Modifier.height(16.dp))
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding) // Apply bottom padding
            ){
                itemsIndexed(productList) { index, product ->
                    // Check if the product is not null
                    product?.let {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {
                                Text("Product: ${product.name}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                                Text("Price: ${product.price}", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                                Button(onClick = {
                                    // Create a new item
                                    val item = hashMapOf(
                                        "store_id" to nearestStoreId,
                                        "item_id" to 0,
                                        "name" to product.name,
                                        "price" to product.price,
                                        "quantity" to 1,
                                        "favorited" to false,
                                        "date_added" to Instant.now()
                                    )

                            // Add a new document with a generated ID
                                    db.collection("items")
                                        .add(item)
                                        .addOnSuccessListener { documentReference ->
                                            Log.d("DATABASE", "DocumentSnapshot added with ID: ${documentReference.id}")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("DATABASE", "Error adding document", e)
                                        }

                                }) { Text("Add to List") }
                            }
                        }
                    }
                }
            }
        }

        // Error handling
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = { navController.navigateUp() }) {
            Text("Back")
        }

    }
    Column {
        BottomNavigation(elevation = 8.dp) {
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Back") },
                selected = false,
                onClick = { navController.navigateUp() }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Grocery List") },
                selected = false,
                onClick = { navController.navigate("GroceryListScreen") }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Product Search") },
                selected = false,
                onClick = { navController.navigate("GoogleMapScreen") }
            )

        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CheapChompTheme {
    }
}