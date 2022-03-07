package org.codebase.ffmpegvideocompression.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.nfc.Tag
import android.os.Bundle
import android.widget.MediaController
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_video_preview.view.*
import org.codebase.ffmpegvideocompression.R
import org.codebase.ffmpegvideocompression.utils.Utils
import java.io.File

@SuppressLint("SetTextI18n")

class VideoDialog: DialogFragment() {
    companion object {
        val Tag = VideoDialog::javaClass.name

        lateinit var file: File

        fun show(fragmentManager: FragmentManager, file: File) {
            this.file = file
            VideoDialog().show(fragmentManager, Tag)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_video_preview, null)

        val videoView = view.video_view
        val videoInfo = view.video_info

        //Set the path in video view to show the video
        videoView.setVideoPath(file.path)

        //Set media controller to set the anchor
        val mediaController = MediaController(activity)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        //Close the dialog on complete
        videoView.setOnCompletionListener {
            dismiss()
        }

        //show duration of video
        videoView.setOnPreparedListener {
            videoInfo.text = "Duration: ${Utils.milliSecondsToTimer(videoView.duration.toLong())}\n"
        }

        videoView.start()

        //show alert dialog
        return MaterialAlertDialogBuilder(requireActivity())
            .setView(view)
            .setTitle("Preview")
            .setPositiveButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .create()
    }
}