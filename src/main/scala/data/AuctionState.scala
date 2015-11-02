package data

/**
 * Created by moczur on 11/2/15.
 */


sealed trait AuctionState
case object Created extends AuctionState
case object Activated extends AuctionState
case object Ignored extends AuctionState
case object ItemSold extends AuctionState
case object Finished extends AuctionState
