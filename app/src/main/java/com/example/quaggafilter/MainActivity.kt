package com.example.quaggafilter

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val RESULT_LOAD_IMAGE = 0
    private val REQUEST_TAKE_PHOTO = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.load_image_button).setOnClickListener {
            loadImage()
        }
        findViewById<Button>(R.id.capture_image_button).setOnClickListener {
            //captureImage()
        }

    }

    fun loadImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, RESULT_LOAD_IMAGE)

    }

    fun captureImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_TAKE_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null){

            val imageUri = data.data

            val intent = Intent(this, EditorActivity::class.java)
            intent.putExtra("imageUri", imageUri)
            startActivity(intent)

        }

    }
}