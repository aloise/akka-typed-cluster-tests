package name.aloise

import akka.actor.typed.ActorSystem
import akka.cluster.typed.{Cluster, ClusterSingleton}
import name.aloise.actors.RegistryActor
import name.aloise.actors.RegistryActor.RegistryMessage

import scala.io.StdIn

object RegistryApp extends App {

  val system: ActorSystem[RegistryMessage] = ActorSystem(RegistryActor.main("Registry"), "registry")

//  val cluster = Cluster(system)
//  val singletonManager = ClusterSingleton(system)


  Console.println("Running the Registry cluster node ...")
  Console.println("Press <enter> to stop")

  StdIn.readLine()

  system.terminate()


}

