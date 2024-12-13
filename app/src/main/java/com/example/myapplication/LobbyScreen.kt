package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LobbyViewModel : ViewModel() {
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> get() = _players

    private val _challenges = MutableStateFlow<Map<String, String>>(emptyMap())
    val challenges: StateFlow<Map<String, String>> get() = _challenges

    fun addPlayer(name: String) {
        if (name.isNotBlank() && _players.value.none { it.name == name }) {
            val newPlayer = Player(
                id = "player${_players.value.size + 1}",
                name = name,
                color = if (_players.value.size % 2 == 0) PlayerColor.Red else PlayerColor.Yellow
            )
            _players.value = _players.value + newPlayer
        }
    }

    // Toggle player ready state
    fun toggleReady(player: Player) {
        _players.value = _players.value.map {
            if (it.id == player.id) it.copy(ready = !it.ready) else it
        }
    }

    fun challengePlayer(challengerId: String, opponentId: String) {
        if (opponentId != challengerId) {
            _challenges.value = _challenges.value + (opponentId to challengerId)
        }
    }

    fun acceptChallenge(playerId: String): String? {
        return _challenges.value[playerId]
    }
}


@Composable
fun LobbyScreen(navController: NavController, viewModel: LobbyViewModel = viewModel()) {
    val players by viewModel.players.collectAsState()
    val challenges by viewModel.challenges.collectAsState()
    var enteredPlayerName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedOpponentId by remember { mutableStateOf<String?>(null) }
    val allPlayersReady = players.size == 2 && players.all { it.ready }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lobby", style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = enteredPlayerName,
                onValueChange = { enteredPlayerName = it },
                modifier = Modifier
                    .background(Color.Gray, CircleShape)
                    .padding(8.dp)
            )
            Button(
                onClick = {
                    viewModel.addPlayer(enteredPlayerName.text)
                    enteredPlayerName = TextFieldValue("")
                }
            ) {
                Text("Add Player")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        players.forEach { player ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${player.name} - ${player.color} (${if (player.ready) "Ready" else "Not Ready"})")
                Button(
                    onClick = { viewModel.toggleReady(player) }
                ) {
                    Text(if (player.ready) "Unready" else "Ready")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { if (allPlayersReady) navController.navigate("game") },
            enabled = allPlayersReady
        ) {
            Text("Start Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (players.size > 1) {
            Text("Select an Opponent:")
            DropdownMenuExample(players, selectedOpponentId) { opponentId ->
                selectedOpponentId = opponentId
            }
            Button(
                onClick = {
                    val challenger = players.firstOrNull { it.ready }
                    if (challenger != null && selectedOpponentId != null) {
                        viewModel.challengePlayer(challenger.id, selectedOpponentId!!)
                    }
                },
                enabled = selectedOpponentId != null
            ) {
                Text("Challenge")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        challenges.forEach { (opponentId, challengerId) ->
            Button(
                onClick = {
                    val gameId = viewModel.acceptChallenge(opponentId)
                    if (gameId != null) {
                        navController.navigate("game/$gameId")
                    }
                }
            ) {
                Text("Accept Challenge")
            }
        }
    }
}

// Förhindrar att flera  av samma namn läggs till


// ändrat ready-state förhindrar mutableState tracking. Kommer icke trigga igång UI
// Detta gör att UI inte uppdaterar för att visa en spelares status (Ready/Unready)
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
            modifier = Modifier
                .clickable { expanded = true }
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