package data.persistent

import akka.persistence.fsm.PersistentFSM.FSMState

/**
 * Created by moczur on 11/2/15.
 */

sealed trait PersistentAuctionState extends FSMState

case object Created extends PersistentAuctionState {
  override def identifier: String = "Created"
}

case object Activated extends PersistentAuctionState  {
  override def identifier: String = "Activated"
}

case object Ignored extends PersistentAuctionState  {
  override def identifier: String = "Ignored"
}

case object ItemSold extends PersistentAuctionState  {
  override def identifier: String = "ItemSold"
}

case object Finished extends PersistentAuctionState  {
  override def identifier: String = "Finished"
}
