package com.sigevsky.routes

import cats.Monad
import cats.effect._
import cats.implicits._
import com.sigevsky.NetworkingUtils
import com.sigevsky.data.DropboxLoadRequest
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Uri}

import com.sigevsky.encodings._
import com.sigevsky.data.LoaderRequest._

object LoaderRoutes {

  def dropboxLoadRoute[F[_]: Sync: Monad](client: Client[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "dropbox" / "upload" =>
        req.decode[DropboxLoadRequest](loadRequest => for {
          aUrl <- Monad[F].pure(Uri.fromString(loadRequest.url))
          sizeResponse <- aUrl match {
            case Left(msg) => BadRequest(s"Bad url:\n $msg")
            case Right(url) =>     for {
              aSize <- NetworkingUtils.fetchFileSize(url, client)
              res <- aSize match {
                case Right(num) => Ok(num.toString)
                case Left(e) => BadRequest(s"$e")
              }
            } yield res
          }
        } yield sizeResponse)
    }
  }
}
