package name.aloise

import akka.actor.typed.ActorSystem
import cats.effect
import cats.effect.{ExitCode, IO, IOApp}
import name.aloise.actors.RegistryActor.LogStats
import name.aloise.actors.{BatteryStats, DeviceId, RegistryActor}
import name.aloise.actors.RegistryActor.{AddBattery, ChargeRandom, DischargeRandom, RegistryMessage}
import scala.io.StdIn
import scala.util.Random

object RegistryApp extends IOApp {

  def readCommand: IO[Either[ExitCode, RegistryMessage]] =
    IO(StdIn.readChar()).flatMap {
      case 'd' =>
        IO(println("Discharging a random battery")).map { _ =>
          Right(DischargeRandom(500))
        }

      case 'c' =>
        IO(println("Charging a random battery")).map { _ =>
          Right(ChargeRandom(500))
        }

      case 'l' =>
        IO.pure(Right(LogStats))

      case _ =>
        IO.pure(Left(ExitCode(0)))
    }

  //  val cluster = Cluster(system)
  //  val singletonManager = ClusterSingleton(system)

  def createActors(system: ActorSystem[RegistryMessage], n: Int = 5): IO[Unit] = IO {
    for (_ <- 1 to n) {
      val max = (Random.nextLong(900) + 100) * 10
      system ! AddBattery(DeviceId.random, BatteryStats(Random.nextLong(max), max))
    }
  }

  def readLoop(system: ActorSystem[RegistryMessage]): IO[ExitCode] = {
    readCommand flatMap {
      case Left(ec) => IO.pure(ec)
      case Right(cmd) =>
        IO {
          system ! cmd
        } flatMap (_ => readLoop(system))
    }
  }

  override def run(args: List[String]): IO[effect.ExitCode] = {
    for {
      _ <- IO(Console.println("Running the Registry cluster node ..."))
      _ <- IO(Console.println("Press <q> to stop, <c> to charge a random battery, <d> to discharge, <l> to show logs"))
      system <- IO.pure(ActorSystem(RegistryActor.main("Registry"), "registry"))
      _ <- createActors(system)
      result <- readLoop(system)
      _ <- IO(system.terminate())
      _ <- IO.fromFuture(IO.pure(system.whenTerminated))
    } yield result
  }
}

