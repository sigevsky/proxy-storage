package com.sigevsky.data

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class UploadRequest(token: String, path: String, url: String)
case class UploadResponse(status: String)

object LoaderRequest {
  implicit val dropboxLoadRequestDecoder: Decoder[UploadRequest] = deriveDecoder

  implicit val dropboxLoadRequestEncoder: Encoder[UploadRequest] = deriveEncoder

  implicit val dropboxLoadResponseDecoder: Decoder[UploadResponse] = deriveDecoder

  implicit val dropboxLoadResponseEncoder: Encoder[UploadResponse] = deriveEncoder
}
