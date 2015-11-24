package actors

import akka.actor.Actor
import data._


class AuctionPublisher extends Actor {


  def receive = {
    case NotifyMsg(aucName, actorRed, value) =>
      println("[PUBLISHER] " + aucName + " new bid: " + value)

    case _ =>
      println("[PUBLISHER] Message not supported.")

  }


}
