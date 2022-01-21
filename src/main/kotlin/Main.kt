fun main() {
    val game = IndigoGame()
}

class Player(val hand: MutableList<Card>, val AI: Boolean, var wonLast: Boolean = false) {
    val wonCards = mutableListOf<Card>()

    fun showHandWithNumbers(): String {
        val handList = mutableListOf<String>()
        hand.forEach { handList.add("${hand.indexOf(it) + 1}) $it") }
        return handList.joinToString(" ")
    }
    fun showScore() = wonCards.sumOf { it.point }
}

data class Card(val rank: String, val suit: String) {
    var point: Int = givePointToCard(rank)

    override fun toString(): String {
        return "$rank$suit"
    }
    private fun givePointToCard(rank: String): Int {
        return when (rank) {
            "A", "10", "J", "Q", "K" -> 1
            else -> 0
        }
    }
}

class IndigoGame {
    private var deck = mutableListOf<Card>()
    private val table = mutableListOf<Card>()

    private var exitCommand = false

    private lateinit var currentPlayer: Player
    private lateinit var firstPlayer: Player
    private lateinit var AIplayer: Player
    private lateinit var player: Player

    init {
        buildDeck()
        setGame()
        mainLoop()
    }

    private fun buildDeck() {
        val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val suits = listOf("♠", "♥", "♦", "♣")
        for (suit in suits) {
            for (rank in ranks) {
                deck.add(Card(rank, suit))
            }
        }
        deck.shuffle()
    }

    private fun give6Cards(): MutableList<Card> {
        val sixCards = mutableListOf<Card>()
        repeat(6) {
            sixCards.add(deck.removeLast())
        }
        return sixCards
    }

    private fun setGame() {
        val subDeck = deck.subList(0, 4)
        table.addAll(subDeck)
        deck.removeAll(subDeck)

        player = Player(give6Cards(), false)
        AIplayer = Player(give6Cards(), true)

        println("Indigo Card Game")
        do {
            var bool = true
            println("Play first? (yes/no)")
            val input = readLine()!!.lowercase()
            if (input == "yes") {
                currentPlayer = player
                firstPlayer = player
                bool = false
            }
            if (input == "no") {
                currentPlayer = AIplayer
                firstPlayer = AIplayer
                bool = false
            }
        } while (bool)
        println("Initial cards on the table: ${table.joinToString(" ")}")
    }

    private fun mainLoop() {
        do {
            printTable()
            move(currentPlayer)
        } while (!checkWinGame() && !exitCommand)
        println("Game Over")
    }

    private fun checkWinGame(): Boolean {
        var endgame = false

        // game over when no cards left in hands
        if (deck.size == 0 && player.hand.size == 0 && AIplayer.hand.size == 0) {
            endgame = true

            if (table.isNotEmpty()) {
                // cards to first player if none cards were won
                if (player.wonCards.size == 0 && AIplayer.wonCards.size == 0) {
                    firstPlayer.wonCards.addAll(table)
                } else {
                    // cards to player who won last
                    if (AIplayer.wonLast) {
                        AIplayer.wonCards.addAll(table)
                    } else {
                        player.wonCards.addAll(table)
                    }
                }
            }
            // add 3 points to first player if won cards equal
            if (player.wonCards.size == AIplayer.wonCards.size) {
                firstPlayer.wonCards[0].point += 3
            } else {
                // add 3 points to player with most won cards
                if (player.wonCards.size > AIplayer.wonCards.size) {
                    player.wonCards[0].point += 3
                } else {
                    AIplayer.wonCards[0].point += 3
                }
            }
            printTable()
            printScore()
        }
        return endgame
    }

    private fun changeTurn() = if (currentPlayer == player) currentPlayer = AIplayer else currentPlayer = player

    private fun move(movePlayer: Player) {
        if (movePlayer.AI) {
            val card = chooseCardForAI()
            table.add(card)
            AIplayer.hand.remove(card)
            println("Computer plays $card")
            checkCapture(movePlayer)
            changeTurn()
        } else {
            println("Cards in hand: ${movePlayer.showHandWithNumbers()}")
            do {
                var bool = true
                println("Choose a card to play (1-${movePlayer.hand.size}):")
                val input = readLine()!!
                if (input.matches("\\d".toRegex()) && input.toInt() in 1..movePlayer.hand.size) {
                    val card = movePlayer.hand.removeAt(input.toInt() - 1)
                    table.add(card)
                    bool = false
                }
                if (input == "exit") {
                    exitCommand = true
                    bool = false
                }
            } while (bool)
            if (!exitCommand) {
                checkCapture(movePlayer)
                changeTurn()
            }
        }
        // checking cards in hand and give 6 if empty
        if (movePlayer.hand.size == 0 && deck.size >= 6) movePlayer.hand.addAll(give6Cards())
    }
    private fun findCandidateCards(): MutableList<Card> {
        val candidateCards = mutableListOf<Card>()
        if (table.size >= 1) {
            candidateCards.addAll(AIplayer.hand.filter{ table.last().rank == it.rank })
            candidateCards.addAll(AIplayer.hand.filter{ table.last().suit == it.suit })
        }
        return candidateCards
    }
    private fun chooseCardForAI(): Card {
        val candidateCards = findCandidateCards()
        return when {
            AIplayer.hand.size == 1 -> AIplayer.hand.first()
            candidateCards.size == 1 -> candidateCards.first()
            table.isEmpty() -> noCardsOnTableCard()
            table.isNotEmpty() && candidateCards.isEmpty() -> noCardsOnTableCard()
            candidateCards.size >= 2 -> candidateCardChoose(candidateCards, table.last())
            else -> TODO("I hope the above covers all cases")
        }
    }
    private fun noCardsOnTableCard(): Card {
        val suitMap = AIplayer.hand.groupingBy { it.suit }.eachCount().filter { it.value > 1 }
        val rankMap = AIplayer.hand.groupingBy { it.rank }.eachCount().filter { it.value > 1 }

        val card = if (suitMap.isNotEmpty()) {
            AIplayer.hand.first { it.suit in suitMap.keys }
        } else if (rankMap.isNotEmpty()) {
            AIplayer.hand.first { it.rank in rankMap.keys}
        } else {
            AIplayer.hand.first()
        }
        return card
    }
    private fun candidateCardChoose(candidateCards: MutableList<Card>, tableTop: Card): Card {
        val card = if (candidateCards.count { it.suit == tableTop.suit } >= 2) {
            candidateCards.first { it.suit == tableTop.suit }
        } else if (candidateCards.count { it.rank == tableTop.rank } >= 2) {
            candidateCards.first { it.rank == tableTop.rank }
        } else {
            candidateCards.first()
        }
        return card
    }
    private fun checkCapture(movePlayer: Player) {
        if (table.size >= 2) {
            val lastCard = table.last()
            val preLastCard = table[table.lastIndex - 1]

            if (lastCard.rank == preLastCard.rank || lastCard.suit == preLastCard.suit) {
                println("${if (movePlayer.AI) "Computer" else "Player"} wins cards")
                movePlayer.wonCards.addAll(table)
                table.clear()
                changeWonLast(movePlayer)
                printScore()
            }
        }
    }
    private fun printScore() {
        println("Score: Player ${player.showScore()} - Computer ${AIplayer.showScore()}")
        println("Cards: Player ${player.wonCards.size} - Computer ${AIplayer.wonCards.size}")
    }
    private fun printTable() {
        if (table.size >= 1) {
            println("\n${table.size} cards on the table, and the top card is ${table.last()}")
        } else {
            println("\nNo cards on the table")
        }
    }
    private fun changeWonLast(movePlayer: Player) {
        if (movePlayer == player) {
            player.wonLast = true
            AIplayer.wonLast = false
        } else {
            player.wonLast = false
            AIplayer.wonLast = true
        }
    }
}