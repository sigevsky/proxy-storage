package com.sigevsky.data.exceptions

object exceptions {
  case class DropboxLoadException(message: String) extends RuntimeException(message)
}
