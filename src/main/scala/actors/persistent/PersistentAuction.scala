package actors.persistent

import akka.actor.ActorRef
import akka.persistence._
import akka.persistence.fsm.PersistentFSM
import data._
import data.persistent.Activated
import data.persistent.Bids
import data.persistent.Created
import data.persistent.Finished
import data.persistent.Ignored
import data.persistent.ItemSold
import data.persistent.NoBid
import data.persistent.NoData
import data.persistent.NoWinner
import data.persistent.Winner
import data.persistent._

import language.postfixOps
import scala.concurrent.duration._
import scala.reflect._


/**
 * Created by moczur on 11/2/15.
 */


class PersistentAuction(var itemInit: Item, var bidders: List[ActorRef]) extends PersistentFSM[PersistentAuctionState, PersistentAuctionData, PersistentAuctionEvent] {
  def this(ii: Item) = this(ii, List.empty[ActorRef])

  override def domainEventClassTag: ClassTag[PersistentAuctionEvent] = classTag[PersistentAuctionEvent]

  override def persistenceId: String = "persistent-auction-fsm-id-1"

  override def applyEvent(domainEvent: PersistentAuctionEvent,
                          currentData: PersistentAuctionData): PersistentAuctionData = domainEvent match {
    case MakeBid(bid)    => print("Current data: " + currentData + "  -->  "); println("[applyEvent] handling MakeBid"); currentData.makeBid(bid)
    case DoNotBid        => print("Current data: " + currentData + "  -->  "); println("[applyEvent] handling DoNotBid"); currentData
    case RecognizeWinner => print("Current data: " + currentData + "  -->  "); println("[applyEvent] handling RecognizeWinner"); currentData.awardWin
    case HaveNoWinner    => print("Current data: " + currentData + "  -->  "); println("[applyEvent] handling HaveNoWinner"); currentData.noWinner
    case NullEvent       => print("Current data: " + currentData + "  -->  "); println("[applyEvent] handling NullEvent"); currentData
  }

  // START THE FSM

  println("Creating the actors.persistent.Auction state machine")
  startWith(Created, NoBid(itemInit))

  when(Created, stateTimeout = 10 seconds) {
    case Event(b@Bid(amount, bidder), data) =>
      println("[Created] checking the minimum offer...")
      if (amount < data.getItem.minPrice) stay applying DoNotBid
      else {
        // add bidder to the list:
        bidders = bidder :: bidders
        bidders.foreach { bdr => bdr ! NewHighestOffer(amount, bidder, self) }
        goto(Activated) applying MakeBid(b) // Bids(amount, bidder, 1, item)
      }

    case Event(StateTimeout, _) =>
      println("[Created] timed out with no winner...")
      goto(Ignored) applying DoNotBid

    case _ => println("Dupa"); stop()
  }

  when(Ignored, stateTimeout = 30 seconds) {
    case Event(Relist, _) =>
      println("[Ignored] Relisting...")
      goto(Created) applying DoNotBid

    case Event(StateTimeout, _) =>
      println("[Ignored] Ignored timed out, finishing auction.")
      goto(ItemSold) applying DoNotBid

    case Event(_, _) =>
      println("[Ignored] Staying in the ignored state...")
      stay applying DoNotBid
  }

  when(Activated, stateTimeout = 10 seconds) {
    case Event(b@Bid(amount, bidder), data) =>
      println("[Activated] checking if the offer is better...")

      if (amount <= data.getBestBid) stay applying DoNotBid // using Bids(bestBid, bestBidder, cnt, item)
      else {
        // add bidder if not in the list:
        if (!bidders.contains(bidder)) bidders = bidder :: bidders
        // notify bidders of a new highest offer:
        bidders.foreach { bdr => bdr ! NewHighestOffer(amount, bidder, self) }
        goto(Activated) applying MakeBid(b) // using Bids(amount, bidder, cnt+1, item)
      }

    case Event(StateTimeout, data) =>
      println("[Activated] we have a winner!")

      goto(ItemSold) applying RecognizeWinner // using Winner(bestBid, bestBidder, cnt, item)

    case Event(StateTimeout, _) =>
      println("[Activated] no winner, awaiting termination...")
      goto(ItemSold) applying HaveNoWinner // using NoWinner(item)
  }

  when(ItemSold, stateTimeout = 10 seconds) {
    case Event(StateTimeout, data) =>
      println("[Sold] SOLD! Contacting both parties...")
      data.getItem.seller ! Sold(data.getBestBid, data.getBestBidder)
      data.getBestBidder ! YouWon
      stop()

    case Event(StateTimeout, data) =>
      println("[Sold] not sold, deleting auction")
      data.getItem.seller ! NotSold
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
      _ => goto(Finished) applying NullEvent
    }
  }

  initialize()
}
