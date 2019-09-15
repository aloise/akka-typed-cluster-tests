package name.aloise

import java.util.UUID

import akka.actor.typed.ActorSystem
import name.aloise.actors.{DeviceId, RegistryActor}
import name.aloise.actors.RegistryActor.RegistryMessage

import scala.io.StdIn

object BatteryApp extends App {

  val batteryId = DeviceId(UUID.randomUUID())

  val system: ActorSystem[RegistryMessage] = ActorSystem(RegistryActor.main("Battery-" + batteryId), "default")

  Console.println(s"Running the Battery cluster node on $batteryId ...")
  Console.println("Press <enter> to stop")

  StdIn.readLine()

  system.terminate()


}