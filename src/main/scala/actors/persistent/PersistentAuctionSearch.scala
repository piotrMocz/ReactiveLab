package actors.persistent

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

import data._

/**
 * Created by moczur on 11/2/15.
 */


class PersistentAuctionSearch(var auctions: List[ActorRef]) extends Actor {

  override def receive = LoggingReceive {
    case Search(keyword) =>
      println("Searching!!!")
      val matching = auctions filter { a => a.path.toString contains keyword }
      sender ! SearchResult(matching)
    //println("[actors.AuctionSearch] Matching auctions: " + matching.map(a => a.path))

    case AddAuction(name, minPr) =>
      println("[actors.AuctionSearch] Adding auction: " + name)
      val validName: String = name.filter(_.isLetterOrDigit)
      val newAuc = context.actorOf(Props(classOf[PersistentAuction], validName, Item(minPr, sender())), validName)
      auctions = newAuc :: auctions
      sender ! SearchResult(auctions)
  }

}