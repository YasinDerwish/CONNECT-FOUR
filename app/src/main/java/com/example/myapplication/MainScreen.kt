package com.example.myapplication

import androidx.navigation.compose.composable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost // Definierar navigation rutten för lobby och spel skärmen
//Skickar nödvändiga argument (exempelvis gameId) mellan skärmarna när man spelar multiplayer
import androidx.navigation.compose.composable
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation.navArgument
import com.google.firebase.database.*

object NavRoutes {
    const val Main_Screen = "mainScreen"
    const val Lobby = "lobby?name={name}"
    const val Game = "game/{gameId}"



}

@Composable
// själva navigationen och hur man navigerar sig till olika ställen inom spel-appen
fun AppNavigation(navController: NavController) {

    NavHost(navController = navController, startDestination = "mainScreen") {
        composable(NavRoutes.Main_Screen) {
            MainScreen(navController = navController)
        }
        composable(NavRoutes.Lobby) { backStackEntry ->
            val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "Guest")
            LobbyScreen(navController = navController, playerName = name)
        }

        composable(NavRoutes.Game) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: "defaultGame"
            ConnectFourGameWithFirebase(navController = navController, gameId = gameId)
        }

    }
}
//

@Composable
// används när man skriver sitt namn och joinar lobby-skärmen
//Skriver ut ett error om namn-fältet är tomt
fun MainScreen(navController: NavController){
    var playerName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text("Welcome to Connect Four", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = playerName,
            onValueChange = {
                playerName = it
                showError = false
            },
            label = {Text("Enter your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if(showError){
            Text(
                text = "Name cannot be empty!",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick ={
                if(playerName.isNotBlank()){
                    navController.navigate("lobby?name=$playerName")
                }
                else{
                    showError = true
                }
            }
        ){
            Text("Go To Lobby")
        }
    }
}

@Composable
fun Disc(color: Color, onClick: () -> Unit, isHighlighted: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .background(color, CircleShape)
            .clickable(onClick = onClick)
    )// Själva spelpjäsen som man använder i spelet
}

@Preview(showBackground = true)
@Composable
fun ConnectFourGamePreview() {
    MyApplicationTheme {
        ConnectFourGame(navController = rememberNavController(), challengerId = "player1")
    }
}
