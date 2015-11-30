package actors

import akka.actor.{Status, Actor}
import data._


class AuctionPublisher extends Actor {

  var shouldFail = false
  var msgs = List.empty[(String, Float)]

  def receive = {
    case NotifyMsg(aucName, actorRed, value) =>
      println("[PUBLISHER] Replaying message log:")
      msgs.foreach { m =>
        println("[PUBLISHER] " + m._1 + " new bid: " + m._2)
      }
      println("[PUBLISHER] New message: " + aucName + " new bid: " + value)

      if (shouldFail) sender() ! Status.Failure(new PublisherException)
      else sender() ! Status.Success

      shouldFail = !shouldFail

    case _ =>
      println("[PUBLISHER] Message not supported.")

  }


}
