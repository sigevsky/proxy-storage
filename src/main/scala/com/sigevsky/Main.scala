package com.sigevsky

import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

import cats.effect._
import cats.implicits._
import com.sigevsky.routes._
import org.http4s.{HttpRoutes, Uri}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.syntax._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object Main extends IOApp {
  val path = "https://gist.githubusercontent.com/sigevsky/34b1a4e20de92a5e8eb39a3081d37db1/raw/caebbc1877a32ea394a62c7d8634276129ee2971/docker-compose.yml"
  val source = new URL(path)

  def printBytes(bt: Array[Byte]): IO[Unit] = IO(println(s"NEW LUMP on ${Thread.currentThread().getName}: \n ${new String(bt, StandardCharsets.UTF_8)}"))
  def printBytes(bt: List[Byte]): IO[Unit] = printBytes(bt.toArray)

  val ec: ExecutionContext  = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  override def run(args: List[String]): IO[ExitCode] =
    for {
      exitCode <- BlazeClientBuilder[IO](ec).resource.use(client =>
        BlazeServerBuilder[IO]
          .bindHttp(8080, "localhost")
          .withHttpApp(LoaderRoutes.dropboxLoadRoute[IO](client).orNotFound)
          .serve
          .compile
          .drain
          .as(ExitCode.Success))
    } yield exitCode
}
