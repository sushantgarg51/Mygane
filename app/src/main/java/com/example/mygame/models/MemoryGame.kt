package com.example.mygame.models

import com.example.mygame.utils.DEFAULT_ICONS

// its gonna take in the board size and creating all the cards in the game
class MemoryGame(private val boardSize: BoardSize,
                private  val customImages: List<String>?

) {


    val cards : List<MemoryCard>
    var numPairsFound = 0

    private var numcardFlips =0
    private var indexOfSelectedCard: Int? =null

    init{
        if(customImages == null){
            val chosenImages: List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it) }
        }else{
            val randomizedImages : List<String> = (customImages+customImages).shuffled()
            cards = randomizedImages.map{ MemoryCard(it.hashCode(), it) }
        }
    }
    //game logic
    fun flipCard(position: Int) : Boolean{
        numcardFlips++
        val card:MemoryCard = cards[position]
        //3 cases:
        //0 cards previously flipped over => flip over the selected card
        //1 card previously flipped over => flip over the selected card + check if the images match
        //2 cards previously flipped over => restore cards + flip over the selected card
        var foundMatch = false
        if(indexOfSelectedCard == null){
            // 0 or 2 cards previously flipped over
            restoreCard()
            indexOfSelectedCard = position
        }
        else{
            //exactly 1 card previously flipped over
                //check the images are identical or not
           foundMatch =  checkForMatch(indexOfSelectedCard!!, position)
            indexOfSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int) : Boolean{
        //check if to cards match or not
        if(cards[position1].identifier != cards[position2].identifier){
            return false
        }
        // if these card matched , we update the state
        cards[position1].isMatched =true
        cards[position2].isMatched =true
        numPairsFound++
        return true

    }

    private fun restoreCard(){
        for (card:MemoryCard in cards){
            if (!card.isMatched){
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numcardFlips / 2
    }
}