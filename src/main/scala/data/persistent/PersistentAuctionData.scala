package data.persistent

import akka.actor.ActorRef
import data.{Bid, Item}

/**
 * Created by moczur on 11/2/15.
 */


sealed trait PersistentAuctionData {
  def getItem: Item
  def getBestBid: Float = getItem.minPrice
  def getBestBidder: ActorRef = null
  def makeBid(bid: Bid): PersistentAuctionData
  def emptyData(item: Item): PersistentAuctionData = NoBid(item)
  def awardWin: PersistentAuctionData = {
    println("[ERROR] Applying event to the wrong kind of data.")
    null
  }
  def noWinner: PersistentAuctionData = {
    println("[ERROR] Applying event to the wrong kind of data.")
    null
  }
}

case class NoBid(item: Item) extends PersistentAuctionData {
  def makeBid(bid: Bid): Bids = Bids(bid.amount, bid.bidder, 1, item)
  override def noWinner: NoWinner = NoWinner(item)
  override def getItem: Item = item
}

case class Bids(bestBid: Float, bestBidder: ActorRef, bidCnt: Int, item: Item) extends PersistentAuctionData {
  def makeBid(bid: Bid): Bids = Bids(bid.amount, bid.bidder, bidCnt+1, item)
  override def awardWin: Winner = Winner(bestBid, bestBidder, bidCnt, item)
  override def getItem: Item = item
  override def getBestBid: Float = bestBid
  override def getBestBidder: ActorRef = bestBidder
}

case class Winner(bestBid: Float, bestBidder: ActorRef, bidCnt: Int, item: Item) extends PersistentAuctionData {
  def makeBid(bid: Bid): Winner = copy()
  override def getItem: Item = item
  override def getBestBid: Float = bestBid
  override def getBestBidder: ActorRef = bestBidder
}

case class NoWinner(item: Item) extends PersistentAuctionData {
  def makeBid(bid: Bid): NoWinner = copy()
  override def getItem: Item = item
}

case object NoData extends PersistentAuctionData {
  def makeBid(bid: Bid) = NoData
  override def getItem: Item = null
}

