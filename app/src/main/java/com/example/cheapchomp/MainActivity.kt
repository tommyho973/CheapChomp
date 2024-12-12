package com.example.cheapchomp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.cheapchomp.repository.OfflineDatabase
import com.example.cheapchomp.ui.screens.GoogleMapScreen
import com.example.cheapchomp.ui.screens.GroceryListScreen
import com.example.cheapchomp.ui.screens.KrogerProductScreen
import com.example.cheapchomp.ui.screens.LoginScreen
import com.example.cheapchomp.ui.screens.RegistrationScreen
import com.example.cheapchomp.ui.theme.CheapChompTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        setContent {
            CheapChompTheme {
                Surface(color = Color(0xFF98FB98)) {
                    //GoogleMapScreen()
                    mainScreen(applicationContext)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun mainScreen(applicationContext: android.content.Context) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val room_db = Room.databaseBuilder(
        applicationContext,
        OfflineDatabase::class.java, "items_database"
    ).allowMainThreadQueries().fallbackToDestructiveMigration().build()


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
        composable(
            route = "KrogerProductScreen/{latitude}/{longitude}",
            arguments = listOf(
                navArgument("latitude") {
                    type = NavType.StringType
                    defaultValue = "37.7749"  // San Francisco default
                },
                navArgument("longitude") {
                    type = NavType.StringType
                    defaultValue = "-122.4194"  // San Francisco default
                }
            )
        ) { backStackEntry ->
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 37.7749
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: -122.4194
            KrogerProductScreen(navController = navController, latitude = latitude, longitude = longitude, room_db = room_db)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CheapChompTheme {
    }
}