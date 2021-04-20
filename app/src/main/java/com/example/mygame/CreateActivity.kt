package com.example.mygame

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ImageDecoder
import android.icu.number.Scale
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygame.models.BoardSize
import com.example.mygame.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE=655
        private const val READ_PHOTO_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val READ_EXTERNAL_PHOTO_CODE=248
        private const val MIN_GAME_NAME_LENGTH =3
        private const val MAX_GAME_NAME_LENGTH =14
    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImageRequired = -1
    //uri - uniform resource identifier or uri defining directory of user photo
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db =Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        //to select image from user phone(3 lines)
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        //to go back to main activivty
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // to pull data from intent
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImageRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics ( 0 / $numImageRequired)"

        btnSave.setOnClickListener {
            saveDataToFirebase()
        }


        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled =shouldEnableSaveButton()
            }

        })

        adapter = ImagePickerAdapter(this,chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceholderClicked() {
                if (isPermissionGranted(this@CreateActivity, READ_PHOTO_PERMISSION)){
                launchIntentForPhotos()
                }else{
                    requestPermission(this@CreateActivity, READ_PHOTO_PERMISSION,READ_EXTERNAL_PHOTO_CODE)
                }
            }
        })
        rvImagePicker.adapter=adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }



    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if(requestCode== READ_EXTERNAL_PHOTO_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }else{
                Toast.makeText(this,"In oder to create a custom game,you need to provide access to your photos",Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){ //home id is defined within the android system
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null){
            Log.w(TAG,"Did not get back from the launched activity,user likely to cancel the flow")
            return
        }
        val selectedUri:Uri? = data.data
        val clipData:ClipData? = data.clipData
        if(clipData != null){
            Log.i(TAG,"clipData numImages ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount){
                val clipItem:ClipData.Item = clipData.getItemAt(i)
                if(chosenImageUris.size< numImageRequired){
                    chosenImageUris.add(clipItem.uri)
                }
            }
        }else if(selectedUri != null){
            Log.i(TAG,"data:$selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "choose pics (${chosenImageUris.size} / $numImageRequired)"
        btnSave.isEnabled= shouldEnableSaveButton()
    }

    private fun saveDataToFirebase() {

        Log.i(TAG,"saveDataToFirebase")
        // to disable button after saving
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        //check that we're not over writing someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                        .setTitle("Name taken")
                        .setMessage("A game already exixts with the name '$customGameName'")
                        .setPositiveButton("OK",null)
                        .show()
                btnSave.isEnabled = true
            }else{
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener{ exception ->
            Log.e(TAG, "Encountered error while saving memory game",exception)
            Toast.makeText(this,"Encountered error while saving memory game",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }


    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for ((index:Int,photoUri:Uri) in chosenImageUris.withIndex()){
            // iBA will be used to save data in firebase
            //gIBA method is used for down grading the quality of photos
            val imageByteArray = getImageByteArray(photoUri)
            //file path of where the image is stored in firebase
            val firePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference: StorageReference = storage.reference.child(firePath)
            photoReference.putBytes(imageByteArray)
                    //lambda block
                    .continueWith{ photoUploadTask ->
                        Log.i(TAG,"Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                        //download url
                        photoReference.downloadUrl
                    }.addOnCompleteListener{downloadUrlTask ->
                        if(!downloadUrlTask.isSuccessful){
                            Log.e(TAG,"Exception with Firebase storage",downloadUrlTask.exception)
                            Toast.makeText(this,"Failed to upload image",Toast.LENGTH_SHORT).show()
                            didEncounterError = true
                            return@addOnCompleteListener
                        }
                        if (didEncounterError){
                            pbUploading.visibility = View.GONE
                            return@addOnCompleteListener
                        }
                        val downloadUrl : String = downloadUrlTask.result.toString()
                        //track of all the images uploaded so far
                        uploadedImageUrls.add(downloadUrl)
                        pbUploading.progress = uploadedImageUrls.size*100 / chosenImageUris.size
                        Log.i(TAG,"Finished uploading $photoUri, num uploaded ${uploadedImageUrls.size}")
                        if(uploadedImageUrls.size==chosenImageUris.size){
                            // sending images and game name to firestore
                            handleAllImagesUploaded(gameName, uploadedImageUrls)
                        }
                    }
        }
    }

    private fun handleAllImagesUploaded(
            gameName: String,
            imageUrls: MutableList<String>) {

        db.collection("games").document(gameName)
                .set(mapOf("images" to imageUrls))
                .addOnCompleteListener { gameCreationTask ->
                    pbUploading.visibility = View.GONE
                    if (!gameCreationTask.isSuccessful) {
                        Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }
                    Log.i(TAG,"Successfully created game $gameName")
                    AlertDialog.Builder(this)
                            .setTitle("Upload complete! Let's play your game '$gameName'")
                            .setPositiveButton("OK"){_, _ ->
                                val resultData = Intent()
                                resultData.putExtra(EXTRA_GAME_NAME, gameName)
                                setResult(Activity.RESULT_OK,resultData)
                                finish()
                            }.show()
                }
    }

    // quality of image
    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap:Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source:ImageDecoder.Source = ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        }
        else{
            MediaStore.Images.Media.getBitmap(contentResolver,photoUri)
        }
        Log.i(TAG,"Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap,250)
        Log.i(TAG,"Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()

    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != numImageRequired){
            return false
        }
        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        startActivityForResult(Intent.createChooser(intent,"choose pics"),PICK_PHOTO_CODE)
    }
}