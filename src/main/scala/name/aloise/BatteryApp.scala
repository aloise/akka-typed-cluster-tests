package name.aloise

import java.util.UUID

import akka.actor.typed.ActorSystem
import akka.cluster.typed.Cluster
import name.aloise.actors.{DeviceId, RegistryActor}
import name.aloise.actors.RegistryActor.RegistryMessage

import scala.io.StdIn

class BatteryApp extends App {

  val batteryId = DeviceId(UUID.randomUUID())

  val system: ActorSystem[RegistryMessage] = ActorSystem(RegistryActor.main("Battery-" + batteryId), "battery")

  val cluster = Cluster(system)

  Console.println(s"Running the Battery cluster node on $batteryId ...")
  Console.println("Press <enter> to stop")

  StdIn.readLine()

  system.terminate()


}
