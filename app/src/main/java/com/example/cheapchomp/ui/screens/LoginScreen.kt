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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cheapchomp.R
import com.google.firebase.auth.FirebaseAuth

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
