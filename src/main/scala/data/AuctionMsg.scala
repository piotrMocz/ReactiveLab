package data

import akka.actor.ActorRef

/**
 * Created by moczur on 11/2/15.
 */


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
case class NewHighestOffer(newOffer: Float, bidder: ActorRef, auction: ActorRef) extends AuctionMsg