package com.example.mygame

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygame.models.*
import com.example.mygame.utils.EXTRA_BOARD_SIZE
import com.example.mygame.utils.EXTRA_GAME_NAME
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

//Brain of our app
class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG ="MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }

    private lateinit var clRoot:ConstraintLayout
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    //these variables will be set by onCreate which will be invoke by android system
    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private var boardSize: BoardSize =BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //R-> resourse,R.layout to look in directory for the resource , file: activity_main
        setContentView(R.layout.activity_main)

        clRoot=findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)


        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.mi_refresh ->{
                if(memoryGame.getNumMoves()>0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your Current Game?",null,View.OnClickListener {
                        setupBoard()
                    })
                }else {
                    //setup the gaming app ,doing tvNumPairs to LayoutManager again
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode ==  CREATE_REQUEST_CODE && requestCode == Activity.RESULT_OK){
            val customGameName : String? = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null){
                Log.e(TAG,"Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customGameName: String) {
            db.collection("games").document(customGameName).get().addOnSuccessListener {document ->
                val userImageList: UserImageList? = document.toObject(UserImageList::class.java)
                if(userImageList?.images ==null){
                    Log.e(TAG,"Invalid custom game data from Firestore")
                    Snackbar.make(clRoot,"Sorry, we couldn't find any such game,'$gameName'",Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val numCard: Int = userImageList.images.size * 2
                boardSize = BoardSize.getByValue(numCard)
                customGameImages = userImageList.images
                setupBoard()
                gameName = customGameName

            }.addOnFailureListener { exception ->
                Log.e(TAG,"Exception when retrieving game",exception)
            }
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize:RadioGroup =boardSizeView.findViewById(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView,View.OnClickListener {
            //set a new value for the board size
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.HARD
            }
            // navigate user to new activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)

        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize:RadioGroup =boardSizeView.findViewById(R.id.radioGroup)
        when (boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }

        showAlertDialog("Choose new size", boardSizeView,View.OnClickListener {
            //set a new value for the board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.HARD
            }
            setupBoard()
        })
    }

    private fun showAlertDialog(title: String,view: View?,positiveButtonClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("OK"){ _, _ ->
                    positiveButtonClickListener.onClick(null)
                }.show()
    }

    private fun setupBoard(){
        when (boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6 x 4"
                tvNumPairs.text = "Pairs: 0 / 12"
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize,customGameImages)

        adapter = MemoryBoardAdapter(this, boardSize,memoryGame.cards,object : MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter=adapter
        rvBoard.setHasFixedSize(true) // to make application efficient
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth()) //spanCount: how many columns in rv
    }

    private fun updateGameWithFlip(position: Int){
        // Error checking
        if (memoryGame.haveWonGame()){
            //Alert the user of an invalid move
            // it appears at the bottom of the screen
            Snackbar.make(clRoot,"You Already won", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){
            //alert the user of an invalid move
            Snackbar.make(clRoot,"Invalid input", Snackbar.LENGTH_SHORT).show()
            return
        }
        //actual flip over the card
        if( memoryGame.flipCard(position)){
            Log.i(TAG,"Found a Match! Num pair found : ${memoryGame.numPairsFound}")
            val color = ArgbEvaluator().evaluate(
                    memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                    ContextCompat.getColor(this,R.color.color_progress_none),
                    ContextCompat.getColor(this,R.color.color_progress_full),
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot,"You won! congratulation" , Snackbar.LENGTH_LONG).show()
            }
        }
        //how many moves user made
        tvNumMoves.text ="Moves: ${memoryGame.getNumMoves()}"
        // to tell recyclerViewAdapter that context what its showing is changed
        //to update
        adapter.notifyDataSetChanged()
    }
}