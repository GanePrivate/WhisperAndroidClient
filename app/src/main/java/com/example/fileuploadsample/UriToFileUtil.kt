package com.example.fileuploadsample

import android.content.Context
import android.net.Uri
import com.example.fileuploadsample.RealPathUtil.getRealPath
import java.io.File


object UriToFileUtil {
    fun toFile(context: Context, uri: Uri): File {
        val realPath: String = getRealPath(context, uri)
        return File(realPath)
    }
}