package actors

import akka.actor.Actor
import akka.event.LoggingReceive

import data._

/**
 * Created by moczur on 11/2/15.
 */


class Seller extends Actor {
  override def receive = LoggingReceive {
    case AddAuction(name, minPr) =>
      val aucSearch = context.system.actorSelection("/user/auctionSearch")
      println("[actors.Seller] Found auctionSearch:   " + aucSearch)
      aucSearch ! AddAuction(name, minPr)

    case Sold(price, bidder) =>
      println("> [actors.Seller] I sold an item for " + price + ", nice!")

    case NotSold =>
      println("> [actors.Seller] I didn't sell a thing.")

    case _ =>
      println("> [actors.Seller] Unknown message.")
  }
}