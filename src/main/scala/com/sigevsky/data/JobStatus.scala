package com.sigevsky.data

sealed trait JobStatus extends Product with Serializable {
  def status: String
}

case class Success(message: String, finishedLoading: Long, status: String = "success") extends JobStatus
case class Pending(pendingStarted: Long, status: String = "pending") extends JobStatus
case class InProgress(bytesTransfered: Long, startedLoading: Long, status: String = "in progress") extends JobStatus
case class Failed(reason: String, status: String = "failed") extends JobStatus
case class UploadNotFound(status: String = "upload not found") extends JobStatus