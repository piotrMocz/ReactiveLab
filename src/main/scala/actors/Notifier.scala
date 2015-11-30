package actors

import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{ActorLogging, OneForOneStrategy, Props, Actor}
import akka.event.LoggingReceive
import data.{PublisherException, NotifyMsg}

import scala.util.Random


class Notifier extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 100) {
      // case _: IllegalStateException => log.error("!!!!!!!RESTARTING!!!!!!!"); Restart
      case _: PublisherException => log.error("[Notifier] RESTARTING"); Restart
      case _                     => log.error("[Notifier] Unknown exception, escalating"); Escalate
    }

  def receive = {
    case a@NotifyMsg(aucName, actorRed, value) =>
      val notifierResp = context.actorOf(Props[NotifierRequest], "notifier" + Random.nextLong().toString)
      notifierResp ! a

    case _ =>
      println("Message not supported for notifier.")

  }


}
