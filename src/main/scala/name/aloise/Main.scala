package name.aloise

import java.util.UUID
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector, Terminated}

object Main extends App {

  //  val system = ActorSystem("default")
}

object Battery {

  final case class GetBatteryStats(replyTo: ActorRef[BatteryStats])

  /**
   * Battery Stats response
   * @param batteryId
   * @param capacity - In Joules, might be improved with Spire and Refined Types (>= 0)
   * @param hmac
   */
  final case class BatteryStats(batteryId: UUID, capacity: Double, hmac: String)

  def battery(myId: UUID, capacity: Double): Behavior[GetBatteryStats] = Behaviors.receive { (context, message) =>
    context.log.info("Got request from {}!", message.replyTo)
    message.replyTo ! BatteryStats(null, 10.5, "TEST")
    Behavior.same
  }

}
