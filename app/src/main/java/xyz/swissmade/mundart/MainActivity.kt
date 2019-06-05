package xyz.swissmade.mundart

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.annotation.WorkerThread
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.Frame
import com.otaliastudios.cameraview.FrameProcessor
import com.otaliastudios.cameraview.Size
import androidx.core.view.ViewCompat.getRotation
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark


class MainActivity : AppCompatActivity() {

    private lateinit var textMessage: TextView
    private lateinit var camera:CameraView
    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                textMessage.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                textMessage.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                textMessage.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        camera = findViewById<CameraView>(R.id.camera)
        camera.setLifecycleOwner(this)

        val realTimeOpts1 = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .build()
        val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .build()

        FirebaseApp.initializeApp(this);


        camera.addFrameProcessor { frame ->
            val data = frame.data
            val rotation = frame.rotation
            val time = frame.time
            val size = frame.size
            val format = frame.format
            val metadata = FirebaseVisionImageMetadata.Builder()
                .setWidth(480) // 480x360 is typically sufficient for
                .setHeight(360) // image recognition
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(3)
                .build()
            val image = FirebaseVisionImage.fromByteArray(data, metadata)
            val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(realTimeOpts)

            val result = detector.detectInImage(image)
                .addOnSuccessListener { faces ->
                    // Task completed successfully
                    // ...
                    Log.e("FACE", faces.toString())

                    for (face in faces) {
                        val bounds = face.boundingBox
                        val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                        val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                        // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                        // nose available):
                        val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                        leftEar?.let {
                            val leftEarPos = leftEar.position
                        }

                        // If contour detection was enabled:
                        val leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).points
                        val upperLipBottomContour = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).points

                        // If classification was enabled:
                        if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            val smileProb = face.smilingProbability
                        }
                        if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            val rightEyeOpenProb = face.rightEyeOpenProbability
                        }

                        // If face tracking was enabled:
                        if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                            val id = face.trackingId
                        }
                    }
        }.addOnFailureListener(
                    object : OnFailureListener {
                        override fun onFailure(e: Exception) {
                            // Task failed with an exception
                            // ...
                            Log.e("M", e.toString())
                        }
                    })
    }}

}
