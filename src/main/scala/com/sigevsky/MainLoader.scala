package com.sigevsky

import java.util.UUID
import java.util.concurrent.Executors

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import com.sigevsky.data.{JobStatus, UploadJob}
import com.sigevsky.routes._
import com.sigevsky.workers.DropboxMachinery
import fs2.concurrent.Queue
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze._

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object MainLoader extends IOApp {

  val clientContext: ExecutionContext  = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  val cachedPool: ExecutionContext  = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  override def run(args: List[String]): IO[ExitCode] =
    for {
      taskQueue <- Queue.bounded[IO, UploadJob](100)
      naiveCache <- Ref.of[IO, HashMap[UUID, JobStatus]](new HashMap())
      exitCode <- BlazeClientBuilder[IO] (clientContext)
        .withResponseHeaderTimeout(3 hours)
        .resource.use { client =>
          val dropBoxRoutes = DropboxRoutes.routes[IO](taskQueue, naiveCache)
          val dropboxMachinery = new DropboxMachinery[IO](taskQueue, naiveCache, client, cachedPool)
          val workers = (1 to 3).map(dropboxMachinery.worker(_).start).toList.sequence

          workers >>
            BlazeServerBuilder[IO]
              .bindHttp(8080, "localhost")
              .withHttpApp(dropBoxRoutes.orNotFound)
              .serve
              .compile
              .drain
              .as(ExitCode.Success)
}
    } yield exitCode
}
