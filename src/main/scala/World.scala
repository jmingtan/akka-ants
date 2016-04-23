import akka.actor.{Actor, Props}

object World {
  case object Begin

  def props(ants: Int, maxFood: Int) = Props(new World(ants, maxFood))
}

class World(ants: Int, maxFood: Int) extends Actor {
  import World._

  override def receive() = {
    case Begin =>
      val hive = context.system.actorOf(Hive.props(maxFood), "hive")
      (1 to ants).map { i => context.system.actorOf(Ant.props(hive), s"ant-$i") }
  }
}

