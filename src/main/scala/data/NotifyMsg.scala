package data

import akka.actor.ActorRef

case class NotifyMsg(auctionName: String, bestBidder: ActorRef, currentPrice : Float)
