package com.sigevsky.data

sealed trait LoadStatus extends Product with Serializable

case class Success(message: String, finishedLoading: Long, status: String = "success") extends LoadStatus
case class InProgress(bytesTransfered: Long, startedLoading: Long, status: String = "in progress") extends LoadStatus
case class Failed(reason: String, status: String = "failed") extends LoadStatus
case class UploadNotFound(status: String = "upload not found") extends LoadStatus