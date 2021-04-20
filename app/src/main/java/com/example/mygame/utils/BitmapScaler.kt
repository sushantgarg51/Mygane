package com.example.mygame.utils

import android.graphics.Bitmap

object BitmapScaler {
    //Scale and maintain ratio given a desired width
    //BitmapScaler.scaleToFitWidth(bitmap, 100);
    fun scaleToFitWidth(b: Bitmap , width: Int) :Bitmap{
        val factor:Float =width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b,width,(b.height * factor).toInt(), true)
    }

    //Scale and maintain aspect ratio given a desired height
    fun scaleToFitHeight(b: Bitmap , height: Int): Bitmap{
        val factor:Float = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.height * factor).toInt(),height, true)
    }
}
