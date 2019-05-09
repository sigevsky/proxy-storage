package com.sigevsky.data

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class DropboxLoadRequest(token: String, path: String, url: String)

object LoaderRequest {
  implicit val dropboxLoadRequestDecoder: Decoder[DropboxLoadRequest] = deriveDecoder

  implicit val dropboxLoadRequestEncoder: Encoder[DropboxLoadRequest] = deriveEncoder
}
