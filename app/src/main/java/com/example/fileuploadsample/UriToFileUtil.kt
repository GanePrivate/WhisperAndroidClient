package com.example.fileuploadsample

import android.content.Context
import android.net.Uri
import com.example.fileuploadsample.RealPathUtil.getRealPath
import java.io.File


/**
 * Created by dhiyaulhaqza on 1/16/18.
 */
object UriToFileUtil {
    fun toFile(context: Context, uri: Uri): File {
        val realPath: String = getRealPath(context, uri)
        return File(realPath)
    }
}