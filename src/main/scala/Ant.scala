import akka.actor.{Actor, ActorRef, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Ant {
  case object Forage
  case object Eat
  case object Retry

  def props(hive: ActorRef) = Props(new Ant(hive))
}

class Ant(hive: ActorRef) extends Actor with Instrumented {
  import Ant._

  val t = context.system.scheduler

  val name = self.path.toStringWithoutAddress
  val status = metrics.gauge(s"$name-status")(self.path.toStringWithoutAddress)
  val eaten = metrics.counter(s"$name-eaten")
  val retries = metrics.counter(s"$name-retries")

  def forage() = t.scheduleOnce(2 seconds, self, Forage)
  def retry() = t.scheduleOnce(1 seconds, self, Retry)
  def eat() = t.scheduleOnce(2 seconds, self, Eat)

  override def receive() = {
    case Forage => hive ! Hive.PutFood

    case Hive.PutFoodSuccess => forage()

    case Hive.PutFoodFailure => retry()

    case Retry =>
      retries += 1
      hive ! Hive.PutFood

    case Eat => hive ! Hive.GetFood

    case Hive.GetFoodSuccess =>
      eaten += 1
      eat()

    case Hive.GetFoodFailure => context.system.stop(self)
  }

  forage()
  eat()
}

