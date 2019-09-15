package name.aloise

import akka.actor.typed.ActorSystem
import name.aloise.actors.RegistryActor
import name.aloise.actors.RegistryActor.RegistryMessage

import scala.io.StdIn

object Main extends App {

  val system: ActorSystem[RegistryMessage] = ActorSystem(RegistryActor.main("Main"), "default")

  Console.println("Running the cluster node ...")
  Console.println("Press <enter> to stop")

  StdIn.readLine()

  system.terminate()


}

