package com.example.mygame

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mygame.models.BoardSize
import com.example.mygame.models.MemoryCard
import com.squareup.picasso.Picasso
import kotlin.math.min

class MemoryBoardAdapter(
        private val context: Context,
        private val boardSize: BoardSize,
        private val cards: List<MemoryCard>,
        private val cardClickListener: CardClickListener
        ) :
    //RecyclerView.Adapter -> abstract class
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() { //subclass RecyclerView which is parametries using ViewHolder(is is object which provide access to all the views of one recyclerView Element)
    // responsible for how to create one view of our RecyclerView
    companion object{
        // it is a singleton variable define constants we can access its members directly in containing class
        //like static var. in java
        private const val MARGIN_SIZE =10
        private const val TAG="MemoryBoardAdapter" //better to use class name
    }

    interface CardClickListener{
        fun onCardClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth:Int = parent.width / boardSize.getWidth() - 2* MARGIN_SIZE
        val cardHeight:Int = parent.height / boardSize.getHeight() - 2* MARGIN_SIZE
        val cardSideLength:Int = min (cardWidth,cardHeight)
        val view:View = LayoutInflater.from(context).inflate(R.layout.memory_card,parent, false)
        // setting the dynamic size
        val layoutParams:ViewGroup.MarginLayoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width= cardSideLength
        layoutParams.height= cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }
    // how many views
    override fun getItemCount() = boardSize.numCards // numCards=total no. of elements

    // taking the data from position and bind it into holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard: MemoryCard=cards[position]
            if(memoryCard.isFaceUp){
                if(memoryCard.imageUrl !=null){
                    Picasso.get().load(memoryCard.imageUrl).into(imageButton)
                }else{
                    imageButton.setImageResource(memoryCard.identifier)
                }
            }
            else{
                    imageButton.setImageResource(R.drawable.ic_launcher_background)
            }

            //opacity
            imageButton.alpha = if (memoryCard.isMatched) .4f else 1.0f
            val colorSateList = if(memoryCard.isMatched) ContextCompat.getColorStateList(context,R.color.color_grey) else null
            // to set background or shading in image button
            ViewCompat.setBackgroundTintList(imageButton, colorSateList)
            // here we r notify on click on image button
            imageButton.setOnClickListener{
                Log.i(TAG,"Clicked on position $position")
                cardClickListener.onCardClicked(position)
            }
        }
    }
}
