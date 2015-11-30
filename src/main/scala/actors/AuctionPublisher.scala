package actors

import akka.actor.{Status, Actor}
import data._


class AuctionPublisher extends Actor {

  val shouldFail = true

  def receive = {
    case NotifyMsg(aucName, actorRed, value) =>
      println("[PUBLISHER] " + aucName + " new bid: " + value)
      if (shouldFail) sender() ! Status.Failure(new Exception())
      else sender() ! Status.Success

    case _ =>
      println("[PUBLISHER] Message not supported.")

  }


}
