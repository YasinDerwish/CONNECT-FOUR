package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.motion.widget.MotionController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable

enum class PlayerColor(val color: Color){
    Red(Color.Red),
    Yellow(Color.Yellow)
}
data class Player(val name: String, val color: PlayerColor)

class GameEngine {
    private val board = Array(6) { Array<PlayerColor?>(7) {null} }

    fun getBoard() = board

    fun makeMove(column: Int, player: Player): Boolean {
        for (row in board.indices.reversed()) {
            if (board[row][column] == null) {
                board[row][column] = player.color
                return true
            }
        }
        return false
    }

    fun checkWin(): Boolean {
        for (row in 0 until 6) {
            for (col in 0 until 7) {
                val color = board[row][col]
                if (color != null && (
                            checkDirection(row, col, 1, 0, color) ||  // Horizontal
                                    checkDirection(row, col, 0, 1, color) ||  // Vertical
                                    checkDirection(row, col, 1, 1, color) ||  // Diagonal Down
                                    checkDirection(row, col, 1, -1, color)    // Diagonal Up
                            )
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int, color: PlayerColor): Boolean {
        var count = 0
        var r = row
        var c = col
        for (i in 0 until 4) {
            if (r in 0..5 && c in 0..6 && board[r][c] == color) {
                count++
                r += deltaRow
                c += deltaCol
            } else break
        }
        return count == 4
    }

    fun isDraw(): Boolean {
        if (checkWin()) return false
        for (row in board) {
            for (cell in row) {
                if (cell == null) return false
            }
        }
        return true
    }

    fun reset() {
        for (row in board) {
            row.fill(null)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val players = remember { mutableStateListOf<Player>() }

                NavHost(navController = navController, startDestination = "lobby"){
                    composable("lobby") {
                        LobbyScreen(navController = navController, players = players)
                    }
                    composable("game"){
                        ConnectFourGame(navController = navController)
                    }
                }
            }
        }
    }
}
@Composable
fun LobbyScreen(navController: NavController, players: MutableList<Player>){
    var playerName by remember { mutableStateOf(TextFieldValue("")) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text("Lobby", style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically){
            BasicTextField(
                value = playerName,
                onValueChange = { playerName = it},
                modifier = Modifier
                    .background(Color.Gray, CircleShape)
                    .padding(8.dp)
            )
            Button(
                onClick = {
                    if(playerName.text.isNotBlank()){
                        players.add(Player(playerName.text, if(players.size % 2 ==0) PlayerColor.Red else PlayerColor.Yellow))
                        playerName = TextFieldValue("")
                    }
                }
            ){
                Text("Add Player")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        players.forEach { player ->
            Text("${player.name} - ${player.color}")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("game") }) {
            Text("Start Game")
        }
    }

}
@Composable
fun ConnectFourGame(navController: NavController) {
    val gameEngine = remember { GameEngine() }
    var currentPlayer by remember { mutableStateOf(PlayerColor.Red) }
    val board = gameEngine.getBoard()
    var gameResult by remember { mutableStateOf<String?>(null) }

    fun playTurn(column: Int): String? {
        if (gameResult != null) return gameResult
        if (gameEngine.makeMove(column, Player("Player", currentPlayer))) {
            return when {
                gameEngine.checkWin() -> "$currentPlayer wins!"
                gameEngine.isDraw() -> "It's a draw!"
                else -> {
                    currentPlayer = if (currentPlayer == PlayerColor.Red) PlayerColor.Yellow else PlayerColor.Red
                    null
                }
            }
        }
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = gameResult ?: "Current Player: $currentPlayer",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (row in board.indices) {
            Row {
                for (col in board[row].indices) {
                    Disc(
                        color = when (board[row][col]) {
                             PlayerColor.Red-> PlayerColor.Red.color
                            PlayerColor.Yellow -> PlayerColor.Yellow.color
                            else -> Color.Gray
                        },
                        onClick = { gameResult = playTurn(col) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            gameEngine.reset()
            gameResult = null
            currentPlayer = PlayerColor.Red
        }) {
            Text("Reset Game")
        }
    }
}

@Composable
fun Disc(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .background(color, shape = CircleShape)
            .clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun ConnectFourGamePreview() {
    MyApplicationTheme {
        ConnectFourGame(navController = rememberNavController())
    }
}
