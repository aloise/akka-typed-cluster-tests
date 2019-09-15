package name.aloise

import akka.actor.typed.ActorSystem
import akka.cluster.typed.{Cluster, ClusterSingleton}
import name.aloise.actors.{BatteryStats, DeviceId, RegistryActor}
import name.aloise.actors.RegistryActor.{AddBattery, RegistryMessage}

import scala.io.StdIn
import scala.util.Random

object RegistryApp extends App {

  val system: ActorSystem[RegistryMessage] = ActorSystem(RegistryActor.main("Registry"), "registry")

//  val cluster = Cluster(system)
//  val singletonManager = ClusterSingleton(system)

  for(_ <- 1 to 20) {
    val max = Random.nextLong(9000) + 1000
    system ! AddBattery(DeviceId.random, BatteryStats( Random.nextLong(10000), max))
  }


  Console.println("Running the Registry cluster node ...")
  Console.println("Press <Enter> to stop")

  StdIn.readLine()

  system.terminate()


}

