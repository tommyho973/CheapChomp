package com.example.cheapchomp.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cheapchomp.R
import com.example.cheapchomp.ui.state.LoginUiState
import com.example.cheapchomp.viewmodel.LoginViewModel
import com.example.cheapchomp.viewmodel.LoginViewModelFactory
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    auth: FirebaseAuth
) {
    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(auth)
    )
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var message by rememberSaveable { mutableStateOf("") } // display whether login was successful
    val intSizeSaver = Saver<IntSize, Pair<Int,Int>>(save = {it.width to it.height}, restore = {IntSize(it.first, it.second)})
    var textFieldSize by rememberSaveable(stateSaver = intSizeSaver) { mutableStateOf(IntSize.Zero) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if(isLandscape){
        Row(){
            Column(modifier = Modifier
                .weight(1f)
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
                    onClick = { viewModel.login(email, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                    modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                ) {
                    when (uiState) {
                        is LoginUiState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                        else -> Text("Login")
                    }
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
                onClick = { viewModel.login(email, password) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
            ) {
                when (uiState) {
                    is LoginUiState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    else -> Text("Login")
                }
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
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val locationService = LocationService(context)
            try {
                val location = locationService.getCurrentLocation()
                navController.navigate(
                    "KrogerProductScreen/${location.latitude}/${location.longitude}"
                ) {
                    popUpTo("LoginScreen") { inclusive = true }
                }
            } catch (e: Exception) {
                navController.navigate("KrogerProductScreen/37.7749/-122.4194") {
                    popUpTo("LoginScreen") { inclusive = true }
                }
            }
        }
    }
}
