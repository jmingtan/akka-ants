import akka.actor.{Actor, Props}

object Hive {
  case object GetFood
  case object GetFoodSuccess
  case object GetFoodFailure

  case object PutFood
  case object PutFoodSuccess
  case object PutFoodFailure

  def props(maxFood: Int) = Props(new Hive(maxFood))
}

class Hive(maxFood: Int) extends Actor {
  import Hive._

  var food = 0

  override def receive() = {
    case GetFood =>
      if (food > 0) {
        food -= 1
        sender() ! GetFoodSuccess
      } else {
        sender() ! GetFoodFailure
      }
    case PutFood =>
      if (food < maxFood) {
        food += 1
        sender() ! PutFoodSuccess
      } else {
        sender() ! PutFoodFailure
      }
  }
}
