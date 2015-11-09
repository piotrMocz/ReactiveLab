package data.persistent

import data.Bid

/**
 * Created by moczur on 11/9/15.
 */
sealed trait PersistentAuctionEvent

case class MakeBid(bid: Bid) extends PersistentAuctionEvent

case object RecognizeWinner extends PersistentAuctionEvent

case object HaveNoWinner extends PersistentAuctionEvent

case object DoNotBid extends PersistentAuctionEvent

case object NullEvent extends PersistentAuctionEvent
