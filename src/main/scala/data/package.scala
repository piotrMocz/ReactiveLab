import akka.actor.ActorRef

/**
 * Created by moczur on 11/2/15.
 */


package object data {

  case class Item(minPrice: Float, seller: ActorRef)

}
