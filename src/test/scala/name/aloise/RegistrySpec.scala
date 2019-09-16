package name.aloise

import akka.actor.testkit.typed.Effect.SpawnedAnonymous
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit}
import name.aloise.actors.RegistryActor.{AddBattery, ConnectBattery}
import name.aloise.actors.{BatteryActor, BatteryStats, DeviceId, FunctioningBatteryState, RegistryActor}
import org.scalatest.WordSpecLike

class RegistrySpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "RegistryActor" must {
    "add a battery" in {
      val testKit = BehaviorTestKit(RegistryActor.main("test"))
      val battery1 = AddBattery(DeviceId.random, BatteryStats(500, 1000))
      val battery2 = AddBattery(DeviceId.random, BatteryStats(1000, 2000))

      testKit.run(battery1)
      testKit.expectEffectType[SpawnedAnonymous[FunctioningBatteryState]]

      testKit.run(battery2)
      testKit.expectEffectType[SpawnedAnonymous[FunctioningBatteryState]]
    }

    "add a connect to a remote battery" in {
      val testKit = BehaviorTestKit(RegistryActor.main("test"))

      val battery1 = AddBattery(DeviceId.random, BatteryStats(500, 1000))
      val remoteBattery = BehaviorTestKit(BatteryActor.functioningBattery(testKit.ref, battery1.id, battery1.stats))

      testKit.run(ConnectBattery(remoteBattery.ref))
    }
  }
}
