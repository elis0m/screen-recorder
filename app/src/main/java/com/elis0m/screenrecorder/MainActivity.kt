package com.elis0m.screenrecorder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioManager
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

    private var screenDensity: Int = 0
    private var projectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjectionCallback: MediaProjectionCallback? = null
    private var audioManager: AudioManager? =null

    private val permissions: Array<String> = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO)

    private var status: Boolean = false
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
            if (status) {
                setStatus(false)

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

        recordBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                    ActivityCompat.requestPermissions(this, permissions, 0)
                } else {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION)
                }
            } else {
                startRecording()
            }
        }

        stopBtn.setOnClickListener {
            stopRecording()
        }

        resumeToggleBtn.setOnClickListener {
            Log.d("resumeToggleBtn", "")

            if ((it as ToggleButton).isChecked) {
                mediaRecorder!!.pause()
            } else {
                mediaRecorder!!.resume()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> (
                    if (grantResults.isNotEmpty() && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        startRecording()
                    } else {
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

    private fun setStatus(stat: Boolean) {
        if (stat) {
            status = true
            recordingButtons.visibility = View.VISIBLE
            recordBtn.visibility = View.GONE
        } else {
            status = false
            recordingButtons.visibility = View.GONE
            recordBtn.visibility = View.VISIBLE
        }
    }

    private fun startRecording() {
        Log.d("startRecording", "")
        initRecorder()
        shareScreen()
        setStatus(true)
    }

    private fun stopRecording() {
        Log.d("stopRecording", "mediaRecorder stop")
        mediaRecorder!!.stop()
        mediaRecorder!!.reset()
        stopScreenRecord()
        timeSwapBuffer != timeInMilliseconds
        customHandler.removeCallbacks(updateTimerThread)

        setStatus(false)

        Log.d("stopRecording", "Video Uri : $output")
        videoView.visibility = View.VISIBLE
        videoView.setVideoURI(Uri.parse(output))
        videoView.start()
    }

    private fun shareScreen() {
        Log.d("shareScreen", "")
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
        Log.d("onActivityResult", "")
        if (requestCode != REQUEST_CODE) return

        if (resultCode != Activity.RESULT_OK) {
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

    private fun checkMicRecording(): Boolean {
        // check that microphone exists
        // TODO: Add a function that can Choose whether to include microphone or use mute mode
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?

        val inputDeviceList = audioManager!!.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val outputDeviceList = audioManager!!.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        Log.d("containMicRecording", "inputDeviceList : $inputDeviceList / outputDeviceList : $outputDeviceList")
        Log.d("containMicRecording return value", "${inputDeviceList.isNotEmpty()}")

        return inputDeviceList.isNotEmpty()
    }

    @SuppressLint("SimpleDateFormat")
    private fun initRecorder() {
        output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .toString() + StringBuilder("/")
                .append("Record_")
                .append(SimpleDateFormat("yyyyMMddhhmmss").format(Date()))
                .append(".mp4")
                .toString()

        try {
            // You should check MediaRecorder state transition. It cannot be reordered.
            if (checkMicRecording()) mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)

            // CamcorderProfile.QUALITY_HIGH 사용할 경우 캠이 연결되어 있지 않으면 에러남
//            mediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (checkMicRecording()) mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
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
}