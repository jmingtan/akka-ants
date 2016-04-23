import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.codahale.metrics.{JmxReporter, MetricRegistry}
import nl.grons.metrics.scala._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Application {
  val metricRegistry = new MetricRegistry
}

trait Instrumented extends InstrumentedBuilder {
  val metricRegistry = Application.metricRegistry
}

object Hive {
  case object GetFood
  case object GetFoodSuccess
  case object GetFoodFailure

  case object PutFood
  case object PutFoodSuccess
  case object PutFoodFailure

  def props(maxFood: Int) = Props(new HiveInstrumented(maxFood))
}

trait Hive extends Actor {
  import Hive._

  def maxFood: Int
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

class HiveInstrumented(val maxFood: Int) extends Hive
  with Instrumented with ActorInstrumentedLifeCycle with ReceiveCounterActor with ReceiveTimerActor with ReceiveExceptionMeterActor  {

  metrics.gauge("name") { self.path.toStringWithoutAddress }
  metrics.gauge("food") { food }
}

object Ant {
  case object Forage
  case object Eat
  case object Retry

  def props(hive: ActorRef) = Props(new AntInstrumented(hive))
}

trait Ant extends Actor with ActorLogging {
  import Ant._

  def hive: ActorRef
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

class AntInstrumented(val hive: ActorRef) extends Ant with Instrumented with ActorInstrumentedLifeCycle {

  metrics.gauge(self.path.name) { self.path.toStringWithoutAddress }
}

object World {
  case object Begin

  def props(ants: Int, maxFood: Int) = Props(new WorldInstrumented(ants, maxFood))
}

trait World extends Actor {
  import World._

  def maxFood: Int
  def ants: Int

  override def receive() = {
    case Begin =>
      val hive = context.system.actorOf(Hive.props(maxFood), "hive")
      (1 to ants).map { i => context.system.actorOf(Ant.props(hive), s"ant-$i") }
  }
}

class WorldInstrumented(val ants: Int, val maxFood: Int) extends World
  with Instrumented with ActorInstrumentedLifeCycle with ReceiveCounterActor with ReceiveTimerActor with ReceiveExceptionMeterActor {

  metrics.gauge("name") { self.path.toStringWithoutAddress }
  metrics.gauge("maxFood") { maxFood }
  metrics.gauge("ants") { ants }
}

object Main extends App {
  JmxReporter.forRegistry(Application.metricRegistry).build().start()
  ActorSystem("ants-sim").actorOf(World.props(ants = 100, maxFood = 100)) ! World.Begin
}
