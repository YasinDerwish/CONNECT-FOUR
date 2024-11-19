package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
//test
data class Player(val name: String, val color: String)

class GameEngine {
    private val board = Array(6) {Array(7) { ""} }
    fun getBoard() = board

    fun makeMove(column: Int, player : Player): Boolean{
        for(row in board.indices.reversed()) {
            if(board[row][column].isEmpty()) {
                board[row][column] = player.color
                return true
            }
        }
        return false
    }
    fun checkWin(): Boolean{
        return false
    }
    fun isDraw(): Boolean{
        return false
    }
    fun reset(){
        for(row in board){
            row.fill("")
        }
    }
}
//test
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
               ConnectFourGame()

                }
            }
        }
    }


@Composable
fun ConnectFourGame() {
    val rows = 6
    val columns = 7
    val gameEngine = remember{ GameEngine() }
    var currentPlayer by remember { mutableStateOf("Red")}
    var board by remember {mutableStateOf(gameEngine.getBoard())}
    var gameResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = gameResult ?: "Current Player: $currentPlayer",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        for(row in board.indices){
            Row{
            for(col in board[row].indices) {
                Disc(
                    color = when (board[row][col]) {
                        "Red" -> Color.Red
                        "Yellow" -> Color.Yellow
                        else -> Color.Gray
                    },
                    onClick = {
                        if (gameResult == null && gameEngine.makeMove(col, Player("Player", currentPlayer))) {
                            board = gameEngine.getBoard()
                            if(gameEngine.checkWin()) {
                                gameResult = "$currentPlayer wins!"
                            }
                            else if (gameEngine.isDraw()) {
                                gameResult = "It's a draw!"
                            }
                            else {
                                currentPlayer = if (currentPlayer == "Red") "Yellow" else "Red"

                            }
                        }
                    }
                )
            }
            }
        }
    }
}
@Composable
fun ConnectFourGame() {

}
@Composable
fun Disc(color: Color, onClick: () -> Unit){
    Box (
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .background(color,shape = CircleShape)
            .clickable(onClick = onClick)
    )
}
@Preview(showBackground = true)
@Composable
fun ConnectFourGamePreview() {
    MyApplicationTheme {
        ConnectFourGame ()
    }
}