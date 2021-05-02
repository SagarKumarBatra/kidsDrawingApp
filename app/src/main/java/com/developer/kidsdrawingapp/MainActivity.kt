package com.developer.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.nfc.Tag
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Gallery
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.developer.kidsdrawingapp.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var mImageButtonCurrentPaint : ImageButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.drawingView.setSizeForBrush(10.toFloat())

        mImageButtonCurrentPaint = binding.llPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        binding.ibBrush.setOnClickListener{
            showBrushSizeChooseDialog()
        }
        binding.ibGallery.setOnClickListener{
            if(isReadStorageAllowed()) {
                // Run Our Code to get the image from gallery
                val pickPhotoIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)

            }else {
                requestStoragePermission()
            }
        }
        binding.ibUndo.setOnClickListener{
            binding.drawingView.onClickUndo()
        }
        binding.ibSave.setOnClickListener {
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(binding.flDrawingViewContainer)).execute()
            }else {
                requestStoragePermission()
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == GALLERY) {
                try {
                    if(data!!.data != null) {
                        binding.ivBackground.visibility = View.VISIBLE
                        binding.ivBackground.setImageURI(data!!.data)
                    }else {
                        Toast.makeText(this,
                        "Error in Parsing the Image or its Corrupted",
                        Toast.LENGTH_LONG).show()
                    }
                }catch (e:Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooseDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ibSmallBrush)
        smallBtn.setOnClickListener{
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ibMediumBrush)
        mediumBtn.setOnClickListener{
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ibLargeBrush)
        largeBtn.setOnClickListener{
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }
    fun paintClicked(view: View) {
        if(view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()
            binding.drawingView.changeColor(colorTag)
            imageButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())
                ) {
            Toast.makeText(this, "Need Permission to add Background",
            Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this@MainActivity,
                "Permission Granted Now you can read the storage file",
                Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(this@MainActivity,
                        "Oops You just denied the permission",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isReadStorageAllowed() : Boolean {
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas  = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null) {
            bgDrawable.draw(canvas)
        }else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
        }

        private inner class BitmapAsyncTask(val mBitmap : Bitmap) :
        AsyncTask<Any, Void, String>() {

            private lateinit var mProgressDialog : Dialog

            override fun onPreExecute() {
                super.onPreExecute()
                showProgressDialog()
            }

            override fun doInBackground(vararg params: Any?): String {
                var result = ""
                if (mBitmap != null) {
                    try {
                        val bytes = ByteArrayOutputStream()
                        mBitmap.compress(Bitmap.CompressFormat.PNG,
                                90, bytes)
                        val f = File(externalCacheDir!!.absoluteFile.toString() +
                        File.separator + "kidsDrawingApp_" + System.currentTimeMillis() / 1000 +
                        ".png")

                        val fos = FileOutputStream(f)
                        fos.write(bytes.toByteArray())
                        fos.close()
                        result = f.absolutePath
                    }catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return result
            }

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                if(!result!!.isEmpty()) {
                    Toast.makeText(applicationContext, "File Saved Successfully",
                    Toast.LENGTH_LONG).show()
                }else {
                    Toast.makeText(applicationContext, "Something went wrong while saving file",
                            Toast.LENGTH_LONG).show()
                }
                cancelProgressDialog()
                MediaScannerConnection.scanFile(this@MainActivity,
                arrayOf(result), null){
                    path, uri -> val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    shareIntent.type = "image/*"

                    startActivity(
                            Intent.createChooser(
                                    shareIntent, "share"
                            )
                    )
                }
            }
            private fun showProgressDialog() {
                mProgressDialog = Dialog(this@MainActivity)
                mProgressDialog.setContentView(R.layout.dialog_custom_progress)
                mProgressDialog.show()
            }
            private fun cancelProgressDialog() {
                mProgressDialog.dismiss()
            }
        }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }

}