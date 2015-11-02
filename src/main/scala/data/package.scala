import akka.actor.ActorRef

import scala.concurrent.duration.{FiniteDuration, Duration}

/**
 * Created by moczur on 11/2/15.
 */


package object data {

  case class Item(minPrice: Float, seller: ActorRef)

  val timeScaleFactor = 100

  def scale(duration: FiniteDuration): FiniteDuration = duration / timeScaleFactor

}
