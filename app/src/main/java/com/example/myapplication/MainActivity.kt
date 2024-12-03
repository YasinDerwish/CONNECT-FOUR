package com.example.myapplication

import android.adservices.adid.AdId
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
data class Player(
    val id: String,
    val name: String,
    val color: PlayerColor,
    var ready: Boolean = false
)

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


                NavHost(navController = navController, startDestination = "main"){

                    composable("main") {
                        MainScreen(navController = navController)
                    }
                    composable("lobby"){
                        LobbyScreen(navController = navController, players = players)
                    }
                    composable("game/{challengerId}"){ backStackEntry ->
                        val challengerId = backStackEntry.arguments?.getString("challengerId") ?: ""
                        ConnectFourGame(navController = navController, challengerId = challengerId)
                    }
                }
            }
        }
    }
}
@Composable
fun LobbyScreen(navController: NavController, players: MutableList<Player>){
    var playerName by remember { mutableStateOf(TextFieldValue("")) }
    val challenges = remember{ mutableStateMapOf<String, String>()  }
    val allPlayersReady = players.size == 2 && players.all { it.ready }
    var selectedOpponentId by remember { mutableStateOf<String?>(null) }
    val name = navController.currentBackStackEntry?.arguments?.getString("name")
    val playerName = navController.currentBackStackEntry?.arguments?.getString("name")


    LaunchedEffect(name) {
        if (!playerName.isNullOrBlank() && players.none { it.name == playerName }) {
            players.add(
                Player(
                    id = "player${players.size +1}",
                    name = playerName,
                    color = if(players.size % 2 == 0) PlayerColor.Red else PlayerColor.Yellow
                )
            )
        }
    }


    fun challengePlayer(challenger: Player, opponentId: String){
        challenges[opponentId] = challenger.id
    }

    fun acceptChallenge(playerId: String){
        val challengerId = challenges[playerId]
        if(challengerId != null){
            navController.navigate("game/$challengerId")
        }
    }
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

                        val newPlayer =  Player(
                                id = "player${players.size +1}",
                                name = playerName.text,
                                color = if (players.size % 2 == 0) PlayerColor.Red else PlayerColor.Yellow
                        )
                        players.add(newPlayer)
                        playerName = TextFieldValue("")
                    }
                }
            ){
                Text("Add Player")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        players.forEach{player ->
            Row(verticalAlignment = Alignment.CenterVertically){
                Text("${player.name} - ${player.color} (${if (player.ready) "Ready" else "Not Ready"})")
                Button(
                    onClick = {
                        player.ready = !player.ready
                    }
                ){
                    Text(if(player.ready) "Unready" else "Ready")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        players.forEach { player ->
            Button(onClick = {challengePlayer(player, "player${players.size +1}")}){
                Text("Challenge")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if(allPlayersReady){
                    navController.navigate("game")
                }
            },
            enabled = allPlayersReady
        ){
            Text("Start Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

       if(players.size > 1) {
           Text("Select an Opponent:")
           DropdownMenuExample(players, selectedOpponentId) {opponentId ->
               selectedOpponentId = opponentId
           }
           Button(
               onClick = {
                   val challenger = players.firstOrNull { it.ready }
                   val opponentId = selectedOpponentId
                   if(challenger != null && opponentId != null) {
                       challengePlayer(challenger, opponentId)
                   }
               },
               enabled = selectedOpponentId != null
           ){
               Text("Challenge")
           }
       }
        Spacer(modifier = Modifier.height(16.dp))

        challenges.forEach { (opponentId, challengerId) ->
            Button(
                onClick = {acceptChallenge(opponentId)}
            ){
                Text("Accept Challenge")
            }
        }

    }

}
@Composable
fun DropdownMenuExample(players: List<Player>, selectedOpponentId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box{
        Text(
            text = selectedOpponentId?.let{ id ->
                players.firstOrNull {it.id == id }?.name ?: "Select Opponent"
            } ?: "Select Opponent",
            modifier = Modifier
                .clickable { expanded = true}
                .background(Color.Gray, CircleShape)
                .padding(8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ){
            players.forEach { player ->
                DropdownMenuItem(
                    text = {Text(player.name)},
                    onClick = {
                        onSelect(player.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ConnectFourGame(navController: NavController, challengerId: String) {
    val gameEngine = remember { GameEngine() }
    var currentPlayer by remember { mutableStateOf(PlayerColor.Red) }
    val board = gameEngine.getBoard()
    var gameResult by remember { mutableStateOf<String?>(null) }
    var highlightedColumn by remember { mutableStateOf(-1) }

    val player1 = Player("player1", "Player 1", PlayerColor.Red, ready = true )
    val player2 = Player("player2", "Player 2", PlayerColor.Yellow, ready = true )

    val firstPlayer = if( challengerId == player1.id) player1 else player2
    currentPlayer = firstPlayer.color
    fun highlightColumn(col: Int){
        highlightedColumn = col
    }

   fun playTurn(column: Int): String? {
        if (gameResult != null) return gameResult
        if (gameEngine.makeMove(column, Player(currentPlayer.name, currentPlayer.name, currentPlayer))) {
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
                        onClick = {
                            gameResult = playTurn(col)
                        },
                        isHighlighted = highlightedColumn == col
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
            .background(
                color = if(isHighlighted) Color.LightGray else color,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun ConnectFourGamePreview() {
    MyApplicationTheme {
        ConnectFourGame(navController = rememberNavController(), challengerId = "player1")
    }
}
