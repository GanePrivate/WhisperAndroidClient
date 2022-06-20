package com.example.fileuploadsample

interface ApiResult {
    fun onSuccess(res: String)
    fun onError(res: String?)
}