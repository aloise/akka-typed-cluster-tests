package name.aloise.actors

import java.time.LocalDateTime

import akka.actor.typed.receptionist.Receptionist
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

  final case class AddBattery(id: DeviceId, stats: BatteryStats) extends RegistryMessage

  final case class BatteryDeviceJoined(listing: Receptionist.Listing) extends RegistryMessage

  // Those are for simulation purposes only
  final case class ChargeRandom(amount: Long) extends RegistryMessage
  final case class DischargeRandom(amount: Long) extends RegistryMessage

  def main(
            name: String,
            batteries: Set[ActorRef[FunctioningBatteryState]] = Set.empty,
            requests: Map[DeviceId, EnergyRequest] = Map.empty,
            transactionLog: List[EnergyTransfer] = Nil
          ): Behavior[RegistryMessage] = Behaviors.setup { context =>

    // subscribe to the processor reference updates we're interested in
    val listingAdapter: ActorRef[Receptionist.Listing] = context.messageAdapter { listing =>
      BatteryDeviceJoined(listing)
    }
    context.system.receptionist ! Receptionist.Subscribe(BatteryActor.GetBatteryStatsKey, listingAdapter)

    Behaviors.receive {
      case (ctx, DeliverEnergyRequest(to, id, amount)) =>
        val newRequests = requests.updatedWith(id) {
          case None => Some(EnergyRequest(to, id, amount, 0L))
          case Some(existing) => Some(existing.copy(requestedAmount = existing.requestedAmount + amount))
        }

        // SEND Stats request here for every battery except for the source of this request
        batteries.filter(_ != to).foreach(_ ! GetBatteryStats(ctx.self))
        main(name, batteries, newRequests, transactionLog)

      case (_, BatteryStatsReport(from, batteryId, stats, _)) =>
        if(stats.currentCapacity > 0)
        Behaviors.same

      case (_, BatteryDeviceJoined(BatteryActor.GetBatteryStatsKey.Listing(listings))) =>
        main(name, listings, requests, transactionLog)

      case (ctx, AddBattery(id, stats)) =>
        ctx.log.info("Creating a Battery {} with capacity {}J of {}J", id, stats.currentCapacity, stats.maxCapacity)
        ctx.spawn(BatteryActor.functioningBattery(ctx.self, id, stats), name = "Battery-" + id.id.toString)
        Behaviors.same

      // Simulation messages
      case (_, ChargeRandom(amt)) =>
        val n = util.Random.nextInt(batteries.size)
        batteries.iterator.drop(n).next() ! Charge(amt)
        Behaviors.same

      case (_, DischargeRandom(amt)) =>
        val n = util.Random.nextInt(batteries.size)
        batteries.iterator.drop(n).next() ! Discharge(amt)
        Behaviors.same
    }
  }




}
