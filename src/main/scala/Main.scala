import akka.actor.ActorSystem

object Main extends App {
  ActorSystem("ants-sim").actorOf(World.props(ants = 100, maxFood = 100)) ! World.Begin
}
