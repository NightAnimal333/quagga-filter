package com.example.quaggafilter

import android.R.attr.name
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import java.io.OutputStream

class EditorActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap
    private lateinit var editedBitmap: Bitmap

    private lateinit var imageView: ImageView
    private lateinit var saveChangesButton: Button
    private lateinit var applyChangesButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var binarySwitch: SwitchMaterial
    private lateinit var grayscaleSwitch: SwitchMaterial

    val brightnessThreshold: Float = 50f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val imageUri: Uri? = intent.getParcelableExtra<Uri>("imageUri")
        imageView = findViewById<ImageView>(R.id.imageView)!!
        saveChangesButton = findViewById<Button>(R.id.save_changes_button)
        applyChangesButton = findViewById<Button>(R.id.apply_changes_button)
        seekBar = findViewById<SeekBar>(R.id.seekBar)
        binarySwitch = findViewById<SwitchMaterial>(R.id.binary_switch)
        grayscaleSwitch = findViewById<SwitchMaterial>(R.id.grayscale_switch)

        var useListeners: Boolean = true

        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)!!
            .copy(Bitmap.Config.ARGB_8888, true);
        editedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        imageView.setImageBitmap(editedBitmap)



        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                if (useListeners){
                    //  useListeners helps to avoid recursive Listener triggers when resetting
                    useListeners = false
                    grayscaleSwitch.reset()
                    binarySwitch.reset()
                    useListeners = true

                    GlobalScope.launch(Dispatchers.Main) {
                        setEnableControls(false)
                        GlobalScope.async(Dispatchers.Default) {
                            applyBrightness(p0!!.progress.toFloat(), false)
                        }.await()
                        setEnableControls(true)

                    }
                    applyChangesButton.isEnabled = true
                }

            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })



        grayscaleSwitch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {

                if (useListeners){
                    //  useListeners helps to avoid recursive Listener triggers when resetting
                    useListeners = false
                    binarySwitch.reset()
                    seekBar.reset()
                    useListeners = true

                    GlobalScope.launch(Dispatchers.Main) {
                        setEnableControls(false)
                        withContext(Dispatchers.Default) {
                            applyGrayscale(isChecked, false)
                        }
                        setEnableControls(true)
                    }
                    applyChangesButton.isEnabled = true
                }

            }
        })



        binarySwitch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {

                if (useListeners){
                    //  useListeners helps to avoid recursive Listener triggers when resetting
                    useListeners = false
                    grayscaleSwitch.reset()
                    seekBar.reset()
                    useListeners = true

                    GlobalScope.launch(Dispatchers.Main) {
                        setEnableControls(false)
                        withContext(Dispatchers.Default) {
                            applyBinary(isChecked, false)
                        }
                        setEnableControls(true)
                    }
                    applyChangesButton.isEnabled = true
                }

            }
        })



        saveChangesButton.setOnClickListener {


            val resolver: ContentResolver = this.contentResolver

            GlobalScope.launch(Dispatchers.Main) {
                setEnableControls(false)

                withContext(Dispatchers.IO){
                    val saved: Boolean
                    val fos: OutputStream

                    val contentValues = ContentValues()
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/QuaggaFilter")
                    val imgUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = resolver.openOutputStream(imgUri!!)!!

                    saved = editedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    fos.close();
                }



                setEnableControls(true)

                seekBar.reset()
                grayscaleSwitch.reset()
                binarySwitch.reset()

                saveChangesButton.isEnabled = false


            }


        }



        applyChangesButton.setOnClickListener {



            GlobalScope.launch(Dispatchers.Main) {
                setEnableControls(false)

                withContext(Dispatchers.Main){
                    applyBrightness(seekBar.progress.toFloat(), true)
                    seekBar.progress = 50

                    applyGrayscale(grayscaleSwitch.isChecked, true)
                    applyBinary(binarySwitch.isChecked, true)


                }

                setEnableControls(true)

                seekBar.reset()
                grayscaleSwitch.reset()
                binarySwitch.reset()

                applyChangesButton.isEnabled = false
                saveChangesButton.isEnabled = true
            }

        }

    }

    private fun setEnableControls(state: Boolean) {

        seekBar.isEnabled = state
        grayscaleSwitch.isEnabled = state
        binarySwitch.isEnabled = state

    }

    private fun applyGrayscale(enabled: Boolean, saveChanges: Boolean){
        val cm: ColorMatrix
        if (enabled){
            cm = ColorMatrix(
                floatArrayOf(
                    0.33f, 0.33f, 0.33f, 0f, 0f,
                    0.33f, 0.33f, 0.33f, 0f, 0f,
                    0.33f, 0.33f, 0.33f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        } else {
            cm = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        val paint: Paint = Paint()

        paint.colorFilter = ColorMatrixColorFilter(cm)

        if (saveChanges) {
            val canvas = Canvas(editedBitmap)
            canvas.drawBitmap(editedBitmap, 0f, 0f, paint)

            imageView.invalidate()
        } else {
            imageView.colorFilter = ColorMatrixColorFilter(cm)
        }
    }

    private fun applyBinary(enabled: Boolean, saveChanges: Boolean){
        val cm: ColorMatrix
        if (enabled){
            cm = ColorMatrix(
                floatArrayOf(
                    85f, 85f, 85f, 0f, -128f*255f,
                    85f, 85f, 85f, 0f, -128f*255f,
                    85f, 85f, 85f, 0f, -128f*255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        } else {
            cm = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        val paint: Paint = Paint()

        paint.colorFilter = ColorMatrixColorFilter(cm)

        if (saveChanges) {
            val canvas = Canvas(editedBitmap)
            canvas.drawBitmap(editedBitmap, 0f, 0f, paint)

            imageView.invalidate()
        } else {
            imageView.colorFilter = ColorMatrixColorFilter(cm)
        }
    }

    private fun applyBrightness(progress: Float, saveChanges: Boolean) {

        //IMPLEMENTATION 1: Threshold is a percent value:
//        val changeBy: Float = 0.01f * progress + 0.5f

//        val cm = ColorMatrix(
//                floatArrayOf(
//                        changeBy, 0f, 0f, 0f, 0f,
//                        0f, changeBy, 0f, 0f, 0f,
//                        0f, 0f, changeBy, 0f, 0f,
//                        0f, 0f, 0f, 1f, 0f
//                )
//        )

        //IMPLEMENTATION 2: Threshold is an absolute value:
        val changeBy: Float = ((2 * brightnessThreshold) * progress/100) - brightnessThreshold

        val cm = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, changeBy,
                0f, 1f, 0f, 0f, changeBy,
                0f, 0f, 1f, 0f, changeBy,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint: Paint = Paint()

        paint.colorFilter = ColorMatrixColorFilter(cm)

        if (saveChanges) {
            val canvas = Canvas(editedBitmap)
            canvas.drawBitmap(editedBitmap, 0f, 0f, paint)

            imageView.invalidate()
        } else {
            imageView.colorFilter = ColorMatrixColorFilter(cm)
        }


    }
}

private fun SeekBar.reset() {
    setProgress(50)
}

private fun SwitchMaterial.reset() {
    setChecked(false)
}
