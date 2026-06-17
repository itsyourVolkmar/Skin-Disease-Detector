package com.example.skindetectorapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.skindetectorapp.ml.SkinDiseaseModel28
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity() {

    lateinit var button: Button  // Gallery
    lateinit var button1: Button  // Camera
    lateinit var button2: Button  // Predict
    lateinit var resultV: TextView
    lateinit var imageV: ImageView
    lateinit var treat_btn: Button
    var bitmap: Bitmap? = null  // Nullable for safety

    private val GALLERY_REQUEST = 100
    private val CAMERA_REQUEST = 101

    // Class names matching your trained model
    private val classNames = arrayOf("Eczema", "Normal", "Psoriasis", "Skin Cancer", "Tinea")

    // Store the latest prediction
    private var latestPrediction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        button = findViewById(R.id.button)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        resultV = findViewById(R.id.resultV)
        imageV = findViewById(R.id.imageV)
        treat_btn = findViewById(R.id.treat_btn)

        // Image Processor: Resize and Normalize for MobileNetV2
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 127.5f))  // Normalize to [-1, 1]
            .build()

        // Pick image from gallery
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, GALLERY_REQUEST)
        }

        // Treat button: Pass prediction to InputActivity
        treat_btn.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra("predicted_disease", latestPrediction ?: "No prediction available. Please predict a disease first.")
            startActivity(intent)
        }

        // Capture image from camera
        button1.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }

        // Run prediction (integrates your inference snippet)
        button2.setOnClickListener {
            bitmap?.let { bmp ->
                try {
                    // Preprocess the image (unchanged)
                    val tensorImage = TensorImage(DataType.FLOAT32)
                    tensorImage.load(bmp)
                    val processedImage = imageProcessor.process(tensorImage)

                    // Load the new model
                    val model = SkinDiseaseModel28.newInstance(this)

                    // Creates inputs for reference (using the preprocessed buffer)
                    val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
                    inputFeature0.loadBuffer(processedImage.buffer)  // Directly use the ByteBuffer from processedImage

                    // Runs model inference and gets result
                    val outputs = model.process(inputFeature0)
                    val outputFeature0 = outputs.outputFeature0AsTensorBuffer

                    // Releases model resources if no longer used
                    model.close()

                    // Interpret results (unchanged)
                    val probabilities = outputFeature0.floatArray
                    val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
                    val predictedClass = classNames[maxIndex]
                    val confidence = probabilities[maxIndex] * 100

                    // Store the prediction
                    latestPrediction = predictedClass

                    if (confidence < 60) {
                        resultV.text = "Low confidence prediction.\nTry a clearer skin image.\n\nTop Guess: $predictedClass (%.2f%%)".format(confidence)
                        return@setOnClickListener
                    }

                    // Display the result
                    resultV.text = "Predicted: $predictedClass\nConfidence: %.2f%%".format(confidence)

                } catch (e: Exception) {
                    resultV.text = "Error during prediction: ${e.message}"
                    latestPrediction = null
                }
            } ?: run {
                resultV.text = "Please select or capture an image first."
                latestPrediction = null
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST -> {
                    val uri = data?.data
                    uri?.let {
                        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                        bitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                        imageV.setImageBitmap(bitmap)
                        resultV.text = ""  // Clear previous result
                        latestPrediction = null  // Reset prediction
                    }
                }
                CAMERA_REQUEST -> {
                    val capturedBitmap = data?.extras?.get("data") as? Bitmap
                    capturedBitmap?.let {
                        bitmap = it
                        bitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                        imageV.setImageBitmap(bitmap)
                        resultV.text = ""  // Clear previous result
                        latestPrediction = null  // Reset prediction
                    }
                }
            }
        }
    }
}