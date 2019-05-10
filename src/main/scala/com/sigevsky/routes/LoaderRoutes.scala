package com.sigevsky.routes

import java.net.URL

import cats.Monad
import cats.effect._
import cats.implicits._
import com.sigevsky.NetworkingUtils
import com.sigevsky.data.{DropboxApiArg, UploadRequest, DropboxSuccessLoadResponse}
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Uri}
import com.sigevsky.encodings._
import com.sigevsky.data.LoaderRequest._
import com.sigevsky.storage.DropboxClient

object LoaderRoutes {

  def dropboxLoadRoute[F[_]: Sync: Monad](client: Client[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "dropbox" / "upload" =>
        req.decode[UploadRequest](loadRequest => for {
          aUrl <- Uri.fromString(loadRequest.url).pure[F]
          sizeResponse <- aUrl match {
            case Left(msg) => BadRequest(s"Bad url:\n $msg")
            case Right(url) =>     for {
              aSize <- NetworkingUtils.fetchFileSize(url, client)
              dropboxClient = new DropboxClient[F](client, loadRequest.token)
              res <- aSize match {
                case Right(num) => uploadToDropbox(dropboxClient, url, loadRequest.path).flatMap(Ok(_)) // TODO: lift to UploadResponse
                case Left(e) => BadRequest(s"$e") // TODO: lift to UploadResponse
              }
            } yield res
          }
        } yield sizeResponse)
    }
  }

  def uploadToDropbox[F[_]: Sync: Monad](dropboxClient: DropboxClient[F], uri: Uri, path: String): F[String] =
    for {
      file <- NetworkingUtils.download(new URL(uri.renderString))
      resp <- dropboxClient.upload(DropboxApiArg(path, "add"), file)
    } yield resp match {
      case Right(DropboxSuccessLoadResponse(name, _, path_display, _, _, _, _, _, _)) => s"Successfully loaded $name to $path_display"
      case Left(e) => s"Failed to upload the file ${e.getCause}"
    }
}
