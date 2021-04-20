package com.example.mygame.models

data class MemoryCard (
    //obj. -> list out every attribute of memory card
    //capture the identifier which represents the uniqueness of memory icon
    val identifier : Int ,
    val imageUrl: String? = null,
    var isFaceUp : Boolean = false ,
    var isMatched: Boolean =false
)