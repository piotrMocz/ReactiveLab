/**
 * Created by moczur on 10/19/15.
 */

import akka.event.LoggingReceive
import akka.actor._
import scala.concurrent.duration._

import scala.util.Random
import language.postfixOps

import actors._
import data._


object Main extends App {
  val system = ActorSystem("AuctionSystem")

  val seller = system.actorOf(Props[Seller])
  val aucsearch = system.actorOf(Props(classOf[AuctionSearch], List.empty[ActorRef]), "auctionSearch")

  seller ! AddAuction("sprzedam opla", 100.0f)
  aucsearch ! Search("sprzedam")


  //  println("actors.persistent.Auction search path: " + aucsearch.path)
  //  val lookedUp = system.actorSelection("/user/auctionSearch")
  //  println("Selection:\n" + lookedUp)

  //  val item1 = Item(13.0f, seller)
  //  val item2 = Item(100.0f, seller)
  //  val auction1 = system.actorOf(Props(classOf[actors.persistent.Auction], Item(13.0f, seller)), "A1")
  //  val auction2 = system.actorOf(Props(classOf[actors.persistent.Auction], Item(133.0f, seller)), "A2")
  //
  //  val buyer1 = system.actorOf(Props(classOf[actors.Buyer], List(auction1, auction2), "Henryk"))
  //  val buyer2 = system.actorOf(Props(classOf[actors.Buyer], List(auction1, auction2), "Janina"))
  //  val buyer3 = system.actorOf(Props(classOf[actors.Buyer], List(auction1, auction2), "Jadwiga"))
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
