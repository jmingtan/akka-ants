import akka.actor.ActorSystem
import com.codahale.metrics.{JmxReporter, MetricRegistry}
import nl.grons.metrics.scala.InstrumentedBuilder

object AntApplication {
  val metricRegistry = new MetricRegistry()
}

trait Instrumented extends InstrumentedBuilder {
  val metricRegistry = AntApplication.metricRegistry
}

object Main extends App {
  JmxReporter.forRegistry(AntApplication.metricRegistry).build().start()
  ActorSystem("ants-sim").actorOf(World.props(ants = 100, maxFood = 100)) ! World.Begin
}
