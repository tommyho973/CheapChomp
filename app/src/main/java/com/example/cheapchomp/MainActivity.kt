package com.example.cheapchomp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cheapchomp.ui.theme.CheapChompTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
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
fun GoogleMapScreen() {
    val context = LocalContext.current
    val fusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Remember currentLocation coordinates
    var currentLocation by remember { mutableStateOf(LatLng(0.0, 0.0)) }

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
            fetchLocation(fusedLocationProviderClient, { location ->
                currentLocation = location // Update the state with the fetched location
            })
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
            fetchLocation(fusedLocationProviderClient, { location ->
                currentLocation = location // Update state after location is fetched
            })
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

@Composable
fun mainScreen() {
    val navController = rememberNavController() // for navigation
    val auth = FirebaseAuth.getInstance() // initialize and pass firebase authentication
    NavHost(navController = navController, startDestination = "LoginScreen") {
        composable("LoginScreen") { LoginScreen(navController = navController, auth = auth) }
        composable("RegistrationScreen") { RegistrationScreen(navController = navController, auth = auth) }
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




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CheapChompTheme {
    }
}