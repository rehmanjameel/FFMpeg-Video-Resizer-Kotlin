package org.codebase.ffmpegvideocompression.tools

import android.content.Context
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import org.codebase.ffmpegvideocompression.callback.FFMpegCallback
import org.codebase.ffmpegvideocompression.utils.Utils
import java.io.File
import java.io.IOException
import java.lang.Exception

class VideoResizer private constructor(private val context: Context){

    private var video : File? = null
    private var callback: FFMpegCallback? = null
    private var outPutPath = ""
    private var outPutFileName = ""
    private var size = ""

    fun setFile(originalFile: File) : VideoResizer {
        this.video = originalFile
        return this
    }

    fun setCallBack(callback: FFMpegCallback): VideoResizer {
        this.callback = callback
        return this
    }

    fun setOutPutPath(outPutPath: String): VideoResizer {
        this.outPutPath = outPutPath
        return this
    }

    fun setOutPutFileName(outPutFileName: String): VideoResizer {
        this.outPutFileName = outPutFileName
        return this
    }

    fun setSize(outPutSize: String) : VideoResizer {
        this.size = outPutSize
        return this
    }

    fun resize() {
        if (video == null || !video!!.exists()) {
            callback!!.onFailure(IOException("File not exist"))
            return
        }
        if (!video!!.canRead()) {
            callback!!.onFailure(IOException("Can't read the file. Missing permissions?"))
            return
        }

        val outPutLocation = Utils.getConvertedFile(outPutPath, outPutFileName)

        val cmd = arrayOf("-i", video!!.path, "-vf", "scale=$size", outPutLocation.path, "-hide_banner")

        try {
            FFmpeg.getInstance(context).execute(cmd, object : ExecuteBinaryResponseHandler(){
                override fun onStart() {}

                override fun onProgress(message: String?) {
                    callback!!.onProgress(message!!)
                }

                override fun onSuccess(message: String?) {
                    Utils.refreshGallery(outPutLocation.path, context)
                    callback!!.onSuccess(outPutLocation, OutPutType.TYPE_VIDEO)

                }

                override fun onFailure(message: String?) {
                    if (outPutLocation.exists()) {
                        outPutLocation.delete()
                    }
                    callback!!.onFailure(IOException(message))
                }

                override fun onFinish() {
                    callback!!.onFinish()
                }
            })
        } catch (e: Exception) {
            callback!!.onFailure(e)
        } catch (e2: FFmpegCommandAlreadyRunningException) {
            callback!!.onNotAvailable(e2)
        }
    }

    companion object {


        val TAG = "VideoResizer"

        fun with(context: Context): VideoResizer {
            return VideoResizer(context)
        }
    }
}