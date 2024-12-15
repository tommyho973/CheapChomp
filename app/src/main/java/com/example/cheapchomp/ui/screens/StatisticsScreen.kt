package com.example.cheapchomp.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cheapchomp.network.LocationService
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.bar.BarChartData.Bar
import com.github.tehras.charts.bar.renderer.bar.SimpleBarDrawer
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.github.tehras.charts.bar.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.bar.renderer.yaxis.SimpleYAxisDrawer
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import kotlinx.coroutines.launch

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    navController: NavController
    ) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {}
        Text("Landscape Mode")
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            MyBarChartParent(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
            )
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
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Grocery List") },
                    label = { Text("Grocery List") },
                    selected = true,
                    onClick = { navController.navigate("GroceryListScreen") }
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
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Statistics") },
                    label = { Text("Statistics") },
                    selected = false,
                    onClick = { /* current screen, do nothing*/ }
                )

            }
        }
    }


}

@Composable
fun MyBarChartParent(modifier: Modifier) {
    BarChart(
        barChartData = BarChartData(bars = listOf(Bar(label = "Bar Label", value = 100f, color = Color(0xFF56AE57)))),
        modifier = modifier,
        animation = simpleChartAnimation(),
        barDrawer = SimpleBarDrawer(),
        xAxisDrawer = SimpleXAxisDrawer(),
        yAxisDrawer = SimpleYAxisDrawer(),
        labelDrawer = SimpleValueDrawer()
    )

}
