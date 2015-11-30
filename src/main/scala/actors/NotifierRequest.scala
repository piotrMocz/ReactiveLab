package actors

import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.{ActorLogging, OneForOneStrategy, Actor}
import akka.pattern.ask
import data._

import scala.concurrent.ExecutionContext.Implicits.global

class PublisherNotFoundException extends Throwable

class NotifierRequest extends Actor with ActorLogging {

  implicit val timeout = Timeout(1 second)

  def receive = {
    case a@NotifyMsg(aucName, actorRed, value) =>
      val auctionPublisher = context.actorSelection("akka.tcp://Reactive5@127.0.0.1:2554/user/publisher")
      Await.result(auctionPublisher ? a, 5 seconds)


    case _ =>
      println("Msg not supported for notifier.")

  }

}
