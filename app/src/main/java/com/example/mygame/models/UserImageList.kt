package com.example.mygame.models

import com.google.firebase.firestore.PropertyName

data class UserImageList (
   @PropertyName("images") val images: List<String>? = null
)
