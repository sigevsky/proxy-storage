package com.sigevsky.routes

import java.net.URL
import java.time.Instant
import java.util.UUID

import cats.Monad
import cats.effect._
import cats.effect.implicits._
import cats.effect.concurrent.Ref
import cats.implicits._
import com.sigevsky.NetworkingUtils
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Uri}
import com.sigevsky.data._
import com.sigevsky.encodings._
import io.circe.generic.auto._
import com.sigevsky.storage.DropboxClient

import scala.collection.immutable.HashMap

class DropboxLoader[F[_]](client: Client[F], cache: Ref[F, HashMap[UUID, LoadStatus]], cachedContext: ContextShift[F]) {

  def uploadRoute(implicit S: Sync[F], M: Monad[F], C: Concurrent[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "dropbox" / "upload" =>
        req.decode[UploadRequest](loadRequest => for {
          aUrl <- Uri.fromString(loadRequest.url).pure[F]
          uuid <- Sync[F].delay(UUID.randomUUID())
          res  <- aUrl match {
            case Left(msg)  => BadRequest(s"Bad url:\n $msg")
            case Right(url) => fetchAndUpload(url, loadRequest.token, loadRequest.path, uuid).start >> Ok(uuid.toString)
          }
        } yield res)
    }
  }

  def fetchAndUpload(url: Uri, token: String, path: String, requestId: UUID)(implicit S: Sync[F], M: Monad[F]) = for {
    aSize <- NetworkingUtils.fetchFileSize(url, client)
    dropboxClient = new DropboxClient[F](client, token)
    now <- S.delay(Instant.now().toEpochMilli)
    res <- aSize match {
      case Right(num) => cache.update(_.updated(requestId, InProccess(bytesTransfered = 0, startedLoading = now))) >> uploadToDropbox(dropboxClient, url, path) // TODO: lift to UploadResponse
      case Left(e)    => cache.update(_.updated(requestId, InProccess(bytesTransfered = 0, startedLoading = now))) >> uploadToDropbox(dropboxClient, url, path)
    }
  } yield res

  def uploadToDropbox(dropboxClient: DropboxClient[F], uri: Uri, path: String)(implicit S: Sync[F], M: Monad[F]): F[String] =
    for {
      file <- NetworkingUtils.download(new URL(uri.renderString))
      resp <- dropboxClient.upload(DropboxApiArg(path, "add"), file)
    } yield resp match {
      case Right(DropboxSuccessLoadResponse(name, _, path_display, _, _, _, _, _, _)) => s"Successfully loaded $name to $path_display"
      case Left(e) => s"Failed to upload the file ${e.getCause}"
    }
}
