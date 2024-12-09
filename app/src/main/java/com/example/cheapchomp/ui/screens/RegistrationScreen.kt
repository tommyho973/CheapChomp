package com.example.cheapchomp.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cheapchomp.repository.DatabaseRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.saveable.Saver
import com.example.cheapchomp.R

@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    auth: FirebaseAuth
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") } // display whether registration was successful
    val intSizeSaver = Saver<IntSize, Pair<Int,Int>>(save = {it.width to it.height}, restore = {IntSize(it.first, it.second)})
    var textFieldSize2 by rememberSaveable(stateSaver = intSizeSaver) { mutableStateOf(IntSize.Zero) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE



    if(isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column( modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .weight(1f)
            ) {
                Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                // email textfield
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .width(400.dp)
                        .onGloballyPositioned { coordinates ->
                            textFieldSize2 = coordinates.size
                        }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // password textfield
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.width(400.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // confirm password textfield
                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.width(400.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            message = "Passwords do not match!"
                        } else {
                            auth.createUserWithEmailAndPassword(
                                email,
                                password
                            ) // firebase authentication
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
                                        DatabaseRepository().getUserRef { userRef ->
                                            val grocery_list = hashMapOf(
                                                "favorited" to false,
                                                "user" to userRef
                                            )
                                            db.collection("grocery_list")
                                                .add(grocery_list)
                                        }
                                        navController.navigate("LoginScreen")
                                    } else {
                                        message =
                                            "Error creating account: ${task.exception?.message}"
                                    }
                                }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                    modifier = Modifier.width(with(LocalDensity.current) { textFieldSize2.width.toDp() })
                ) {
                    Text("Create Account")
                }
            }
            Column(modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .weight(.75f)) {
                Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.weight(1f))
                Text(
                    text = "Already have an account?"
                )
                Text(text = "Login", modifier = Modifier.clickable(onClick = { navController.navigate("LoginScreen") }), color = Color(0xFF56AE57))
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, modifier = Modifier.widthIn(max = 250.dp)) // display success or fail
            }
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
            Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            // email textfield
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    textFieldSize2 = coordinates.size
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
                            auth.createUserWithEmailAndPassword(
                                email,
                                password
                            ) // firebase authentication
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
                                        DatabaseRepository().getUserRef { userRef ->
                                            val grocery_list = hashMapOf(
                                                "favorited" to false,
                                                "user" to userRef
                                            )
                                            db.collection("grocery_list")
                                                .add(grocery_list)
                                        }
                                        navController.navigate("LoginScreen")
                                    } else {
                                        message =
                                            "Error creating account: ${task.exception?.message}"
                                    }
                                }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                    modifier = Modifier.width(with(LocalDensity.current) { textFieldSize2.width.toDp() })
                ) {
                    Text("Create Account")
                }
            }
            Button(
                onClick = { navController.navigate("LoginScreen") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56AE57)),
                modifier = Modifier.width(with(LocalDensity.current) { textFieldSize2.width.toDp() })
            ) {
                Text("Back to Login")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, modifier = Modifier.widthIn(max = 250.dp)) // display success or fail
        }
    }
}