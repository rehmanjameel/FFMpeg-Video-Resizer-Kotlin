package org.codebase.ffmpegvideocompression

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.Tag
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import org.codebase.ffmpegvideocompression.callback.FFMpegCallback
import org.codebase.ffmpegvideocompression.dialog.ProgressDialog
import org.codebase.ffmpegvideocompression.dialog.VideoDialog
import org.codebase.ffmpegvideocompression.filespath.FilePickerHelper
import org.codebase.ffmpegvideocompression.tools.OutPutType
import org.codebase.ffmpegvideocompression.tools.VideoResizer
import org.codebase.ffmpegvideocompression.utils.Utils
import java.io.File
import java.lang.Exception

@SuppressLint("SetTextI18n")

class MainActivity : AppCompatActivity(), FFMpegCallback {

    interface ProgressPublish {
        fun onProgress(progress: String)

        fun onDismiss()
    }

    companion object {
        lateinit var onProgress: ProgressPublish

        fun setProgressListener(onProgress: ProgressPublish) {
            Log.e("Progress", "Check progress")
            this.onProgress = onProgress
        }
    }

    private var context: Context? = null

    private var videoPath: String = ""
    private var videoUri: Uri? = null
    lateinit var video: File
    private var videoGallery: File? = null
    private var isGranted = false
    //    private lateinit var radioGroup: RadioGroup
    private lateinit var radioButtons: RadioButton

    private var radioButtonText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.context = this
//        radioGroup = radioGroupId
        //Check for permissions
        checkPermissions()

        //This will copy resources file to storage directory
        setUpResources()

        pickVideoButtonId.setOnClickListener {
            pickVideo()
        }

        //This will resize video in given size
        //Note: Size must be in this format = width:height
        resizeVideoButtonId.setOnClickListener {
            reSizeVideo()
        }
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result ->
                var isPlay = false

                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    if (intent != null) {
                        videoUri = intent.data!!
                        val videoView = videoViewId
                        videoView.visibility = View.VISIBLE
                        videoView.setVideoPath(videoUri.toString())

                        Log.e("Uri0", videoUri.toString())
                        val mediaController = MediaController(this@MainActivity)
                        mediaController.setAnchorView(videoView)
                        videoView.setMediaController(mediaController)

                        videoView.setOnPreparedListener {

                            videoDurationTextId.text = "Duration: ${Utils.milliSecondsToTimer(videoView.duration.toLong())}\n"
                            isPlay = true
                        }

                        videoView.setOnClickListener {
                            isPlay = if (isPlay) {
                                videoView.pause()
                                false
                            } else {
                                videoView.start()
                                true
                            }
                        }
                        videoView.setOnCompletionListener {
                            videoView.pause()
                        }

                        videoView.start()
                        radioGroupId.visibility = View.VISIBLE

                    }
                }
            })

    private fun pickVideo() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "video/*"
//        galleryLauncher.launch(intent)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//        } else {
        if (isGranted) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "video/*"
            galleryLauncher.launch(intent)
            isGranted = true
        } else {
            Log.e("Granted!", "${!isGranted}")
            checkPermissions()
            isGranted = false
        }

//        }
    }

    private fun videoPath() {
        videoPath = FilePickerHelper.getPath(applicationContext, videoUri!!).toString()
        videoGallery = File(videoPath)
        Log.e("Path", videoGallery.toString())

    }

    private fun getRadioButtonText() {
        val radioButtonId: Int = radioGroupId.checkedRadioButtonId
        Log.e("RadioId", radioButtonId.toString())

        if (radioButtonId != -1) {
            radioButtons = findViewById(radioButtonId)
            radioButtonText = radioButtons.text.toString()
            Log.e("Radiotext", radioButtonText)
        }
    }

    private fun reSizeVideo() {
        //Function to get the VideoPath in File format
        //First kill previous running task if in process
        stopRunningProcess()
        if (videoUri != null) {
            videoPath()

            getRadioButtonText()
            if (!isRunning()) {
                Log.e("Radio", radioButtonText)

                VideoResizer.with(context!!)
                    .setFile(videoGallery!!)
                    .setSize(radioButtonText) //320 x 480
                    .setOutPutPath(Utils.outPath + "video")
                    .setOutPutFileName("resized_${System.currentTimeMillis()}.mp4")
                    .setCallBack(this)
                    .resize()

                ProgressDialog.show(supportFragmentManager, VideoResizer.TAG)
            } else {
                showInProgressToast()
            }
        } else {
            Toast.makeText(context, "Please pick the video first", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissions() {
        //Ask for permissions
        val externalStorageReadPermission: Int = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_EXTERNAL_STORAGE)
        val externalStorageWritePermission: Int = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_EXTERNAL_STORAGE)
        val listPermissionNeeded: ArrayList<String> = ArrayList()

        if (externalStorageReadPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (externalStorageWritePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionNeeded.toTypedArray(), 1)
        } else {
            isGranted = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> {
                val permissionMap : HashMap<String, Int> = HashMap()

                // Initialize the map with permissions
                permissionMap[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                permissionMap[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED

                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices) {
                        permissionMap[permissions[i]] = grantResults[i]
                    }
                    if (permissionMap[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED &&
                        permissionMap[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {
                        isGranted = true

                        pickVideo()
                        Log.d("Permissions", "All permissions granted")
                    } else {
                        isGranted = false
                        Log.d("Permissions", "Some permissions not granted ask again")

                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            showDialogOK("Storage Permission required for this app") { dialog, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> checkPermissions()
                                    DialogInterface.BUTTON_NEGATIVE -> {
                                        dialog.dismiss()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(this, "Go to settings and enable permissions",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    //Dialog for permissions if permission not granted
    private fun showDialogOK(message: String, okListener: DialogInterface.OnClickListener) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

    override fun onProgress(progress: String) {
        Log.i("Tag", "Running: $progress")

        onProgress.run {
            onProgress(progress)
        }
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show()

        //Show preview of outputs for after checking type of media

        when (type) {
            OutPutType.TYPE_VIDEO -> VideoDialog.show(supportFragmentManager, convertedFile)
        }
    }

    override fun onFailure(error: Exception) {
        error.printStackTrace()

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Error")
        builder.setMessage("${error.message}")
        builder.setPositiveButton("Ok") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.create().show()

        onProgress.run {
            onDismiss()
        }
    }

    override fun onNotAvailable(error: Exception) {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Not available error")
        builder.setMessage("${error.message}")
        builder.setPositiveButton("Ok") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.create().show()

        onProgress.run {
            onDismiss()
        }
    }

    override fun onFinish() {
        onProgress.run {
            onDismiss()
        }
    }

    private fun setUpResources() {
        //Copy Video from resources to storage directory

        video = Utils.copyFileToExternalStorage(R.raw.video2, "video2.mp4", applicationContext)
        Log.e("Video0", video.path)

    }

    private fun isRunning(): Boolean {
        return FFmpeg.getInstance(this).isFFmpegCommandRunning
    }

    private fun stopRunningProcess(): Boolean {
        return FFmpeg.getInstance(this).killRunningProcesses()
    }

    private fun showInProgressToast() {
        Toast.makeText(
            this, "Operation already in progress! Try again in a while.",
            Toast.LENGTH_SHORT).show()
    }
}