package data.persistent

import akka.persistence.fsm.PersistentFSM.FSMState

/**
 * Created by moczur on 11/2/15.
 */

sealed trait PersistentAuctionState extends FSMState

case object Created extends PersistentAuctionState {
  override def identifier: String = "created"
}

case object Activated extends PersistentAuctionState  {
  override def identifier: String = "activated"
}

case object Ignored extends PersistentAuctionState  {
  override def identifier: String = "ignored"
}

case object ItemSold extends PersistentAuctionState  {
  override def identifier: String = "itemsold"
}

case object Finished extends PersistentAuctionState  {
  override def identifier: String = "finished"
}
