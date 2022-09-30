package it.unibo.pcd.pluviometer

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.*
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object Pluviometer:

  trait Message {}
  sealed trait Command extends Message
  private case object CheckWaterLevel extends Command
  final case class NotifyAlarm() extends Command
  final case class NotifyState(inAlarm: Boolean) extends Command
  final case class CheckAlarm() extends Command
  final case class AlarmConfirmed() extends Command
  final case class StopAlarm() extends Command

  def apply(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      period: FiniteDuration,
      serviceKey: String
  ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(ServiceKey[Command](serviceKey), ctx.self)
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(CheckWaterLevel, period)
        baseBehaviour(zoneManagerRef, position, ctx, false, 0)
      }
    }

  def baseBehaviour(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      ctx: ActorContext[Command],
      inAlarm: Boolean,
      waterLevel: Double
  ): Behavior[Command] = Behaviors.receiveMessage {
    case CheckWaterLevel =>
      if (Random.nextInt(10) == 1)
        zoneManagerRef ! NotifyAlarm()
        baseBehaviour(zoneManagerRef, position, ctx, true, waterLevel)
      Behaviors.same
    case CheckAlarm() =>
      zoneManagerRef ! NotifyState(inAlarm)
      Behaviors.same
    case AlarmConfirmed() => alarmBehaviour(zoneManagerRef, position, ctx)
    case _ => Behaviors.same
  }

  def alarmBehaviour(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      ctx: ActorContext[Command]
  ): Behavior[Command] = Behaviors.receiveMessage {
    case StopAlarm() =>
      baseBehaviour(zoneManagerRef, position, ctx, false, 0)
    case _ => Behaviors.same
  }
