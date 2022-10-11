package it.unibo.pcd.pluviometer

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.*
import akka.cluster.typed.{Cluster, Join}
import it.unibo.pcd.utils.Protocol.{
  AlarmConfirmed,
  AlarmOver,
  CheckAlarm,
  CheckWaterLevel,
  Command,
  NotifyAlarm,
  NotifyState
}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object Pluviometer:

  val waterLevelAlarm: Int = 800

  def apply(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      period: FiniteDuration,
      serviceKey: String
  ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(CheckWaterLevel(), period)
        ctx.system.receptionist ! Receptionist.Register(ServiceKey[Command](serviceKey), ctx.self)
        baseBehavior(zoneManagerRef, position, ctx, false, 0)
      }
    }

  def baseBehavior(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      ctx: ActorContext[Command],
      inAlarm: Boolean,
      waterLevel: Int
  ): Behavior[Command] = Behaviors.receiveMessage {
    case CheckWaterLevel() =>
      val newWaterLevel = Math.max(0, waterLevel + Random.between(-5, 10))
      if (newWaterLevel >= waterLevelAlarm)
        zoneManagerRef ! NotifyAlarm()
        baseBehavior(zoneManagerRef, position, ctx, true, newWaterLevel)
      else baseBehavior(zoneManagerRef, position, ctx, false, newWaterLevel)
    case CheckAlarm() =>
      zoneManagerRef ! NotifyState(inAlarm)
      Behaviors.same
    case AlarmConfirmed() =>
      alarmBehaviour(zoneManagerRef, position, ctx)
    case _ => Behaviors.same
  }

  def alarmBehaviour(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      ctx: ActorContext[Command]
  ): Behavior[Command] = Behaviors.receiveMessage {
    case AlarmOver() =>
      baseBehavior(zoneManagerRef, position, ctx, false, 0)
    case _ => Behaviors.same
  }
