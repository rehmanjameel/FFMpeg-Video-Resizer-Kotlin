package org.codebase.ffmpegvideocompression.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_progress.view.*
import org.codebase.ffmpegvideocompression.MainActivity
import org.codebase.ffmpegvideocompression.R

class ProgressDialog: DialogFragment(), MainActivity.ProgressPublish {

    var text: String = ""

    lateinit var progressText: AppCompatTextView
    lateinit var nameText: AppCompatTextView
    lateinit var stopButton: Button

    override fun onProgress(progress: String) {
        this.text = progress
        progressText.text = text
    }

    override fun onDismiss() {
        dismiss()
    }

    companion object {
        val TAG = ProgressDialog::javaClass.name
        var name: String = ""

        fun show(fragmentManager: FragmentManager, name: String) {
            ProgressDialog().show(fragmentManager, TAG)
            this.name = name
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        MainActivity.setProgressListener(this)

        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_progress, null)

        progressText = view.textProgressId
        nameText = view.textNameProgressId
        stopButton = view.progressCancelButtonId

        progressText.text = text
        nameText.text = name

        stopButton.setOnClickListener {
            FFmpeg.getInstance(requireActivity()).killRunningProcesses()
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(view)
            .setTitle("Running FFmpeg commands")
            .setCancelable(false)
            .create()
    }
}
