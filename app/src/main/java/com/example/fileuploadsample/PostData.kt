package com.example.fileuploadsample

import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


class PostData(val fileName: String, val filePath: String, val modelName: String) {
    private val client = OkHttpClient()

    fun run(callback: ApiResult) {

        // 送信ファイルの指定
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "model_name", modelName
            )
            .addFormDataPart(
                "file", fileName,
                File(filePath).asRequestBody()
            )
            .build()

        // リクエストの作成
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://whisper-gpu.kajilab.tk/upload/")
            .post(requestBody)
            .build()

        // 非同期でリクエストを実行
        client.newCall(request).enqueue(object : okhttp3.Callback {
            @Throws(IOException::class)

            // 成功時の処理
            override fun onResponse(call: Call, response: Response) {
                val resString = response.body!!.string()
                println(resString)
                callback.onSuccess(resString)
            }

            // 失敗時の処理
            override fun onFailure(call: Call, arg1: IOException) {
                println(arg1)
                callback.onError(arg1.toString())
            }
        })
    }
}