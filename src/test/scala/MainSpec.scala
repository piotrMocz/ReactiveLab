import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._

class MainSpec extends TestKit(ActorSystem("AuctionSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Auction System" must {

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
      val aucSearch = system.actorOf(Props(classOf[AuctionSearch], List.empty[ActorRef]))
      val seller = TestProbe("seller")
      val auction = system.actorOf(Props(classOf[Auction], Item(13.0f, seller.ref)), "A1")
      val buyer = TestProbe("buyer") // system.actorOf(Props(classOf[Buyer], List(auction), "Henryk"))

      within(51 seconds) {
        buyer.send(auction, Bid(14.0f, buyer.ref))

        seller.expectMsgPF(max=50 seconds) {
          case Sold(_, _) => true
        }
        buyer.expectMsg(max=50 seconds, obj=YouWon)
      }
    }
  }

}
