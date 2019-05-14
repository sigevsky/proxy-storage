package com.sigevsky.routes

import java.time.Instant
import java.util.UUID

import cats.Monad
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import com.sigevsky.data._
import com.sigevsky.encodings._
import io.circe.generic.auto._
import fs2.concurrent.Queue

import scala.concurrent.duration._
import scala.collection.immutable.HashMap

object DropboxRoutes {

  def routes[F[_]: Sync: Monad: Concurrent: Timer](queue: Queue[F, UploadJob], cache: Ref[F, HashMap[UUID, JobStatus]]): HttpRoutes[F] = {
    val dsl: Http4sDsl[F] = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "dropbox" / "upload" =>
        req.decode[UploadRequest](lr => for {
          uuid <- Sync[F].delay(UUID.randomUUID())
          now <- Timer[F].clock.realTime(MILLISECONDS)
          res  <- queue.enqueue1(UploadJob(uuid, lr.path, lr.token, lr.url)) >> cache.update(_.updated(uuid, Pending(now))) >> Ok(UploadResponse(uuid.toString))
        } yield res)

      case GET -> Root / "dropbox" / "status" / id => for {
        uuid <- Sync[F].delay(UUID.fromString(id))
        opLoadStatus <- cache.get.map(_.get(uuid))
        respStatus   <- opLoadStatus match {
          case Some(status) => Ok(status)
          case None => Ok(UploadNotFound())
        }
      } yield respStatus
    }
  }
}
