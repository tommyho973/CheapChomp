package com.example.cheapchomp

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigationDefaults
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.cheapchomp.ui.theme.CheapChompTheme
import com.google.android.engage.shopping.datamodel.ShoppingCart
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            CheapChompTheme {
                Surface(color = Color(0xFF98FB98)) {
                    //GoogleMapScreen()
                    mainScreen()
                    //RegistrationScreen()
                }
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
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    auth: FirebaseAuth
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") } // display whether login was successful
    val intSizeSaver = Saver<IntSize, Pair<Int,Int>>(save = {it.width to it.height}, restore = {IntSize(it.first, it.second)})
    var textFieldSize by rememberSaveable(stateSaver = intSizeSaver) { mutableStateOf(IntSize.Zero) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if(isLandscape){
        Row(){
            Column(modifier = Modifier.weight(1f)
                    .fillMaxSize()
                .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center){
                Text("Welcome to CheapChomp!", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
                // email textfield
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        textFieldSize = coordinates.size
                    }
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
                // sign in with email & password
                Button(
                    onClick = {
                        auth.signInWithEmailAndPassword(email, password) // firebase authentication
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    message = "Login successful :)"
                                    isLoggedIn = true
                                } else {
                                    message = "Login failed: ${task.exception?.message}"
                                }
                            }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                    modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                ) {
                    Text("Login")
                }
                Row() {
                    Text("Don't have an account?")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sign Up",
                        color = Color(0xFF56AE57),
                        modifier = Modifier.clickable { navController.navigate("RegistrationScreen") })
                }
                Text("OR")
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Sign In with Google",
                    modifier = Modifier
                        .size(250.dp, 50.dp)
                        .border(1.dp, Color(0xFF56AE57), RoundedCornerShape(50)) // Border first
                        .clip(RoundedCornerShape(50)) // Clip to the border shape
                        .background(Color.White)
                        .clickable { /*Do something later*/ }
                )
                Text(message, modifier = Modifier.widthIn(max = 250.dp)) // display success or fail
            }
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.weight(1f))
        }
    }
    else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo")
            Text("Welcome to CheapChomp!", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            // email textfield
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size
                }
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
            // sign in with email & password
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(email, password) // firebase authentication
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                message = "Login successful :)"
                                isLoggedIn = true
                            } else {
                                message = "Login failed: ${task.exception?.message}"
                            }
                        }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row() {
                Text("Don't have an account?")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sign Up",
                    color = Color(0xFF56AE57),
                    modifier = Modifier.clickable { navController.navigate("RegistrationScreen") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("OR")
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Sign In with Google",
                modifier = Modifier
                    .size(250.dp, 50.dp)
                    .border(1.dp, Color(0xFF56AE57), RoundedCornerShape(50)) // Border first
                    .clip(RoundedCornerShape(50)) // Clip to the border shape
                    .background(Color.White)
                    .clickable { /*Do something later*/ }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, modifier = Modifier.widthIn(max = 250.dp)) // display success or fail

        }
    }
        // Add LaunchedEffect for navigation
        LaunchedEffect(key1 = isLoggedIn) {
            if (isLoggedIn) {
                navController.navigate("GoogleMapScreen") { // or "KrogerProductScreen/{latitude}/{longitude}"
                    // Pass arguments if needed
                    popUpTo("LoginScreen") {
                        inclusive = true
                    } // Optional: Remove LoginScreen from back stack
                }
            }
        }

}

@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    auth: FirebaseAuth
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") } // display whether registration was successful
    var textFieldSize2 by remember{mutableStateOf(IntSize.Zero)}


    Column (
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        // email textfield
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.onGloballyPositioned { coordinates ->textFieldSize2 = coordinates.size}
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
                                    val db = Firebase.firestore
                                    val user = hashMapOf(
                                        "email" to email
                                    )
                                    // Add a new document with a generated ID
                                    db.collection("users")
                                        .add(user)

                                    getUserRef { userRef ->
                                        val grocery_list = hashMapOf(
                                            "favorited" to false,
                                            "user" to userRef
                                        )
                                        db.collection("grocery_list")
                                            .add(grocery_list)
                                    }
                                    navController.navigate("LoginScreen")
                                } else {
                                    message = "Error creating account: ${task.exception?.message}"
                                }
                            }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                modifier = Modifier.width(with(LocalDensity.current){textFieldSize2.width.toDp()})) {
                Text("Create Account")
            }

        }
        Button(onClick = { navController.navigate("LoginScreen") },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),modifier = Modifier.width(with(LocalDensity.current){textFieldSize2.width.toDp()})) {
            Text("Back to Login")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, modifier = Modifier.widthIn(max = 250.dp)) // display success or fail
    }
}

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
    getGroceryList { listRef ->
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
                                    deleteFromDatabase(
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
                        errorMessage = "Could not find nearest store"
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
        modifier = Modifier.fillMaxSize()
    ) {
        // Existing content moved inside a Column with fillMaxSize
        // For Nav vs Content Positioning Purposes
        Column(
            modifier = Modifier
                .weight(1f)
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
                    onClick = { performSearch()},
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
            if (productList.isNotEmpty()) {
                val db = Firebase.firestore
                Spacer(modifier = Modifier.height(16.dp))
                val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding)
                ){
                    itemsIndexed(productList.filter{
                        it?.price != "N/A"
                    }) { index, product ->
                        product?.let {
                            SwipeableProductItem(
                                product = it,
                                nearestStoreId = nearestStoreId,
                                onAddToDatabase = { quantity ->
                                    Log.d("DATABASE", "Quantity being passed to addToDatabase: $quantity")
                                    addToDatabase(it, nearestStoreId, quantity)
                                }
                            )
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
        }

        // Bottom Navigation
        BottomNavigation(backgroundColor = Color(0xFF56AE57),elevation = 8.dp) {
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                label = { Text("Back") },
                selected = false,
                onClick = { navController.navigateUp() }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Search, contentDescription = "Product Search") },
                label = { Text("Product Search") },
                selected = true,
                onClick = { /* current screen! do nothing :> */ }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Grocery List") },
                label = { Text("Grocery List") },
                selected = false,
                onClick = { navController.navigate("GroceryListScreen") }
            )
        }
    }
}

@Composable
fun ProductImage(
    imageUrl: String?
){
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier.size(100.dp),
        contentScale = ContentScale.Crop
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun addToDatabase(
    product: ProductPrice,
    nearestStoreId: String,
    quantity: Int
) {
    val db = Firebase.firestore
    getGroceryList { listRef ->
        // Debug: incoming quantity
        Log.d("DATABASE", "Adding quantity: $quantity")

        db.collection("items")
            .whereEqualTo("grocery_list", listRef)
            .whereEqualTo("store_id", nearestStoreId)
            .whereEqualTo("name", product.name)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Item exists, update quantity
                    val doc = documents.documents[0]
                    val currentQuantity = doc.getLong("quantity")?.toInt() ?: 0
                    Log.d("DATABASE", "Current quantity in DB: $currentQuantity")
                    Log.d("DATABASE", "Adding quantity: $quantity")
                    val newQuantity = currentQuantity + quantity
                    Log.d("DATABASE", "New quantity will be: $newQuantity")

                    doc.reference.update("quantity", newQuantity)
                        .addOnSuccessListener {
                            Log.d("DATABASE", "DocumentSnapshot updated with new quantity: $newQuantity")
                        }
                        .addOnFailureListener { e ->
                            Log.w("DATABASE", "Error updating document", e)
                        }
                } else {
                    // Item doesn't exist, create new document
                    Log.d("DATABASE", "Creating new document with initial quantity: $quantity")
                    val item = hashMapOf(
                        "store_id" to nearestStoreId,
                        "item_id" to 0,
                        "name" to product.name,
                        "price" to product.price,
                        "quantity" to quantity,
                        "favorited" to false,
                        "date_added" to Instant.now(),
                        "grocery_list" to listRef
                    )

                    db.collection("items")
                        .add(item)
                        .addOnSuccessListener { documentReference ->
                            Log.d("DATABASE", "DocumentSnapshot added with ID: ${documentReference.id} and quantity: $quantity")
                        }
                        .addOnFailureListener { e ->
                            Log.w("DATABASE", "Error adding document", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("DATABASE", "Error checking for existing item", e)
            }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun deleteFromDatabase(
    product: ProductPrice,
    nearestStoreId: String,
    onItemDeleted:() -> Unit
) {
    val db = Firebase.firestore
    getGroceryList { listRef ->
        db.collection("items")
            .whereEqualTo("grocery_list", listRef)
            .whereEqualTo("store_id", nearestStoreId)
            .whereEqualTo("name", product.name)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    doc.reference.delete()
                        .addOnSuccessListener {
                            Log.d("DATABASE", "DocumentSnapshot successfully deleted!")
                            onItemDeleted()
                        }
                        .addOnFailureListener { e ->
                            Log.w("DATABASE", "Error deleting document", e)
                        }
                } else {
                    Log.d("DATABASE", "Item not found in database")
                }
            }
            .addOnFailureListener { e ->
                Log.w("DATABASE", "Error checking for existing item", e)
            }
    }
}


@Composable
fun SwipeableProductItem(
    product: ProductPrice,
    nearestStoreId: String,
    onAddToDatabase: (quantity: Int) -> Unit
) {
    var offset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val swipeThreshold = 300f
    var clickCount by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var manualQuantity by remember { mutableStateOf("1") }
    var existingQuantity by remember { mutableIntStateOf(0) }
    var showAddedMessage by remember { mutableStateOf(false) }
    var lastAddedQuantity by remember { mutableIntStateOf(0) }

    // Query Firestore to get initial quantity
    LaunchedEffect(product.name, nearestStoreId) {
        getGroceryList { listRef ->
            val db = Firebase.firestore
            db.collection("items")
                .whereEqualTo("grocery_list", listRef)
                .whereEqualTo("store_id", nearestStoreId)
                .whereEqualTo("name", product.name)
                .get()
                .addOnSuccessListener { documents ->
                    existingQuantity = documents.sumOf { doc ->
                        doc.getLong("quantity")?.toInt() ?: 0
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting existing quantity", e)
                }
        }
    }

    // Hide the message after delay
    LaunchedEffect(showAddedMessage) {
        if (showAddedMessage) {
            delay(2000) // Message shows for 2 seconds
            showAddedMessage = false
        }
    }

    val handleAddToDatabase = { quantity: Int ->
        existingQuantity += quantity
        lastAddedQuantity = quantity
        showAddedMessage = true
        onAddToDatabase(quantity)
    }

    // Animate the offset with a spring-like animation for smooth sling-back
    val animatedOffset by animateFloatAsState(
        targetValue = if (!isDragging) 0f else offset,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "offset animation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        // If swiped beyond threshold, add to database
                        if (offset >= swipeThreshold) {
                            handleAddToDatabase(1) // Swipe should be simplistic, only add 1
                        }
                        // Reset offset for next swipe
                        offset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // Allow dragging regardless of previous swipes
                        offset = (offset + dragAmount).coerceIn(0f, swipeThreshold)
                    }
                )
            }
    ) {
        // Background (Add icon)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF56AE57).copy(alpha = 0.2f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add to List",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .scale(2f)
                    .alpha(animatedOffset / swipeThreshold),
                tint = Color(0xFF56AE57)
            )
        }

        // Foreground (Product Card)
        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Product Image
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        ProductImage(imageUrl = product.imageUrl)
                    }

                    // Product Details Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Product Name
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Price and Save to Grocery List in a row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$${product.price}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF56AE57)
                            )

                            Button(
                                onClick = {
                                    clickCount++
                                    if (clickCount >= 3) {
                                        showDialog = true
                                    } else {
                                        handleAddToDatabase(1)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF56AE57)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                            ) {
                                Text("Save to List")
                            }
                        }
                        AnimatedVisibility(
                            visible = showAddedMessage,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = "Added ${lastAddedQuantity} item${if (lastAddedQuantity > 1) "s" else ""}",
                                color = Color(0xFF56AE57),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Quantity Badge
                if (existingQuantity > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 12.dp, y = (-12).dp)
                            .background(
                                color = Color(0xFF56AE57),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = existingQuantity.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                clickCount = 0
            },
            title = { Text("Seems You Want Multiple Items...") },
            text = {
                Column {
                    Text("Enter the quantity you'd like to add:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = manualQuantity,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                manualQuantity = newValue
                            }
                        },
                        label = { Text("Quantity") },
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
                        val quantity = manualQuantity.toIntOrNull() ?: 1
                        if (quantity > 0) {
                            handleAddToDatabase(quantity)
                            showDialog = false
                            clickCount = 0
                            manualQuantity = "1"
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                        clickCount = 0
                        manualQuantity = "1"
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getUserRef(onResult: (DocumentReference) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val email = currentUser?.email

    if (email == null) {
        // Handle cases where email is null
        null
    }

    val firestore = Firebase.firestore
    firestore.collection("users")
        .whereEqualTo("email", email)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val userId = querySnapshot.documents[0].reference // Firestore document ID
                onResult(userId)
            } else {
                null // No user found
            }
        }
        .addOnFailureListener { e ->
            Log.e("FirestoreError", "Error fetching user ID", e)
            null
        }
}

fun getGroceryList(onResult: (DocumentReference) -> Unit) {
    getUserRef { userRef ->
        val firestore = Firebase.firestore
        Log.d("Firestore", "User ID: ${userRef.id}")
        Log.d("Firestore", "User Ref Path: ${userRef.path}")

        firestore.collection("grocery_list")
            .whereEqualTo("user", userRef)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val groceryListRef = querySnapshot.documents[0].reference // Firestore document ID
                    Log.d("Firestore", "GroceryList ID: ${groceryListRef.id}")
                    onResult(groceryListRef)
                } else {
                    Log.d("Firestore", "No grocery list found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching grocery list ID", e)
            }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CheapChompTheme {
    }
}