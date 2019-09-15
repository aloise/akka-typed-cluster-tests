package name.aloise

import akka.actor.typed.ActorSystem
import akka.cluster.typed.{Cluster, ClusterSingleton}
import name.aloise.actors.{BatteryStats, DeviceId, RegistryActor}
import name.aloise.actors.RegistryActor.{AddBattery, ChargeRandom, DischargeRandom, RegistryMessage}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.Random

object RegistryApp extends App {

  val system: ActorSystem[RegistryMessage] = ActorSystem(RegistryActor.main("Registry"), "registry")
  //  val cluster = Cluster(system)
  //  val singletonManager = ClusterSingleton(system)

  for (_ <- 1 to 3) {
    val max = (Random.nextLong(900) + 100)*10
    system ! AddBattery(DeviceId.random, BatteryStats(Random.nextLong(max), max))
  }

  Console.println("Running the Registry cluster node ...")
  Console.println("Press <q> to stop, <c> to charge a random battery, <d> to discharge")

  var line = ""
  do {
    line = StdIn.readLine()
    line.trim match {
      case "d" =>
        println("Discharging a random battery")
        system ! DischargeRandom(500)

      case "c" =>
        println("Charging a random battery")
        system ! ChargeRandom(500)

      case _ =>
    }
  } while (line != "q")

  system.terminate()
  Await.result(system.whenTerminated, Duration.Inf)

}

