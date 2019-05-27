package xyz.swissmade.mundart

import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView


class MainActivity : AppCompatActivity() {
    private var cameraView: CameraView? = null
    private var fotoapparat: Fotoapparat? = null
    private lateinit var textMessage: TextView
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


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        cameraView = findViewById(R.id.camera_view)

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val button_record: Button = findViewById(R.id.record)

        /*button_record.setOnClickListener {

        }*/

        button_record.setOnClickListener(photoOnClickListener);

        val cameraView: io.fotoapparat.view.CameraView = findViewById(R.id.camera_view)
        // Real-time contour detection of multiple faces
        val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        val metadata = FirebaseVisionImageMetadata.Builder()
            .setWidth(480) // 480x360 is typically sufficient for
            .setHeight(360) // image recognition
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
            .setRotation(this.getRotationCompensation(this,applicationContext))
            .build()

        val cameraConfiguration = CameraConfiguration(
            pictureResolution = highestResolution(), // (optional) we want to have the highest possible photo resolution
            previewResolution = highestResolution(), // (optional) we want to have the highest possible preview resolution
            previewFpsRange = highestFps(),          // (optional) we want to have the best frame rate
            focusMode = firstAvailable(              // (optional) use the first focus mode which is supported by device
                continuousFocusPicture(),
                autoFocus(),                       // if continuous focus is not available on device, auto focus will be used
                fixed()                            // if even auto focus is not available - fixed focus mode will be used
            ),
            flashMode = firstAvailable(              // (optional) similar to how it is done for focus mode, this time for flash
                autoRedEye(),
                autoFlash(),
                torch(),
                off()
            ),
            antiBandingMode = firstAvailable(       // (optional) similar to how it is done for focus mode & flash, now for anti banding
                auto(),
                hz50(),
                hz60(),
                none()
            ),
            jpegQuality = manualJpegQuality(90),     // (optional) select a jpeg quality of 90 (out of 0-100) values
            sensorSensitivity = lowestSensorSensitivity(), // (optional) we want to have the lowest sensor sensitivity (ISO)
            frameProcessor = { frame -> run {
                    val image = FirebaseVisionImage.fromByteArray(frame.image, metadata)
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
                    }
                    .addOnFailureListener(
                        object : OnFailureListener {
                            override fun onFailure(e: Exception) {
                                // Task failed with an exception
                                // ...
                            }
                        })

                }
            }            // (optional) receives each frame from preview stream
        )


        this.fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,                   // view which will draw the camera preview
            scaleType = ScaleType.CenterCrop,    // (optional) we want the preview to fill the view
            lensPosition = front(),               // (optional) we want back camera
            cameraConfiguration = cameraConfiguration, // (optional) define an advanced configuration
            logger = loggers(                    // (optional) we want to log camera events in 2 places at once
                logcat()                   // ... in logcat
            ),
            cameraErrorCallback = { error -> }   // (optional) log fatal errors
        )

    }

    private val photoOnClickListener = object : View.OnClickListener {
        override fun onClick(v: View) {
            val text = "Foto wurde geschossen!"
            val duration = Toast.LENGTH_SHORT

            val toast = Toast.makeText(applicationContext, text, duration)
            toast.show()
            Log.e("FACE", "Miao")
        }
    }

    override fun onStart() {
        super.onStart()
        fotoapparat?.start()
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getFrontFacingCameraId(cManager: CameraManager): String? {
        for (cameraId in cManager.cameraIdList) {
            val characteristics = cManager.getCameraCharacteristics(cameraId)
            val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
            if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId
        }
        return null
    }
    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }
    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(activity: Activity, context: Context): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager

        val sensorOrientation = cameraManager
            .getCameraCharacteristics(getFrontFacingCameraId(cameraManager))
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
                Log.e("ROTATION", "Bad rotation value: $rotationCompensation")
            }
        }
        return result
    }
}
