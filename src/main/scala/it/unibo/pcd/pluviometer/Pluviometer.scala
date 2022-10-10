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
        baseBehaviour(zoneManagerRef, position, ctx, false, 0)
      }
    }

  def baseBehaviour(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      ctx: ActorContext[Command],
      inAlarm: Boolean,
      waterLevel: Int
  ): Behavior[Command] = Behaviors.receiveMessage {
    case CheckWaterLevel() =>
      val newWaterLevel = Math.max(0, waterLevel + Random.between(-5, 10))
      ctx.log.info("Pluviometer in " + position + " water level: " + newWaterLevel)
      if (newWaterLevel >= waterLevelAlarm)
        ctx.log.info("Pluviometer in " + position + " level high!")
        zoneManagerRef ! NotifyAlarm()
        baseBehaviour(zoneManagerRef, position, ctx, true, newWaterLevel)
      else baseBehaviour(zoneManagerRef, position, ctx, false, newWaterLevel)
    case CheckAlarm() =>
      ctx.log.info("PROTOCOL: Pluviometer in " + position + "asked state from manager and it said " + inAlarm)
      zoneManagerRef ! NotifyState(inAlarm)
      Behaviors.same
    case AlarmConfirmed() =>
      ctx.log.info("PROTOCOL: Pluviometer in " + position + " get alarm confirmed!")
      alarmBehaviour(zoneManagerRef, position, ctx)
    case _ => Behaviors.same
  }

  def alarmBehaviour(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      ctx: ActorContext[Command]
  ): Behavior[Command] = Behaviors.receiveMessage {
    case AlarmOver() =>
      ctx.log.info("PROTOCOL: Pluviometer in " + position + " received alarm over!")
      baseBehaviour(zoneManagerRef, position, ctx, false, 0)
    case _ => Behaviors.same
  }
