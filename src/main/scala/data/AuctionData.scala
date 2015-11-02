package data

import akka.actor.ActorRef

/**
 * Created by moczur on 11/2/15.
 */


sealed trait AuctionData
case class NoBid(item: Item) extends AuctionData
case class Bids(bestBid: Float, bestBidder: ActorRef, bidCnt: Int, item: Item) extends AuctionData
case class Winner(bestBid: Float, bestBidder: ActorRef, bidCnt: Int, item: Item) extends AuctionData
case class NoWinner(item: Item) extends AuctionData
case object NoData extends AuctionData

