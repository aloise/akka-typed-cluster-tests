package name.aloise.actors

import java.time.LocalDateTime

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object RegistryActor {

  type BatteryActorRef = ActorRef[FunctioningBatteryState]
  type EnergyTransferWithRef = List[(EnergyTransfer, BatteryActorRef)]

  case class EnergyTransfer(timestamp: LocalDateTime, from: DeviceId, to: DeviceId, amount: Long)

  case class EnergyRequest(to: BatteryActorRef, id: DeviceId, requestedAmount: Long, deliveredAmount: Long = 0)

  sealed trait RegistryMessage

  final case class DeliverEnergyRequest(deliverTo: BatteryActorRef, id: DeviceId, amount: Long) extends RegistryMessage

  /**
   * Battery Stats response
   *
   * @param batteryId UUID of the battery (from the factory)
   * @param stats     - Battery Stats
   * @param hmac      String - UUID and capacity (hardware-backed HMAC)
   */
  final case class BatteryStatsReport(from: BatteryActorRef, batteryId: DeviceId, stats: BatteryStats, hmac: String) extends RegistryMessage

  final case class AddBattery(id: DeviceId, stats: BatteryStats) extends RegistryMessage

  final case class BatteryDeviceJoined(listing: Receptionist.Listing) extends RegistryMessage

  // Those are for simulation purposes only
  final case class ChargeRandom(amount: Long) extends RegistryMessage

  final case class DischargeRandom(amount: Long) extends RegistryMessage

  /**
   * Main Regisrty Behavior
   *
   * @param name           Registry Name
   * @param batteries      list of batteries connected
   * @param requests       Energy delivery requests TODO - should use a better data structure / Queue ?
   * @param transactionLog Log of transactions (energy transfer) - should be persisted
   * @return
   */
  def main(
            name: String,
            batteries: Set[BatteryActorRef] = Set.empty,
            requests: List[EnergyRequest] = Nil,
            transactionLog: List[EnergyTransfer] = Nil
          ): Behavior[RegistryMessage] = Behaviors.setup { context =>

    // subscribe to the processor reference updates we're interested in
    val listingAdapter: ActorRef[Receptionist.Listing] = context.messageAdapter { listing =>
      BatteryDeviceJoined(listing)
    }
    context.system.receptionist ! Receptionist.Subscribe(BatteryActor.GetBatteryStatsKey, listingAdapter)

    Behaviors.receive {
      case (ctx, DeliverEnergyRequest(to, id, amount)) =>
        ctx.log.info("Battery {} requested {}J", id, amount)
        val newRequests = EnergyRequest(to, id, amount) :: requests

        // SEND Stats request here for every battery except for the source of this request
        batteries.filter(_ != to).foreach(_ ! GetBatteryStats(ctx.self))
        main(name, batteries, newRequests, transactionLog)

      case (ctx, BatteryStatsReport(from, fromBatteryId, stats, _)) =>

        // trying to find the first request in the queue to serve from the oldest one to a newest
        val (energyTransfers, updatedRequests, _) =
          requests.reverse.foldLeft[(EnergyTransferWithRef, List[EnergyRequest], Long)]((Nil, Nil, stats.currentCapacity)) {
            case ((receivers, requestsLeft, energyLeft), req) =>
              if (req.id == fromBatteryId || energyLeft <= 0) {
                // energy should not be delivered to itself or there is nothing more left
                (receivers, req :: requestsLeft, energyLeft)
              } else {
                val amtToTransfer =
                  if (energyLeft >= req.requestedAmount - req.deliveredAmount)
                    req.requestedAmount - req.deliveredAmount
                  else
                    energyLeft

                val transfer = (EnergyTransfer(LocalDateTime.now(), fromBatteryId, req.id, amtToTransfer), req.to)
                // removing the request from the list
                val updatedList =
                  if (amtToTransfer == req.requestedAmount - req.deliveredAmount)
                    requestsLeft
                  else
                    req.copy(deliveredAmount = req.deliveredAmount + amtToTransfer) :: requestsLeft

                (transfer :: receivers, updatedList, energyLeft - transfer._1.amount)
              }
          }

        // Sending deliver messages for all requested transfers
        energyTransfers.foreach { case (et, to) =>
          ctx.log.info("Delivering {}J of Energy from {} to {}", et.amount, et.from, et.to)
          from ! Deliver(to, et.to, et.amount)
        }

        if (energyTransfers.nonEmpty) {
          main(name, batteries, updatedRequests, energyTransfers.map(_._1) ::: transactionLog)
        } else {
          Behaviors.same
        }

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
