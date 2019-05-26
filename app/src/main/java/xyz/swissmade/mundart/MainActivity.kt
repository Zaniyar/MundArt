package xyz.swissmade.mundart

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import br.com.netodevel.circle_video_record.BuilderCameraView
import com.camerakit.CameraKitView
import com.wonderkiln.camerakit.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import com.wonderkiln.camerakit.CameraKitVideo
import com.wonderkiln.camerakit.CameraKitImage
import com.wonderkiln.camerakit.CameraKitError
import com.wonderkiln.camerakit.CameraKitEvent
import com.wonderkiln.camerakit.CameraKitEventListener
import java.security.AccessController.getContext
import android.os.Environment.getExternalStorageDirectory
import android.view.View
import java.io.File
import java.io.FileOutputStream
import android.R.attr.data
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions


class MainActivity : AppCompatActivity() {
    private var cameraKitView: CameraKitView? = null

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

    lateinit var cameraView: CameraView
    var mStatus: Boolean? = false

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        cameraKitView = findViewById(R.id.camera);

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        val isRecording:Boolean = false;

        val button_record:Button = findViewById(R.id.record)

        /*button_record.setOnClickListener {

        }*/

        button_record.setOnClickListener(photoOnClickListener);


        /* Round Camera View old Library
        /**
         * Build CameraView
         */
        cameraView = BuilderCameraView().build(this)

        /**
         * Setup Circle Video Record
         */
        circle_video_record.setup(findViewById(R.id.teich),cameraView)
        //circle_video_record.setRecordTime(2000)
        val button_record:Button = findViewById(R.id.record)
        button_record.setOnClickListener {
            mStatus = !mStatus!!;

            if (mStatus == true) {
                circle_video_record.show()
            } else {
                circle_video_record.hide()
            }

            /**
             * Listener callback video file
             */
            circle_video_record.setVideoListener {
                Log.d("video_path", it.absolutePath)
            }
        }
        */

    }
    private val photoOnClickListener = object : View.OnClickListener {
        override fun onClick(v: View) {
            cameraKitView?.captureVideo(CameraKitView.VideoCallback{ cameraKitView, photo: Any? ->
                val savedPhoto = File(Environment.getExternalStorageDirectory(), "photo.jpg")
                try {
                    val outputStream = FileOutputStream(savedPhoto.getPath())
                    val photo = photo as ByteArray
                    outputStream.write(photo)
                    outputStream.close()
                } catch (e: java.io.IOException) {
                    e.printStackTrace()
                    Log.e("CKDemo", "Exception in photo callback")
                }
            })
        }
    }
    fun onVideo(cameraKitView: CameraKitView) {
        // The File parameter is an MP4 file.
        Toast.makeText(applicationContext,"Hi",Toast.LENGTH_SHORT).show();
    }
    override fun onStart() {
        super.onStart()
        cameraKitView!!.onStart()
    }
    override fun onResume() {
        super.onResume()
        cameraKitView?.onResume()
    }

    override fun onPause() {
        cameraKitView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        cameraKitView?.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
