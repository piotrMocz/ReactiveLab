package actors

import akka.actor.Actor
import akka.event.LoggingReceive
import scala.util.Random

import data._

/**
 * Created by moczur on 11/2/15.
 */

class Buyer(name: String, maxOffer: Option[Float]) extends Actor {

  def takeRand[T](l: List[T]): T = {
    val rnd = new Random()
    l(rnd.nextInt(l.length))
  }

  override def receive = if (maxOffer.isDefined) receiveWithMax else receiveNoMax

  val receiveWithMax = LoggingReceive {
    case BidOrder(toBid) =>
      println("> [actors.Buyer] Looking up auctions.")
      toBid.foreach { kwd =>
        val aucSearch = context.system.actorSelection("/user/auctionSearch")
        aucSearch ! Search(kwd)
        //takeRand(auctions) ! Bid(bidAmount, self)
      }

    case SearchResult(auctions) =>
      println("> [actors.Buyer] Bidding in auctions...")
      auctions.foreach { a =>
        val bidAmount = new Random().nextFloat() * 100.0f
        if (bidAmount <= maxOffer.get) a ! Bid(bidAmount, self)
      }

    case NewHighestOffer(newOffer, newBidder, auction) =>
      if ((newOffer + 1.0f) <= maxOffer.get && newBidder != self) auction ! Bid(newOffer + 1.0f, self)

    case YouWon =>
      println("> [actors.Buyer] I won!!! (" + name + ")")

    case _ =>
      println("> [actors.Buyer] Unknown message.")
  }

  val receiveNoMax = LoggingReceive {
    case BidOrder(toBid) =>
      println("> [actors.Buyer] Looking up auctions.")
      toBid.foreach { kwd =>
        val aucSearch = context.system.actorSelection("/user/auctionSearch")
        aucSearch ! Search(kwd)
        //takeRand(auctions) ! Bid(bidAmount, self)
      }

    case SearchResult(auctions) =>
      println("> [actors.Buyer] Bidding in auctions...")
      auctions.foreach { a =>
        val bidAmount = new Random().nextFloat() * 100.0f
        a ! Bid(bidAmount, self)
      }

    case YouWon =>
      println("> [actors.Buyer] I won!!! (" + name + ")")

    case _ =>
      println("> [actors.Buyer] Unknown message.")
  }
}