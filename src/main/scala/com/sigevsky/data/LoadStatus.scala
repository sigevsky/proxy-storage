package com.sigevsky.data

sealed trait LoadStatus

case class Success(status: String = "success", finishedLoading: Long) extends LoadStatus
case class InProccess(status: String = "in progress", bytesTransfered: Long, startedLoading: Long) extends LoadStatus
case class Failed(status: String = "failed", reason: String) extends LoadStatus