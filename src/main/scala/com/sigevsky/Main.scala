package com.sigevsky

import java.util.UUID
import java.util.concurrent.Executors

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import com.sigevsky.data.LoadStatus
import com.sigevsky.routes._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze._

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object Main extends IOApp {
  val ec: ExecutionContext  = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  override def run(args: List[String]): IO[ExitCode] =
    for {
      naiveCache <- Ref.of[IO, HashMap[UUID, LoadStatus]](new HashMap())
      exitCode <- BlazeClientBuilder[IO](ec).resource.use { client =>
      val dropBoxLoader = new DropboxLoader[IO](client, naiveCache, IO.contextShift(ec))
        BlazeServerBuilder[IO]
          .bindHttp(8080, "localhost")
          .withHttpApp(dropBoxLoader.uploadRoute.orNotFound)
          .serve
          .compile
          .drain
          .as(ExitCode.Success)
      }
    } yield exitCode
}
