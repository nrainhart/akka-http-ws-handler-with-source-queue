import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
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
      |
      |""".stripMargin)


  implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "test-server", config)
  implicit val classicSystem: classic.ActorSystem = system.toClassic
  implicit val executionContext: ExecutionContext = system.executionContext
  implicit val materializer: Materializer = Materializer(classicSystem)

  val (queue, source) = Source.queue[Int](50, OverflowStrategy.fail).preMaterialize()

  system.scheduler.scheduleWithFixedDelay(0.seconds, 1.second)(() => queue.offer(1))
  val wsHandler = path("ws") {
    handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, source.map(_ => TextMessage("Hey!"))))
  }

  Http().bindAndHandle(wsHandler, "127.0.0.1", port = 3030)
}
