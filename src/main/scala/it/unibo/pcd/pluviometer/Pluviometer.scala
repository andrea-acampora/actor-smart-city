package it.unibo.pcd.pluviometer

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.*
import akka.cluster.typed.{Cluster, Join}

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
  val waterLevelAlarm: Int = 200
  val service: ServiceKey[Command] = ServiceKey[Command]("service")

  def apply(
      zoneManagerRef: ActorRef[Command],
      position: (Int, Int),
      period: FiniteDuration,
      serviceKey: String
  ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      val cluster = Cluster(ctx.system)
      cluster.manager ! Join(cluster.selfMember.address)
      ctx.log.info("state = " + cluster.state)
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(CheckWaterLevel, period)
        ctx.system.receptionist ! Receptionist.Register(Pluviometer.service, ctx.self)
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
    case CheckWaterLevel =>
      val newWaterLevel = waterLevel + Random.nextInt(10)
      ctx.log.info("pluviometer " + position + " waterLevel = " + newWaterLevel)
      if (newWaterLevel >= waterLevelAlarm)
        zoneManagerRef ! NotifyAlarm()
        ctx.log.info("pluviometer " + position + " waterLevel high")
        baseBehaviour(zoneManagerRef, position, ctx, true, newWaterLevel)
      else baseBehaviour(zoneManagerRef, position, ctx, false, newWaterLevel)
    case CheckAlarm() =>
      zoneManagerRef ! NotifyState(inAlarm)
      Behaviors.same
    case AlarmConfirmed() =>
      ctx.log.info("pluviometer " + position + " alarm confirmed")
      alarmBehaviour(zoneManagerRef, position, ctx)
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
