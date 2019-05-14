package com.sigevsky.workers

import java.net.URL
import java.util.UUID

import cats.Monad
import cats.effect.{ContextShift, Sync, Timer}
import cats.effect.concurrent.Ref
import cats.implicits._
import com.sigevsky.NetworkingUtils
import com.sigevsky.data.{DropboxApiArg, DropboxSuccessLoadResponse, Failed, InProgress, JobStatus, Success, UploadJob}
import com.sigevsky.storage.DropboxClient
import fs2.concurrent.Queue
import org.http4s.Uri
import org.http4s.client.Client

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.MILLISECONDS

class DropboxMachinery[F[_]: Sync: Monad: Timer: ContextShift](queue: Queue[F, UploadJob], cache: Ref[F, HashMap[UUID, JobStatus]], client: Client[F], cachedPool: ExecutionContext) {

  def worker: F[Unit] = for {
    job <- queue.dequeue1
    now <- Timer[F].clock.realTime(MILLISECONDS)
    _   <- cache.update(_.updated(job.id, InProgress(0, now)))
    _   <- Uri.fromString(job.url) match {
      case Left(_) => cache.update(_.updated(job.id, Failed(s"Failed to parse url ${job.url}")))
      case Right(url) => ContextShift[F].evalOn(cachedPool)(fetchAndUpload(url, job.token, job.path, job.id).handleErrorWith(e => cache.update(_.updated(job.id, {e.printStackTrace(); Failed(s"$e")}))))
    }
  } yield ()

  def fetchAndUpload(url: Uri, token: String, path: String, id: UUID): F[Unit] = for {
    aSize <- NetworkingUtils.fetchFileSize(url, client)
    dropboxClient = new DropboxClient[F](client, token)
    now <- Timer[F].clock.realTime(MILLISECONDS)
    res <- aSize match {
      case Right(num) => cache.update(_.updated(id, InProgress(0, now))) >> uploadToDropbox(dropboxClient, url, path, id)
      case Left(e)    => cache.update(_.updated(id, InProgress(0, now))) >> uploadToDropbox(dropboxClient, url, path, id)
    }
  } yield res

  def uploadToDropbox(dropboxClient: DropboxClient[F], uri: Uri, path: String, id: UUID): F[Unit] =
    for {
      file <- NetworkingUtils.download(new URL(uri.renderString))
      now <- Timer[F].clock.realTime(MILLISECONDS)
      resp <- dropboxClient.upload(DropboxApiArg(path, "add"), file)
      _ <- resp match {
        case Right(DropboxSuccessLoadResponse(name, _, path_display, _, _, _, _, _, _)) => cache.update(_.updated(id, Success(s"Successfully loaded $name to $path_display", now)))
        case Left(e) => cache.update(_.updated(id, Failed(s"Failed to load file $e")))
      }
    } yield ()

  // fetchAndUpload(url, loadRequest.token, loadRequest.path, uuid).handleErrorWith(e => cache.update(_.updated(uuid, {e.printStackTrace(); Failed(s"$e")}))).start
}