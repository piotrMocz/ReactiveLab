package actors

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.{ActorLogging, OneForOneStrategy, Actor}
import data._

import scala.concurrent.ExecutionContext.Implicits.global

class PublisherNotFoundException extends Throwable

class NotifierRequest extends Actor with ActorLogging {

  def receive = {
    case a@NotifyMsg(aucName, actorRed, value) =>
      val auctionPublisher = context.actorSelection("akka.tcp://Reactive5@127.0.0.1:2554/user/publisher1234567")
      auctionPublisher.resolveOne(5 seconds).onComplete( {
        case Success(_)  => log.info("OJOJOJOJOJOJOJOJOJ"); auctionPublisher ! a
        case Failure(ex) => log.info("EJEJEJEJJEJEJEJEJE"); throw ex
      })

    case _ =>
      println("Msg not supported for notifier.")

  }

}
