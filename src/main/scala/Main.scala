/**
 * Created by moczur on 10/19/15.
 */

import akka.event.LoggingReceive
import akka.actor._
import scala.concurrent.duration._

import scala.util.Random
import language.postfixOps

/* -----------------------------------------------------------------------------
 * Auction datatypes
 * ----------------------------------------------------------------------------- */
sealed trait AuctionMsg
case class Bid(amount: Float, bidder: ActorRef) extends  AuctionMsg
case object Relist extends AuctionMsg
case class Sold(price: Float, bidder: ActorRef) extends AuctionMsg
case object NotSold extends AuctionMsg
case class BidOrder(toBid: List[String]) extends AuctionMsg
case object YouWon extends AuctionMsg
case class Search(keyword: String) extends AuctionMsg
case class SearchResult(result: List[ActorRef]) extends AuctionMsg
case class MakeAuction(title: String, minPrice: Float) extends AuctionMsg
case class AddAuction(title: String, minPrice: Float) extends AuctionMsg


case class Item(minPrice: Float, seller: ActorRef)


sealed trait AuctionState
case object Created extends AuctionState
case object Activated extends AuctionState
case object Ignored extends AuctionState
case object Sold extends AuctionState
case object Finished extends AuctionState


sealed trait AuctionData
case class NoBid(item: Item) extends AuctionData
case class Bids(bestBid: Float, bestBidder: ActorRef, bidCnt: Int, item: Item) extends AuctionData
case class Winner(bestBid: Float, bestBidder: ActorRef, bidCnt: Int, item: Item) extends AuctionData
case class NoWinner(item: Item) extends AuctionData
case object NoData extends AuctionData


/* --------------------------------------------------------------------------------------
 * Auction state machine
 * -------------------------------------------------------------------------------------- */

class Auction(itemInit: Item) extends FSM[AuctionState, AuctionData] {
  println("Creating the Auction state machine")
  startWith(Created, NoBid(itemInit))

  when(Created, stateTimeout = 10 seconds) {
    case Event(Bid(amount, bidder), NoBid(item)) =>
      println("[Created] checking the minimum offer...")
      if (amount < item.minPrice) stay using NoBid(item)
      else goto(Activated) using Bids(amount, bidder, 1, item)

    case Event(StateTimeout, NoBid(item)) =>
      println("[Created] timed out with no winner...")
      goto(Ignored) using NoBid(item)
  }

  when(Ignored, stateTimeout = 30 seconds) {
    case Event(Relist, NoBid(item)) =>
      println("[Ignored] Relisting...")
      goto(Created) using NoBid(item)

    case Event(StateTimeout, NoBid(item)) =>
      println("[Ignored] Ignored timed out, finishing auction.")
      goto(Sold) using NoWinner(item)

    case Event(_, st) =>
      println("[Ignored] Staying in the ignored state...")
      stay using st
  }

  when(Activated, stateTimeout = 10 seconds) {
    case Event(Bid(amount, bidder), Bids(bestBid, bestBidder, cnt, item)) =>
      println("[Activated] checking if the offer is better...")
      if (amount <= bestBid) stay using Bids(bestBid, bestBidder, cnt, item)
      else goto(Activated) using Bids(amount, bidder, cnt+1, item)

    case Event(StateTimeout, Bids(bestBid, bestBidder, cnt, item)) =>
      println("[Activated] we have a winner!")
      goto(Sold) using Winner(bestBid, bestBidder, cnt, item)

    case Event(StateTimeout, NoBid(item)) =>
      println("[Activated] no winner, awaiting termination...")
      goto(Sold) using NoWinner(item)
  }

  when(Sold, stateTimeout = 10 seconds) {
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
      _ => goto(Finished) using(NoData)
    }
  }

  initialize()
}


/* --------------------------------------------------------------------------------------
 * Actors
 * -------------------------------------------------------------------------------------- */
class Buyer(name: String) extends Actor {
  def takeRand[T](l: List[T]): T = {
    val rnd = new Random()
    l(rnd.nextInt(l.length))
  }

  override def receive = LoggingReceive {
    case BidOrder(toBid) =>
      println("> [Buyer] Looking up auctions.")
      toBid.foreach { kwd =>
        val aucSearch = context.system.actorSelection("/user/auctionSearch")
        aucSearch ! Search(kwd)
        //takeRand(auctions) ! Bid(bidAmount, self)
      }

    case SearchResult(auctions) =>
      println("> [Buyer] Bidding in auctions...")
      auctions.foreach { a =>
        val bidAmount = new Random().nextFloat() * 100.0f
        a ! Bid(bidAmount, self)
      }

    case YouWon =>
      println("> [Buyer] I won!!! (" + name + ")")

    case _ =>
      println("> [Buyer] Unknown message.")
  }
}

class Seller extends Actor {
  override def receive = LoggingReceive {
    case AddAuction(name, minPr) =>
      val aucSearch = context.system.actorSelection("/user/auctionSearch")
      println("[Seller] Found auctionSearch:   " + aucSearch)
      aucSearch ! AddAuction(name, minPr)

    case Sold(price, bidder) =>
      println("> [Seller] I sold an item for " + price + ", nice!")

    case NotSold =>
      println("> [Seller] I didn't sell a thing.")

    case _ =>
      println("> [Seller] Unknown message.")
  }
}

class AuctionSearch(var auctions: List[ActorRef]) extends Actor {
  override def receive = LoggingReceive {
    case Search(keyword) =>
      println("Searching!!!")
      val matching = auctions filter { a => a.path.toString contains keyword }
      sender ! SearchResult(matching)
      //println("[AuctionSearch] Matching auctions: " + matching.map(a => a.path))

    case AddAuction(name, minPr) =>
      println("[AuctionSearch] Adding auction: " + name)
      val newAuc = context.actorOf(Props(classOf[Auction], Item(minPr, sender())), name.filter(_.isLetterOrDigit))
      auctions = newAuc :: auctions
      sender ! SearchResult(auctions)
  }
}

/* --------------------------------------------------------------------------------------
 * System creation and testing
 * -------------------------------------------------------------------------------------- */
object Main extends App {
  val system = ActorSystem("AuctionSystem")

  val seller = system.actorOf(Props[Seller])
  val aucsearch = system.actorOf(Props(classOf[AuctionSearch], List.empty[ActorRef]), "auctionSearch")

  seller ! AddAuction("sprzedam opla", 100.0f)
  aucsearch ! Search("sprzedam")


  //  println("Auction search path: " + aucsearch.path)
  //  val lookedUp = system.actorSelection("/user/auctionSearch")
  //  println("Selection:\n" + lookedUp)

  //  val item1 = Item(13.0f, seller)
  //  val item2 = Item(100.0f, seller)
  //  val auction1 = system.actorOf(Props(classOf[Auction], Item(13.0f, seller)), "A1")
  //  val auction2 = system.actorOf(Props(classOf[Auction], Item(133.0f, seller)), "A2")
  //
  //  val buyer1 = system.actorOf(Props(classOf[Buyer], List(auction1, auction2), "Henryk"))
  //  val buyer2 = system.actorOf(Props(classOf[Buyer], List(auction1, auction2), "Janina"))
  //  val buyer3 = system.actorOf(Props(classOf[Buyer], List(auction1, auction2), "Jadwiga"))
  //
  //  import system.dispatcher
  //
  //  system.scheduler.scheduleOnce(5 seconds, buyer1, BidOrder)
  //  system.scheduler.scheduleOnce(6 seconds, buyer1, BidOrder)
  //  system.scheduler.schedule(35 seconds, 6 seconds, buyer2, BidOrder)
  //  system.scheduler.schedule(55 seconds, 4 seconds, buyer3, BidOrder)

  //  auction ! Bid(16.0f, buyer1)
  //  auction ! Bid(10.5f, buyer1)

  //  system.awaitTermination()
}
