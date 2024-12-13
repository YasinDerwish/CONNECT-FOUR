package com.example.myapplication
//Skickar nödvändiga argument (exempelvis gameId) mellan skärmarna när man spelar multiplayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.database.*
import com.example.myapplication.LobbyScreen

object NavRoutes {
    const val Main_Screen = "mainScreen"
    const val Lobby = "lobby"
    const val Game = "game/{gameId}"

}
class MainActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}

@Composable
// själva navigationen och hur man navigerar sig till olika ställen inom spel-appen
fun AppNavigation(navController: NavController) {

    NavHost(navController = navController, startDestination = NavRoutes.Main_Screen) {
        composable(NavRoutes.Main_Screen) {
            MainScreen(navController = navController)
        }
        composable(
            route = NavRoutes.Lobby
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Guest"
            LobbyScreen(navController = navController, playerName = name)
        }

        composable(
            route = NavRoutes.Game,
            arguments = listOf(navArgument("gameId") { defaultValue = "defaultGame" })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: "defaultGame"
            ConnectFourGameWithFirebase(navController = navController, gameId = gameId)
        }
    }
}
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
                    navController.navigate("${NavRoutes.Lobby}?name=${Uri.encode(playerName)}")
                }
                else{
                    showError = true
                }
            }
        )
        {
            Text("Go To Lobby")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ConnectFourGamePreview() {
    MyApplicationTheme {
        ConnectFourGame(navController = rememberNavController(), challengerId = "player1")
    }
}

