package actors

import akka.actor.{ActorRef, FSM}
import scala.concurrent.duration._

import data._


/**
 * Created by moczur on 11/2/15.
 */


class Auction(var itemInit: Item, var bidders: List[ActorRef]) extends FSM[AuctionState, AuctionData] {
  def this(ii: Item) = this(ii, List.empty[ActorRef])

  println("Creating the actors.Auction state machine")
  startWith(Created, NoBid(itemInit))

  when(Created, stateTimeout = scale(10 seconds)) {
    case Event(Bid(amount, bidder), NoBid(item)) =>
      println("[Created] checking the minimum offer...")
      if (amount < item.minPrice) stay using NoBid(item)
      else {
        // add bidder to the list:
        bidders = bidder :: bidders
        goto(Activated) using Bids(amount, bidder, 1, item)
      }

    case Event(StateTimeout, NoBid(item)) =>
      println("[Created] timed out with no winner...")
      goto(Ignored) using NoBid(item)
  }

  when(Ignored, stateTimeout = scale(30 seconds)) {
    case Event(Relist, NoBid(item)) =>
      println("[Ignored] Relisting...")
      goto(Created) using NoBid(item)

    case Event(StateTimeout, NoBid(item)) =>
      println("[Ignored] Ignored timed out, finishing auction.")
      goto(ItemSold) using NoWinner(item)

    case Event(_, st) =>
      println("[Ignored] Staying in the ignored state...")
      stay using st
  }

  when(Activated, stateTimeout = scale(10 seconds)) {
    case Event(Bid(amount, bidder), Bids(bestBid, bestBidder, cnt, item)) =>
      println("[Activated] checking if the offer is better...")

      if (amount <= bestBid) stay using Bids(bestBid, bestBidder, cnt, item)
      else {
        // add bidder if not in the list:
        if (!bidders.contains(bidder)) bidders = bidder :: bidders
        // notify bidders of a new highest offer:
        bidders.foreach { bdr => bdr ! NewHighestOffer(amount, bidder, self) }
        goto(Activated) using Bids(amount, bidder, cnt+1, item)
      }

    case Event(StateTimeout, Bids(bestBid, bestBidder, cnt, item)) =>
      println("[Activated] we have a winner!")
      goto(ItemSold) using Winner(bestBid, bestBidder, cnt, item)

    case Event(StateTimeout, NoBid(item)) =>
      println("[Activated] no winner, awaiting termination...")
      goto(ItemSold) using NoWinner(item)
  }

  when(ItemSold, stateTimeout = scale(10 seconds)) {
    case Event(StateTimeout, Winner(bestBid, bestBidder, bidCnt, item)) =>
      println("[Sold] SOLD! Contacting both parties...")
      item.seller ! Sold(bestBid, bestBidder)
      bestBidder ! YouWon
      stop()

    case Event(StateTimeout, NoWinner(item)) =>
      println("[Sold] not sold, deleting auction")
      item.seller ! NotSold
      stop()

    case ev =>
      println("Unknown event: "  + ev +  ", terminating anyway.")
      stop()
  }

  whenUnhandled {
    case Event(ev, dt) =>
      println("Got unknown event: " + ev + " with data: " + dt)
      stop()
  }

  onTermination {
    PartialFunction {
      _ => goto(Finished) using NoData
    }
  }

  initialize()
}
