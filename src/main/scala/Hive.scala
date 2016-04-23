import akka.actor.{Actor, Props}

object Hive {
  case object GetFood
  case object GetFoodSuccess
  case object GetFoodFailure

  case object PutFood
  case object PutFoodSuccess
  case object PutFoodFailure

  case object SetFire

  def props(maxFood: Int) = Props(new Hive(maxFood))
}

class Hive(maxFood: Int) extends Actor with Instrumented {
  import Hive._

  var food = 0

  val name = self.path.toStringWithoutAddress
  val foodGauge = metrics.gauge(s"$name-foodgauge")(food)
  val foodHist = metrics.histogram(s"$name-foodhist")

  override def receive() = {
    case GetFood =>
      if (food > 0) {
        food -= 1
        foodHist += food
        sender() ! GetFoodSuccess
      } else {
        sender() ! GetFoodFailure
      }

    case PutFood =>
      if (food < maxFood) {
        food += 1
        foodHist += food
        sender() ! PutFoodSuccess
      } else {
        sender() ! PutFoodFailure
      }
  }
}
