package com.sigevsky.data

case class DropboxApiArg(path: String, mode: String, autorename: Boolean = false, mute: Boolean = false, strict_conflict: Boolean = false)

sealed trait DropboxApiResponse extends Product with Serializable

case class DropboxSuccessLoadResponse(name: String,
                               path_lower: String,
                               path_display: String,
                               id: String,
                               client_modified: String,
                               server_modified: String,
                               rev: String,
                               size: Double,
                               content_hash: String) extends DropboxApiResponse

case class DropboxFailLoadResponse(error_summary: String, error: Error)  extends DropboxApiResponse

case class Error(`.tag`: String, reason: Reason, upload_session_id: String)
case class Reason(`.tag`: String, conflict: Conflict)
case class Conflict(`.tag`: String)

case class DropboxBadRequestLoadResponse(err: String)  extends DropboxApiResponse
