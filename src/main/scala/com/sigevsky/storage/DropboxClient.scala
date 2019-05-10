package com.sigevsky.storage


import cats.Monad
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.sigevsky.Main.ec
import io.circe.syntax._
import org.http4s.client._
import org.http4s.headers.{Authorization, `Content-Type`}
import org.http4s.{AuthScheme, Credentials, Header, Headers, MediaType, Method, ParseFailure, Request, Uri}
import com.sigevsky.encodings._
import com.sigevsky.data.implicits._
import com.sigevsky.data._
import com.sigevsky.data.exceptions.exceptions.DropboxLoadException
import org.http4s.Status.{ClientError, Successful}
import org.http4s.client.blaze.BlazeClientBuilder


class DropboxClient[F[_]](client: Client[F], token: String) {
  private val dropBoxUriParse = Uri.fromString("https://content.dropboxapi.com/2/files/")

  private def uploadRequest(args: DropboxApiArg, bytes: List[Byte])(implicit F: Sync[F]): Either[ParseFailure, Request[F]] = dropBoxUriParse.map(uri =>
    Request(Method.POST, uri / "upload", headers=
      Headers.of(
        Authorization(Credentials.Token(AuthScheme.Bearer, token)),
        `Content-Type`(MediaType.application.`octet-stream`),
        Header("Dropbox-API-Arg", args.asJson.noSpaces)
      ), body = fs2.Stream.fromIterator[F, Byte](bytes.iterator)
    ))

    def upload(args: DropboxApiArg, bytes: List[Byte])(implicit F: Sync[F], M: Monad[F]): F[Either[Throwable, DropboxSuccessLoadResponse]] =
      uploadRequest(args, bytes)
        .traverse(req => client.fetch[Either[Throwable, DropboxSuccessLoadResponse]](req) {
          case Successful(res) => res.as[DropboxSuccessLoadResponse].map(_.asRight)
          case ClientError(res) if 400 == res.status.code => res.as[String].map(e => DropboxLoadException(e).asLeft)
          case ClientError(res) if 409 == res.status.code => res.as[DropboxFailLoadResponse].map(e => DropboxLoadException(e.error_summary).asLeft)
          case _ => M.pure(DropboxLoadException("Something went wrong while quering dropbox api").asLeft)
        }).map(_.flatten)
}

object test extends IOApp {

  val dargs = DropboxApiArg("/sincre1.txt", "add")
  val token = "P3kcOuKBjoMAAAAAAAACBXacGJnVEUl05Za_RIwtuFZGjrP05ot6zomlypLiiODf"

  override def run(args: List[String]): IO[ExitCode] = for {
    aRes <- BlazeClientBuilder[IO](ec).resource.use(client => {
      val dClient = new DropboxClient(client, token)
      dClient.upload(dargs, "Hello from http4s!".getBytes("UTF-8").toList)
    })
    _ <- IO(println(aRes))
  } yield ExitCode.Success
}
