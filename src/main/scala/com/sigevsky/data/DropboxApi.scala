package com.sigevsky.data

import io.circe.{Decoder, Encoder}
import cats.syntax.functor._
import io.circe.syntax._
import io.circe.generic.semiauto._

case class DropboxApiArg(path: String, mode: String, autorename: Boolean = false, mute: Boolean = false, strict_conflict: Boolean = false)

case class DropboxSuccessLoadResponse(name: String,
                               path_lower: String,
                               path_display: String,
                               id: String,
                               client_modified: String,
                               server_modified: String,
                               rev: String,
                               size: Double,
                               content_hash: String)

case class DropboxFailLoadResponse(error_summary: String, error: Error)

case class Error(`.tag`: String, reason: Reason, upload_session_id: String)
case class Reason(`.tag`: String, conflict: Conflict)
case class Conflict(`.tag`: String)

case class DropboxBadRequestLoadResponse(err: String)


object implicits {

  // Encoders

  implicit val dropboxApiArgEncoder: Encoder[DropboxApiArg] = deriveEncoder

  implicit val dropboxSuccessLoadRespEncoder: Encoder[DropboxSuccessLoadResponse] = deriveEncoder

  implicit val dropboxFailLoadRespEncoder: Encoder[DropboxFailLoadResponse] = deriveEncoder
  implicit val dropboxErrorLoadRespEncoder: Encoder[Error] = deriveEncoder
  implicit val dropboxReasonLoadRespEncoder: Encoder[Reason] = deriveEncoder
  implicit val dropboxConflictLoadRespEncoder: Encoder[Conflict] = deriveEncoder

  // Decoders

  implicit val dropboxApiArgDecoder: Decoder[DropboxApiArg] = deriveDecoder

  implicit val dropboxSuccessLoadRespDecoder: Decoder[DropboxSuccessLoadResponse] = deriveDecoder

  implicit val dropboxBadRequestLoadRespDecoder: Decoder[DropboxBadRequestLoadResponse] = deriveDecoder

  implicit val dropboxFailLoadRespDecoder: Decoder[DropboxFailLoadResponse] = deriveDecoder
  implicit val dropboxErrorLoadRespDecoder: Decoder[Error] = deriveDecoder
  implicit val dropboxReasonLoadRespDecoder: Decoder[Reason] = deriveDecoder
  implicit val dropboxConflictLoadRespDecoder: Decoder[Conflict] = deriveDecoder
}
