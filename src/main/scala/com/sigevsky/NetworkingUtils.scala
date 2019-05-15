package com.sigevsky

import java.io.{BufferedInputStream, InputStream}
import java.net.URL

import cats.Monad
import cats.effect._
import cats.implicits._

import scala.collection.mutable.ArrayBuffer
import org.http4s.client._
import org.http4s._
import org.http4s.headers._

import scala.language.higherKinds

object NetworkingUtils {

  def remoteResource[F[_]: Sync](file: URL): Resource[F, BufferedInputStream] = Resource.fromAutoCloseable(Sync[F].delay {
    new BufferedInputStream(file.openStream())
  })

  def download[F[_]: Sync: Monad](file: URL): F[List[Byte]] = for {
    ioBuffer <- Sync[F].delay(new Array[Byte](2048))
    acc      <- Sync[F].delay(new ArrayBuffer[Byte]())
    res      <- remoteResource(file).use(is => load(is, ioBuffer, acc))
  } yield res

  private def load[F[_]: Sync: Monad](is: InputStream, buff: Array[Byte], acc: ArrayBuffer[Byte]): F[ArrayBuffer[Byte]] = for {
    n   <- Sync[F].delay(is.read(buff, 0, buff.length))
    res <- if (n == -1) acc.pure[F] else Sync[F].delay(acc ++= buff.take(n)).flatMap(a => load(is, buff, a))
  } yield res

  def downloadChunkAndConsume[F[_]: Sync: Monad](file: URL, chunkSize: Int, consumer: Array[Byte] => F[Unit]): F[Long] = for {
    buffer <- Sync[F].delay(new Array[Byte](chunkSize))
    n      <- remoteResource(file).use(is => loadAndConsume(is, buffer, 0, consumer))
  } yield n

  private def loadAndConsume[F[_]: Sync: Monad](is: InputStream, buff: Array[Byte], acc: Long, consumer: Array[Byte] => F[Unit]): F[Long] = for {
    n  <- Sync[F].delay(is.read(buff))
    rn <- if (n == -1) Monad[F].pure(acc) else consumer(buff.take(n)) >> loadAndConsume(is, buff, acc + n.toLong, consumer)
  } yield rn

  def fetchFileSize[F[_]: Monad](target: Uri, client: Client[F]): F[Either[String, Long]] =
    client.fetch(Request[F](Method.HEAD, target)) {
      case Status.Successful(r) => Monad[F].pure(
        r.headers.get(`Content-Length`)
          .map(h => h.length)
          .map(e => Right(e))
          .getOrElse(Left(s"No Content-Length were recieved from ${target.path}"))
      )
      case _ => Monad[F].pure(Left(s"Failed to obtain headers from ${target.path}"))
    }
}
