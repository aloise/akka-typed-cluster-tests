package name.aloise.actors

import java.time.LocalDateTime

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object RegistryActor {

  case class EnergyTransfer(timestamp: LocalDateTime, from: DeviceId, to: DeviceId, amount: Long)
  case class EnergyRequest(to: ActorRef[FunctioningBatteryState], id: DeviceId, requestedAmount: Long, deliveredAmount: Long)

  sealed trait RegistryMessage
  final case class DeliverEnergyRequest(deliverTo: ActorRef[FunctioningBatteryState], id: DeviceId, amount: Long) extends RegistryMessage
  /**
   * Battery Stats response
   *
   * @param batteryId UUID of the battery (from the factory)
   * @param stats     - Battery Stats
   * @param hmac      String - UUID and capacity (hardware-backed HMAC)
   */
  final case class BatteryStatsReport(from: ActorRef[FunctioningBatteryState], batteryId: DeviceId, stats: BatteryStats, hmac: String) extends RegistryMessage


  def main(name: String, requests: Map[DeviceId, EnergyRequest] = Map.empty, transactionLog: List[EnergyTransfer] = Nil): Behavior[RegistryMessage] = Behaviors.receive {
    case (_, DeliverEnergyRequest(to, id, amount)) =>
      val newRequests = requests.updatedWith(id) {
        case None => Some(EnergyRequest(to, id, amount, 0L))
        case Some(existing) => Some(existing.copy(requestedAmount = existing.requestedAmount + amount))
      }

      // SEND Stats request here for every battery except for the source of this request
      main(name, newRequests, transactionLog)

    case (_, BatteryStatsReport(from, batteryId, stats, _)) =>
      Behaviors.same

  }

}
