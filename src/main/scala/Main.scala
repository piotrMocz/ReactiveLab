
import akka.event.LoggingReceive
import akka.actor._
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._


import scala.util.Random
import language.postfixOps

import actors._
import data._


object Main extends App {
  val config = ConfigFactory.load()
  val serversystem = ActorSystem("Reactive5", config.getConfig("serverApp").withFallback(config))
  val account = serversystem.actorOf(Props[AuctionPublisher], "publisher")

  val system = ActorSystem("Reactive6", config.getConfig("clientApp").withFallback(config))

  val seller = system.actorOf(Props[Seller])
  // val aucsearch = system.actorOf(Props(classOf[AuctionSearch], List.empty[ActorRef]), "auctionSearch")

  //seller ! AddAuction("sprzedam opla", 100.0f)
  //aucsearch ! Search("sprzedam")


  //println("actors.Auction search path: " + aucsearch.path)
  // val lookedUp = system.actorSelection("/user/auctionSearch")

  //val item1 = Item(100.0f, seller)
  //val item2 = Item(100.0f, seller)

  val auction1 = system.actorOf(Props(classOf[actors.Auction], Item(13.0f, seller)), "A1")
  val auction2 = system.actorOf(Props(classOf[actors.Auction], Item(133.0f, seller)), "A2")

  val buyer1 = system.actorOf(Props(classOf[actors.Buyer], "Heniu", Some(200.0f)))
  val buyer2 = system.actorOf(Props(classOf[actors.Buyer], "Janina",Some(100.0f)))

  import system.dispatcher

  // system.scheduler.scheduleOnce(5 seconds, buyer1, BidOrder)
  // system.scheduler.scheduleOnce(6 seconds, buyer1, BidOrder)

  system.scheduler.scheduleOnce(1 seconds, auction1, Bid(16.0f, buyer1))
  system.scheduler.scheduleOnce(7 seconds, auction2, Bid(18.0f, buyer1))
//  auction1 ! Bid(10.5f, buyer1)

  system.awaitTermination()
}

