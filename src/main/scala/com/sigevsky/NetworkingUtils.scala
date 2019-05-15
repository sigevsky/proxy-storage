package com.sigevsky

import java.net.URL

import cats.Monad
import cats.effect._

import org.http4s.client._
import org.http4s._
import org.http4s.headers._

import scala.concurrent.ExecutionContext

object NetworkingUtils {

  def streamFile[F[_]: Sync: ContextShift](file: URL, blockingPool: ExecutionContext): fs2.Stream[F, Byte] =
    fs2.io.readInputStream(Sync[F].delay(file.openStream()), 2048, blockingPool)

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
