import akka.actor.{Actor, ActorLogging, Props, Terminated}

object World {
  case object Begin

  def props(ants: Int, maxFood: Int) = Props(new World(ants, maxFood))
}

class World(ants: Int, maxFood: Int) extends Actor with ActorLogging with Instrumented {
  import World._

  val died = metrics.counter(s"${self.path.toStringWithoutAddress}-antsdied")
  val hive = context.system.actorOf(Hive.props(maxFood), "hive")

  override def receive() = {
    case Begin =>
      (1 to ants).map { i => context.watch(context.system.actorOf(Ant.props(hive), s"ant-$i")) }

    case Terminated(actor) =>
      log.info(s"${actor.path.toStringWithoutAddress} has died")
      died += 1
  }
}

