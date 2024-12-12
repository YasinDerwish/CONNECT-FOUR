package com.example.myapplication

import android.net.Uri
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
import com.google.firebase.database.FirebaseDatabase




class LobbyViewModel : ViewModel() {
    val players = mutableStateListOf<Player>()
}
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
// GameEngine används för att behandla spelarens tur, vem som lagt spelpjäsen vart
// Upptäcker när det blivit lika, or startar om spelet tillsammans med spel-statusen
class GameEngine {
    private val board = Array(6) { Array<PlayerColor?>(7) {null} }
    private var currentPlayer: PlayerColor = PlayerColor.Red
    private var winner: PlayerColor? = null

    fun isDraw(): Boolean{
        for(row in board){
            for(cell in row){
                if(cell == null){
                    return false
                }
            }
        }
        return winner == null
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
        for (row in board) {
            row.fill(null)
        }
        winner = null
        currentPlayer = PlayerColor.Red
        // inte lyckats göra en kod där man kan ta bort eller byta ut spelare när de lagts till i lobbyn
    }
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
fun LobbyScreen(navController: NavController, players: MutableList<Player>){
    var enteredPlayerName by remember { mutableStateOf(TextFieldValue("")) }
    val allPlayersReady = players.size == 2 && players.all { it.ready }
    var selectedOpponentId by remember { mutableStateOf<String?>(null) }
    val playerName = navController.currentBackStackEntry?.arguments?.getString("name")
    val challenges = remember { mutableStateOf(mutableMapOf<String, String>()) }
    // Är ingen "MutableState", UI kommer inte visa då när "challenges" läggs till/uppdateras.
    //Slutar med att UI kan inte visa nya challenges och andra ny information




    LaunchedEffect(playerName) {
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
// Förhindrar att flera  av samma namn läggs till

    fun challengePlayer(challenger: Player, opponentId: String){
        challenges.value[opponentId] = challenger.id
    }

    fun acceptChallenge(playerId: String){
        val challengerId = challenges.value[playerId]
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
                value = enteredPlayerName,
                onValueChange = { enteredPlayerName = it},
                modifier = Modifier
                    .background(Color.Gray, CircleShape)
                    .padding(8.dp)
            )
            Button(
                onClick = {
                    if(enteredPlayerName.text.isNotBlank()){

                        val newPlayer =  Player(
                                id = "player${players.size +1}",
                                name = enteredPlayerName.text,
                                color = if (players.size % 2 == 0) PlayerColor.Red else PlayerColor.Yellow
                        )
                        players.add(newPlayer)
                        enteredPlayerName = TextFieldValue("")
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
            // ändrat ready-state förhindrar mutableState tracking. Kommer icke trigga igång UI
            // Detta gör att UI inte uppdaterar för att visa en spelares status (Ready/Unready)
        }
        Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (allPlayersReady) {
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
                   if(challenger != null && opponentId != null && opponentId != challenger.id) {
                       challengePlayer(challenger, opponentId)
                   }
               },
               enabled = selectedOpponentId != null
           ){
               Text("Challenge")
           }
       }
        Spacer(modifier = Modifier.height(16.dp))

        challenges.value.forEach{ (opponentId, challengerId) ->
            Button(
                onClick = {acceptChallenge(opponentId)}
            ){
                Text("Accept Challenge")
            }
        }

    }

}
@Composable
// Meningen med denna är att man skall kunna välja en spelare från de aktiva
// Uppdaterar den nuvarande motståndarens ID
fun DropdownMenuExample(players: List<Player>, selectedOpponentId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Text(
            text = selectedOpponentId?.let { id ->
                players.firstOrNull { it.id == id }?.name ?: "Select Opponent"
            } ?: "Select Opponent",
            modifier = Modifier.clickable { expanded = true }
                .background(Color.Gray, CircleShape)
                .padding(8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.name) },
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

        onDispose {
            gameRef.removeEventListener(gameStateListener)
        }
        //Försökt fixa denna, den är inuti LaunchedEffect(gameId)
        // När gameId ändrar, skall LaunchedEffect "cancel" den gamla
        // Detta kan kanske inte hända, kan leda till "leaks" eller dubbla listeners för samma spelrunda.

    }

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
                                gameRef.child("currentPlayer").setValue(
                                    if(currentPlayer == "Red") "Yellow" else "Red"
                                )
                            }
                        }
                )
            }
            }
        }
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
fun App() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "lobby") {
        composable("lobby") {
            LobbyScreen(navController, players = mutableListOf())
        }
        composable("game") {
            GameScreen(navController) // Navigerar till game utan att själva game finns, spelet laddas inte
        }
    }
}

@Composable
// Används för game-logiken
//Visar när "Win/Draw" skall implementeras när ett spel nått sitt slut
// Har hand om turordningen
// En 6x7 grid används för att hålla koll på storleken på själva spelbordet
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
        // Kan inte på ett korrekt sätt implementera denna kod
        // Kan inte visa när en korrekt speltur har gjorts
    }
    // inte gjort denna kod rätt, kan sluta med att oförväntat spel, inkorrekt turordning kan uppkomma

    fun playTurn(column: Int): String? {
        if (gameResult != null) return gameResult
        if (gameEngine.makeMove(column)) {
            gameResult = when {
                gameEngine.checkWin() != null ->
                    "$currentPlayer wins!"

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
        Button(onClick = {navController.navigate("lobby") }){
            Text("Back to Lobby")
        }
    }
}

@Composable
// själva navigationen och hur man navigerar sig till olika ställen inom spel-appen
fun AppNavigation(navController: NavController) {

    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") {
            MainScreen(navController = navController)
        }
        composable("lobby?name={name}") { backStackEntry ->
            val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "Guest")
            LobbyScreen(navController = navController, playerName = name)
        }

        composable(
            route = "game/{gameId}",
            arguments = listOf(navArgument("gameId") { defaultValue = "defaultGame" })
        ) { backStackEntry ->
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
