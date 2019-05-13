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

class DropboxRoutes[F[_]](client: Client[F], cache: Ref[F, HashMap[UUID, LoadStatus]], cachedContext: ContextShift[F]) {

  private[routes] val dsl: Http4sDsl[F] = new Http4sDsl[F]{}

  def uploadRoute(implicit S: Sync[F], M: Monad[F], C: Concurrent[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "dropbox" / "upload" =>
        req.decode[UploadRequest](loadRequest => for {
          aUrl <- Uri.fromString(loadRequest.url).pure[F]
          uuid <- Sync[F].delay(UUID.randomUUID())
          res  <- aUrl match {
            case Left(msg)  => BadRequest(s"Bad url:\n $msg")
            case Right(url) => fetchAndUpload(url, loadRequest.token, loadRequest.path, uuid).handleErrorWith(e => cache.update(_.updated(uuid, Failed(s"$e")))).start >> Ok(uuid.toString)
          }
        } yield res)
      case GET -> Root / "dropbox" / "status" / id => for {
        uuid <- S.delay(UUID.fromString(id))
        opLoadStatus <- cache.get.map(_.get(uuid))
        respStatus <- opLoadStatus match {
          case Some(status) => Ok(status)
          case None => Ok(UploadNotFound())
        }
      } yield respStatus
    }
  }

  def fetchAndUpload(url: Uri, token: String, path: String, id: UUID)(implicit S: Sync[F], M: Monad[F]): F[Unit] = for {
    aSize <- NetworkingUtils.fetchFileSize(url, client)
    dropboxClient = new DropboxClient[F](client, token)
    now <- S.delay(Instant.now().toEpochMilli)
    res <- aSize match {
      case Right(num) => cache.update(_.updated(id, InProgress(0, now))) >> uploadToDropbox(dropboxClient, url, path, id)
      case Left(e)    => cache.update(_.updated(id, InProgress(0, now))) >> uploadToDropbox(dropboxClient, url, path, id)
    }
  } yield res

  def uploadToDropbox(dropboxClient: DropboxClient[F], uri: Uri, path: String, id: UUID)(implicit S: Sync[F], M: Monad[F]): F[Unit] =
    for {
      file <- NetworkingUtils.download(new URL(uri.renderString))
      now <- S.delay(Instant.now().toEpochMilli)
      resp <- dropboxClient.upload(DropboxApiArg(path, "add"), file)
      _ <- resp match {
        case Right(DropboxSuccessLoadResponse(name, _, path_display, _, _, _, _, _, _)) => cache.update(_.updated(id, Success(s"Successfully loaded $name to $path_display", now)))
        case Left(e) => cache.update(_.updated(id, Failed(s"Failed to load file $e")))
      }
    } yield ()
}
