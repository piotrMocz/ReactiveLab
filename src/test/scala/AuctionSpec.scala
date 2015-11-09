import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._

import language.postfixOps

import actors._
import data._

class AuctionSpec extends TestKit(ActorSystem("AuctionSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Auction System" must {

    "notify seller when auction had no winner" in {
      val aucSearch = system.actorOf(Props(classOf[AuctionSearch], List.empty[ActorRef]))
      val seller = system.actorOf(Props[Seller])

      within(51 seconds) {
        aucSearch ! AddAuction("Sprzedam Opla", 42.0f)

        expectMsgPF(hint = "any auction list") {
          case SearchResult(_) => true
        }

        expectNoMsg(49 seconds)

        expectMsg(NotSold)
      }
    }

    "notify seller and buyer when there is a winner" in {
      val seller = TestProbe("seller")
      val auction = system.actorOf(Props(classOf[Auction], Item(13.0f, seller.ref)), "A1")
      val buyer = TestProbe("buyer") // system.actorOf(Props(classOf[actors.Buyer], List(auction), "Henryk"))

      within(51 seconds) {
        buyer.send(auction, Bid(14.0f, buyer.ref))

        buyer.expectMsgPF() {
          case NewHighestOffer(_, _, _) => true
        }

        seller.expectMsgPF(max=50 seconds) {
          case Sold(_, _) => true
        }
        buyer.expectMsg(max=50 seconds, obj=YouWon)
      }
    }

    "award the win to the highest bidder" in {
      val seller = TestProbe("seller")
      val auction = system.actorOf(Props(classOf[Auction], Item(13.0f, seller.ref)), "A2")
      val buyer1 = TestProbe("buyer1")
      val buyer2 = TestProbe("buyer2")

      within(51 seconds) {
        buyer1.send(auction, Bid(14.0f, buyer1.ref))
        buyer2.send(auction, Bid(15.0f, buyer2.ref))

        buyer2.expectMsgPF() {
          case NewHighestOffer(_, _, _) => true
        }
        buyer2.expectMsg(max=50 seconds, obj=YouWon)
      }
    }

    "notify buyers of a new best offer" in {
      val buyer1 = TestProbe("buyer1")
      val buyer2 = TestProbe("buyer2")
      val seller = TestProbe("seller")
      val auction = system.actorOf(Props(classOf[Auction], Item(13.0f, seller.ref), List(buyer1.ref, buyer2.ref)), "A3")

      buyer1.send(auction, Bid(14.0f, buyer1.ref))
      buyer2.expectMsgPF() {
        case NewHighestOffer(newOffer, bidder, auc) if newOffer == 14.0f && bidder == buyer1.ref && auc == auction => true
      }
    }
  }


"Auction search" must {

    "allow adding auctions" in {
      val aucSearch = system.actorOf(Props(classOf[AuctionSearch], List.empty[ActorRef]))

      aucSearch ! AddAuction("auction", 42.0f)

      expectMsgPF(hint="1 element auction list") {
        case SearchResult(l) if l.length == 1 => true
      }
    }

    "allow searching for auctions" in {
      val probe1 = TestProbe("auction1")
      val probe2 = TestProbe("auction2")
      val aucSearch = system.actorOf(Props(classOf[AuctionSearch], List(probe1.ref, probe2.ref)))

      aucSearch ! Search("auction")

      expectMsgPF(hint="2 element auction list") {
        case SearchResult(l) if l.length == 2 => true
      }
    }

  }

  "Augmented buyer" must {

    "bid when his offer is beaten" in {
      val buyer = system.actorOf(Props(classOf[Buyer], "buyer", Some(123.4f)))
      val bidder = TestProbe("bidder")
      val auction = TestProbe("auction")
      buyer ! NewHighestOffer(42.0f, bidder.ref, auction.ref)

      auction.expectMsgPF() {
        case Bid(amt, b) if amt > 42.0f && b == buyer => true
      }
    }

  }

}
