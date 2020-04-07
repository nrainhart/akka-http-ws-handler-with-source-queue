import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.{actor => classic}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object TestServer extends App {

  private val config = ConfigFactory.parseString(
    """
      |
      | akka.loglevel = "DEBUG"
      | akka.http.server.linger-timeout = 5s
      |
      |""".stripMargin)


  implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "test-server", config)
  implicit val classicSystem: classic.ActorSystem = system.toClassic
  implicit val executionContext: ExecutionContext = system.executionContext
  implicit val materializer: Materializer = Materializer(classicSystem)

  val (queue, source0) =
    Source.queue[Int](50, OverflowStrategy.fail)
      .scan(0)(_ + _)
      .toMat(BroadcastHub.sink)(Keep.both)
      .run()
  val source = source0.buffer(1000 /* Tune as needed */, OverflowStrategy.fail)

  system.scheduler.scheduleWithFixedDelay(0.seconds, 1.second)(() => queue.offer(1))
  val wsHandler = path("ws") {
    handleWebSocketMessages(Flow.fromSinkAndSourceCoupled(Sink.ignore, source.map(i => TextMessage(s"[$i] Hey!"))))
  } ~ getFromResource("ws.html")

  Http().bindAndHandle(wsHandler, "127.0.0.1", port = 3030)
}
