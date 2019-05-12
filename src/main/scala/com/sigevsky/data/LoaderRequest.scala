package com.sigevsky.data

case class UploadRequest(token: String, path: String, url: String)
case class UploadResponse(id: String)
