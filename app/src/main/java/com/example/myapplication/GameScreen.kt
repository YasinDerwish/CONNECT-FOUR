package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

data class FirebaseGameState(
    val board: Array<Array<String?>> = Array(6) { Array(7) { null } },
    val currentPlayer: String = "Red",
    val winner: String? = null
)

enum class PlayerColor(val color: Color){
    Red(Color.Red),
    Yellow(Color.Yellow)

}
data class Player(
    val id: String,
    val name: String,
    val color: PlayerColor,
    var ready: Boolean = false
)


class GameViewModel : ViewModel() {
    var board by mutableStateOf(Array(6) {Array<String?>(7) {null} })
    var currentPlayer by mutableStateOf(PlayerColor.Red)
    var gameResult by mutableStateOf<String?>(null)
    private val gameEngine = GameEngine()

    fun playTurn(column: Int): String? {
        if(gameResult != null) return gameResult

        if(gameEngine.makeMove(column)){
            gameResult = when{
                gameEngine.checkWin() != null -> "${currentPlayer.name} wins!"
                gameEngine.isDraw() -> "It's a draw!"
                else -> {
                    currentPlayer = if (currentPlayer == PlayerColor.Red) PlayerColor.Yellow else PlayerColor.Red
                    null
                }
            }
            return gameResult
        }
        return null
    }
    fun resetGame() {
        gameEngine.reset()
        gameResult = null
        currentPlayer = PlayerColor.Red
        board = Array(6) {Array<String?>(7) {null} }
    }
}

// GameEngine används för att behandla spelarens tur, vem som lagt spelpjäsen vart
// Upptäcker när det blivit lika, or startar om spelet tillsammans med spel-statusen
class GameEngine {
    private val board = Array(6) { Array<PlayerColor?>(7) {null} }
    private var currentPlayer: PlayerColor = PlayerColor.Red
    private var winner: PlayerColor? = null

    fun isDraw(): Boolean{
        return board.all { row -> row.all { it != null } } && winner == null
    }
    fun getBoard() = board
    fun getCurrentPlayer() = currentPlayer
    fun getWinner() = winner

    fun makeMove(column: Int): Boolean {

        if (winner != null) return false

        for (row in board.indices.reversed()) {
            if (board[row][column] == null) {
                board[row][column] = currentPlayer
                winner = checkWin()
                currentPlayer =
                    if (currentPlayer == PlayerColor.Red) PlayerColor.Yellow else PlayerColor.Red
                return true
            }
        }
        return false
    }
    // Försökt att göra en kod som skall hjälpa med att när kolumnen blir full, skall inget mer läggas till.
    // Lyckades inte göra den rätt, slutar med att man kan lägga till hur många "spelpjäser" som helst
    // kan sluta upp i en krasch
    fun reset() {
        for (row in board)row.fill(null)
        winner = null
        currentPlayer = PlayerColor.Red
        }

        // inte lyckats göra en kod där man kan ta bort eller byta ut spelare när de lagts till i lobbyn

    fun checkWin(): PlayerColor? {
        val directions = listOf(
            Pair(1, 0), // Horizontal
            Pair(0, 1), // Vertical
            Pair(1, 1), // Diagonal down-right
            Pair(1, -1) // Diagonal up-right
        )
        for (row in board.indices) {
            for (col in board[row].indices) {
                val color = board[row][col] ?: continue
                for ((dx, dy) in directions) {
                    if ((0 until 4).all { i ->
                            val x = row + i * dx
                            val y = col + i * dy
                            x in board.indices && y in board[0].indices && board[x][y] == color
                        }
                    ) {
                        return color
                        // inte gjort denna kod rätt
                        // Kan resultera i en krasch
                    }
                }
            }
        }
        return null
    }
}

@Composable
//Själva skärmen för gameplay
// UI används här för att "Reset Game", vem som vinnaren är och färgarna av spelpjäserna
fun GameScreen(navController: NavController){
    val gameId = navController.currentBackStackEntry?.arguments?.getString("gameId")
    if (gameId != null) {
        ConnectFourGameWithFirebase(navController, gameId = gameId)
    } else {
        Text("Error: No game ID found.")
    }
}
@Composable
fun Disc(color:Color, onClick: () -> Unit){
    Box(
        modifier = Modifier
            .size(50.dp)
            .padding(2.dp)
            .background(color, CircleShape)
            .clickable { onClick() }
    )
}
/*fun App() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "lobby") {
        composable("lobby") {
            LobbyScreen(navController, playerName = name)
        }
        composable("game") {
            GameScreen(navController) // Navigerar till game utan att själva game finns, spelet laddas inte
        }
    }
}
*/
// Används för game-logiken
//Visar när "Win/Draw" skall implementeras när ett spel nått sitt slut
// Har hand om turordningen
// En 6x7 grid används för att hålla koll på storleken på själva spelbordet
@Composable
fun ConnectFourGame(navController: NavController, challengerId: String) {
    val gameViewModel: GameViewModel = viewModel()
    val board = gameViewModel.board
    val currentPlayer = gameViewModel.currentPlayer
    val gameResult = gameViewModel.gameResult

    val player1 = Player("player1", "Player 1", PlayerColor.Red, ready = true)
    val player2 = Player("player2", "Player 2", PlayerColor.Yellow, ready = true)

    val firstPlayer = if (challengerId == player1.id) player1 else player2
    gameViewModel.currentPlayer = firstPlayer.color

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = gameResult ?: "Current Player: ${currentPlayer}",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (row in board.indices) {
            Row {
                for (col in board[row].indices) {
                    Disc(
                        color = when (board[row][col]) {
                            "Red" -> Color.Red
                            "Yellow" -> Color.Yellow
                            else -> Color.Gray
                        },
                        onClick = {
                            gameViewModel.playTurn(col)
                        }
                    )
                }

            }

        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                gameViewModel.resetGame()
            }) {
            Text("Reset Game")
        }
        Button(onClick = { navController.navigate("lobby") }) {
            Text("Back to Lobby")
        }
    }
}
@Composable
// Använts för att integrera in Firebase med spelet
// Detta för att multiplayer funktionen skall funka och att man inte bara spelar lokalt
//använder LaunchedEffect för att hålla UI uppdaterat med bl.a board currentPlayer, och vem vinnaren är
fun ConnectFourGameWithFirebase(navController: NavController, gameId: String) {
    val database = FirebaseDatabase.getInstance()
    val gameRef = database.getReference("games/$gameId")

    var board by remember { mutableStateOf(Array(6) { Array<String?>(7) { null } }) }
    var currentPlayer by remember { mutableStateOf("Red") }
    var winner by remember { mutableStateOf<String?>(null) }



    LaunchedEffect(gameId) {
        val gameStateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(FirebaseGameState::class.java)?.let { gameState ->
                    board = gameState.board
                    currentPlayer = gameState.currentPlayer
                    winner = gameState.winner
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle Firebase errors
            }

        }
        gameRef.addValueEventListener(gameStateListener)


                   gameRef.removeEventListener(gameStateListener)

           }


        //Försökt fixa denna, den är inuti LaunchedEffect(gameId)
        // När gameId ändrar, skall LaunchedEffect "cancel" den gamla
        // Detta kan kanske inte hända, kan leda till "leaks" eller dubbla listeners för samma spelrunda.



    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (winner != null) {
            Text("Winner: $winner", color = Color.Green)
        } else {
            Text("Current Player: $currentPlayer", color = if (currentPlayer == "Red") Color.Red else Color.Yellow)
        }

        Spacer(modifier = Modifier.height(16.dp))

        board.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEachIndexed { colIndex, cell ->
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(2.dp)
                            .background(
                                when (cell) {
                                    "Red" -> Color.Red
                                    "Yellow" -> Color.Yellow
                                    else -> Color.Gray
                                },
                                CircleShape
                            )
                            .clickable {
                                if(winner == null && cell == null){
                                    gameRef.child("board/$rowIndex/$colIndex").setValue(currentPlayer)


                                        val nextPlayer = if(currentPlayer == "Red") "Yellow" else "Red"
                        gameRef.child("currentPlayer").setValue(nextPlayer)
                                }
                            }
                    )
                }
            }
        }
    }
}
