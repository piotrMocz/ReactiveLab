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


class PersistentAuction(val title: String, itemInit: Item) extends PersistentFSM[PersistentAuctionState, PersistentAuctionData, PersistentAuctionEvent] {

  override def domainEventClassTag: ClassTag[PersistentAuctionEvent] = classTag[PersistentAuctionEvent]

  override def persistenceId: String = "persistent-auction-fsm-" + title

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
    case Event(b@Bid(amount, bidder), data) if amount < data.getItem.minPrice => stay applying DoNotBid andThen { _ =>
      println("[Created] Minimum offer not reached, staying")
    }

    case Event(b@Bid(amount, bidder), data) => goto(Activated) applying MakeBid(b) andThen { _ =>
        println("[Created] Got a bid! Going to activated and notifying watchers")
        data.getBidders.foreach { bdr => bdr ! NewHighestOffer(amount, bidder, self) }
      }

    case Event(StateTimeout, _) => goto(Ignored) applying DoNotBid andThen { _ =>
      println("[Created] timed out with no winner...")
    }
  }

  when(Ignored, stateTimeout = 10 seconds) {
    case Event(Relist, _) =>
      goto(Created) applying DoNotBid andThen { _ =>
        println("[Ignored] Relisting...")
      }

    case Event(StateTimeout, _) =>
      goto(ItemSold) applying HaveNoWinner andThen { _ =>
        println("[Ignored] Ignored timed out, finishing auction.")
      }

    case Event(_, _) =>
      stay applying DoNotBid andThen { _ =>
        println("[Ignored] Staying in the ignored state...")
      }
  }

  when(Activated, stateTimeout = 10 seconds) {
    case Event(b@Bid(amount, bidder), data) if amount <= data.getBestBid =>
       stay applying DoNotBid // using Bids(bestBid, bestBidder, cnt, item)

    case Event(b@Bid(amount, bidder), data) => goto(Activated) applying MakeBid(b) andThen { _ => // using Bids(amount, bidder, cnt+1, item)
        // notify bidders of a new highest offer:
        data.getBidders.foreach { bdr => bdr ! NewHighestOffer(amount, bidder, self) }
      }

    case Event(StateTimeout, data) =>
      goto(ItemSold) applying RecognizeWinner andThen { _ =>
        println("[Activated] we have a winner!")
      } // using Winner(bestBid, bestBidder, cnt, item)
  }

  when(ItemSold, stateTimeout = 10 seconds) {
    case Event(StateTimeout, data) => stop() andThen { _ =>
      println("[Sold] SOLD! Contacting both parties...")
      data.getItem.seller ! Sold(data.getBestBid, data.getBestBidder)
      data.getBestBidder ! YouWon
    }

    case Event(StateTimeout, data) => stop() andThen { _ =>
      println("[Sold] not sold, deleting auction")
      data.getItem.seller ! NotSold
    }

    case ev => stop()
  }

  whenUnhandled {
    case Event(ev, dt) => stop() andThen { _ =>
      println("Got unknown event: " + ev + " with data: " + dt)
    }
  }

  onTermination {
    PartialFunction {
      _ => goto(Finished) applying NullEvent
    }
  }

  initialize()
}
