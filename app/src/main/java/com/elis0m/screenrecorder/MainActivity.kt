package com.elis0m.screenrecorder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    var state: Boolean = false

    private var screenDensity: Int = 0
    private var projectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjectionCallback: MediaProjectionCallback? = null

    private lateinit var customHandler: Handler
    private var startTime = 0L
    private var timeInMilliseconds = 0L
    private var timeSwapBuffer = 0L
    private var updatedTime = 0L

    val updateTimerThread: Runnable = run {
        Runnable {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime
            updatedTime = timeSwapBuffer + timeInMilliseconds

            recordingTimeView.text = String.format("%02d:%02d:%02d", (updatedTime / 60000) % 60, updatedTime / 1000 % 60, updatedTime % 1000/10)
            customHandler.postDelayed(updateTimerThread, 0)
        }
    }

    companion object {
        private const val REQUEST_CODE = 1000
        private const val REQUEST_PERMISSION = 1001
        private var DISPLAY_WIDTH = 1920
        private var DISPLAY_HEIGHT = 1080
    }

    inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.d("MediaProjectionCallback", "onStop")
            if (recordToggleBtn.isChecked) {
                recordToggleBtn.isChecked = false
                mediaRecorder!!.stop()
                mediaRecorder!!.reset()
                timeSwapBuffer != timeInMilliseconds
                customHandler.removeCallbacks(updateTimerThread)
            }
            mediaProjection = null
            stopScreenRecord()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        customHandler = Handler()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenDensity = metrics.densityDpi
        DISPLAY_HEIGHT = metrics.heightPixels
        DISPLAY_WIDTH = metrics.widthPixels

        mediaRecorder = MediaRecorder()
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        recordToggleBtn.setOnClickListener { v ->
            if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                    recordToggleBtn.isChecked = false
                    val permissions = arrayOf(
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    ActivityCompat.requestPermissions(this, permissions, 0)
                } else {
                    ActivityCompat.requestPermissions(this,
                            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.RECORD_AUDIO), REQUEST_PERMISSION)
                }
            } else {
                Log.d("onCreate", "startRecording")
                startRecording(v)
            }
        }

/*
        startBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val permissions = arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                ActivityCompat.requestPermissions(this, permissions, 0)
            } else {
                startAudioRecording()
            }
        }

        stopBtn.setOnClickListener {
            stopAudioRecording()
        }
*/

/*
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenDensity = metrics.densityDpi

        mediaRecorder = MediaRecorder()
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        DISPLAY_HEIGHT = metrics.heightPixels
        DISPLAY_WIDTH = metrics.widthPixels

        recordToggleBtn.setOnClickListener{v ->
            if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                    recordToggleBtn.isChecked = false
                    val permissions = arrayOf(
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    ActivityCompat.requestPermissions(this, permissions, 0)
                } else {
                    ActivityCompat.requestPermissions(this,
                            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.RECORD_AUDIO), REQUEST_PERMISSION)                }
            }
            else {
                startRecording(v)
            }
        }
*/
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION -> (
                    if (grantResults.isNotEmpty() && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        startRecording(recordToggleBtn)
                    } else {
                        recordToggleBtn.isChecked = false
                        Snackbar.make(rootLayout, "Permissions", Snackbar.LENGTH_INDEFINITE)
                                .setAction("ENABLE") {
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_APPLICATION_SETTINGS
                                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                                    intent.data = Uri.parse("package:$packageName")
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                    startActivity(intent)
                                }.show()
                    }
                    )
        }
    }

    private fun startRecording(v: View?) {
        if ((v as ToggleButton).isChecked) {
            Log.d("startRecording", "initRecorder")
            initRecorder()
            shareScreen()
        } else {
            Log.d("startRecording", "mediaRecorder stop")
            mediaRecorder!!.stop()
            mediaRecorder!!.reset()
            stopScreenRecord()
            timeSwapBuffer != timeInMilliseconds
            customHandler.removeCallbacks(updateTimerThread)

            Log.d("startRecording", "Video Uri : $output")
            videoView.visibility = View.VISIBLE
            videoView.setVideoURI(Uri.parse(output))
            videoView.start()
        }
    }

    private fun shareScreen() {
        if (mediaProjection == null) {
            startActivityForResult(projectionManager!!.createScreenCaptureIntent(), REQUEST_CODE)
            return
        }
        virtualDisplay = createVirtualDisplay()
        mediaRecorder!!.start()
        startTime = SystemClock.uptimeMillis()
        customHandler.postDelayed(updateTimerThread, 0)
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection!!.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder?.surface, null, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) return

        if (resultCode != Activity.RESULT_OK) {
            recordToggleBtn.isChecked = false
            Toast.makeText(this, "screen cast permission denied", Toast.LENGTH_LONG).show()
            return
        }

        mediaProjectionCallback = MediaProjectionCallback()
        mediaProjection = data?.let { projectionManager!!.getMediaProjection(resultCode, it) }
        mediaProjection!!.registerCallback(mediaProjectionCallback, null)
        virtualDisplay = createVirtualDisplay()
        mediaRecorder!!.start()
        startTime = SystemClock.uptimeMillis()
        customHandler.postDelayed(updateTimerThread, 0)
    }

    @SuppressLint("SimpleDateFormat")
    private fun initRecorder() {
        try {
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)

            output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + StringBuilder("/")
                    .append("Record_")
                    .append(SimpleDateFormat("yyyyMMddhhmmss").format(Date()))
                    .append(".mp4")
                    .toString()

            // CamcorderProfile.QUALITY_HIGH 사용할 경우 캠이 연결되어 있지 않으면 에러남
//            mediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder!!.setVideoEncodingBitRate(4 * 1000000) // 4Mbps
            mediaRecorder!!.setVideoFrameRate(30) // 30fps
            mediaRecorder!!.setOutputFile(output)
            mediaRecorder!!.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)

            mediaRecorder!!.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopScreenRecord() {
        if (virtualDisplay == null) return
        virtualDisplay!!.release()
        destroyMediaProjection()
    }

    private fun destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection!!.unregisterCallback(mediaProjectionCallback)
            mediaProjection!!.stop()
            mediaProjection = null
            timeSwapBuffer != timeInMilliseconds
            customHandler.removeCallbacks(updateTimerThread)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyMediaProjection()
    }

    /* 음성 녹음 */
    private fun startAudioRecording() {
        val fileName: String = "Record_" + SimpleDateFormat("yyyyMMddhhmmss").format(Date()).toString() + ".mp3"
        output = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        mediaRecorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(this, "Recording Start !", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopAudioRecording() {
        if (state) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            state = false
            Toast.makeText(this, "Recording Stop !", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No Recording State", Toast.LENGTH_SHORT).show()
        }
    }
}