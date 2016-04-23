import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Ant {
  case object Forage
  case object Eat
  case object Retry

  def props(hive: ActorRef) = Props(new Ant(hive))
}

class Ant(hive: ActorRef) extends Actor with ActorLogging {
  import Ant._

  val t = context.system.scheduler

  def forage() = t.scheduleOnce(2 seconds, self, Forage)
  def retry() = t.scheduleOnce(1 seconds, self, Retry)
  def eat() = t.scheduleOnce(2 seconds, self, Eat)

  override def receive() = {
    case Forage => hive ! Hive.PutFood
    case Hive.PutFoodSuccess => forage()
    case Hive.PutFoodFailure => retry()
    case Retry => hive ! Hive.PutFood
    case Eat => hive ! Hive.GetFood
    case Hive.GetFoodSuccess => eat()
    case Hive.GetFoodFailure =>
      log.info(s"${self.path.name} has died")
      context.system.stop(self)
  }

  forage()
  eat()
}

