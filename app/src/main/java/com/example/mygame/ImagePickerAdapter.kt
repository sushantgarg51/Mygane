package com.example.mygame

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.mygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener{
        fun onPlaceholderClicked()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)
        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }
        //here we listen if user want to select photos
        fun bind(){
            ivCustomImage.setOnClickListener{
                //Launch intent for user to select photos
                imageClickListener.onPlaceholderClicked()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image,parent, false)
        val cardwidth:Int = parent.width / boardSize.getWidth()
        val cardHeight:Int = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardwidth,cardHeight)
        // reference to image
        val layoutParams:ViewGroup.LayoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height= cardSideLength
        return  ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position<imageUris.size){
            holder.bind(imageUris[position])
        }
        else{
            holder.bind()
        }
    }

    override fun getItemCount() = boardSize.getNumPairs()
}
