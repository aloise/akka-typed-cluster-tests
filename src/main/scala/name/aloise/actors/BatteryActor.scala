package name.aloise.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import name.aloise.actors.RegistryActor.{BatteryStatsReport, RegistryMessage}

/**
 * Battery Stats. TODO - might be improved with Spire and Refined Types (>= 0)
 *
 * @param currentCapacity - in Joules
 * @param maxCapacity     - in Joules
 */
case class BatteryStats(currentCapacity: Long, maxCapacity: Long)

sealed trait Message

sealed trait BatteryMessage extends Message

trait FunctioningBatteryState extends BatteryMessage

trait FailedBatteryState extends BatteryMessage

trait FailReasonLogger extends Message


final case class GetBatteryStats(replyTo: ActorRef[RegistryMessage]) extends FunctioningBatteryState

/**
 * Send the particular amount of energy to the grid
 *
 * @param to     Receiver
 * @param amount Amount of energy in Joules
 */
final case class Deliver(to: ActorRef[FunctioningBatteryState], amount: Long) extends FunctioningBatteryState

/**
 * Receive the particular amount of energy from the grid
 *
 * @param from   Sender
 * @param amount Amount of energy in Joules
 */
final case class Receive(from: ActorRef[FunctioningBatteryState], amount: Long) extends FunctioningBatteryState

/**
 * The amount of energy the battery accepted from the local generator (solar panels)
 *
 * @param amount Amount of energy in Joules
 */
final case class Charge(amount: Long) extends FunctioningBatteryState

/**
 * The amount of energy consumed from the battery by local consumer
 *
 * @param amount Amount of energy in Joules
 */
final case class Discharge(amount: Long) extends FunctioningBatteryState

final case class GetFailReason(respondTo: ActorRef[FailReasonLogger]) extends FailedBatteryState

final case class FailReason(batteryId: DeviceId, reason: String) extends FailReasonLogger


object BatteryActor {

  def functioningBattery(myId: DeviceId, stats: BatteryStats): Behavior[FunctioningBatteryState] =
    Behaviors.receive {
      case (ctx, GetBatteryStats(to)) =>
        replyWithStats(to)(ctx.self, myId, stats)

      case (ctx, Deliver(to, amt)) =>
        val newAmount = Math.max(0, stats.currentCapacity - amt)
        val amtToDeliver = amt - newAmount
        if (amtToDeliver > 0) {
          to ! Receive(ctx.self, amtToDeliver)
          functioningBattery(myId, stats.copy(currentCapacity = newAmount))
        } else {
          Behavior.same
        }

      case (_, Receive(_, amount)) =>
        val newAmount = Math.min(stats.maxCapacity, stats.currentCapacity + amount)
        functioningBattery(myId, stats.copy(currentCapacity = newAmount))

      case (_, Charge(amt)) =>
        functioningBattery(myId, stats.copy(currentCapacity = Math.min(stats.maxCapacity, stats.currentCapacity + amt)))

      case (_, Discharge(amt)) =>
        functioningBattery(myId, stats.copy(currentCapacity = Math.max(0, stats.currentCapacity - amt)))

    }

  def failedBattery(myId: DeviceId, reason: String): Behavior[FailedBatteryState] = Behaviors.receive {
    case (_, GetFailReason(to)) =>
      to ! FailReason(myId, reason)
      Behavior.same
  }


  protected def replyWithStats[T](to: ActorRef[RegistryMessage])(self: ActorRef[FunctioningBatteryState], myId: DeviceId, stats: BatteryStats): Behavior[T] = {
    to ! BatteryStatsReport(self, myId, stats, hmac(myId.toString + stats.toString))
    Behaviors.same[T]
  }

  /**
   * Generates a HMAC of a string
   *
   * @param str       - Input string
   * @param secretKey - It should be a hardware baked secret
   * @return Hex Encoded string
   */
  private def hmac(str: String, secretKey: Array[Byte] = "hardware-baked-secret".getBytes): String = {
    val secret = new SecretKeySpec(secretKey, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secret)
    convertBytesToHex(mac.doFinal(str.getBytes))
  }

  private def convertBytesToHex(bytes: Seq[Byte]): String = {
    val sb = new StringBuilder
    for (b <- bytes) {
      sb.append(String.format("%02x", Byte.box(b)))
    }
    sb.toString
  }

}
