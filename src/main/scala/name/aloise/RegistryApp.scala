package name.aloise

import akka.actor.typed.ActorSystem
import name.aloise.actors.RegistryActor
import name.aloise.actors.RegistryActor.RegistryMessage

import scala.io.StdIn

object RegistryApp extends App {

  val system: ActorSystem[RegistryMessage] = ActorSystem(RegistryActor.main("Registry"), "default")

  Console.println("Running the Registry cluster node ...")
  Console.println("Press <enter> to stop")

  StdIn.readLine()

  system.terminate()


}

