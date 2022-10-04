package it.unibo.pcd.pluviometer

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.pcd.pluviometer.Pluviometer.{
  AlarmConfirmed,
  CheckAlarm,
  Command,
  NotifyAlarm,
  NotifyState,
  baseBehaviour
}
import akka.cluster.typed.{Cluster, Join}

object ZoneManager:

  case class Notification(alarm: Int, notAlarm: Int)

  def apply(
      serviceKey: String,
      fireStation: ActorRef[Command]
  ): Behavior[Command | Receptionist.Listing] =
    Behaviors.setup[Command | Receptionist.Listing] { ctx =>
      val cluster = Cluster(ctx.system)
      cluster.manager ! Join(cluster.selfMember.address)
      ctx.system.receptionist ! Receptionist.Subscribe(ServiceKey[Command](serviceKey), ctx.self)
      zoneManagerLogic(ctx, serviceKey, fireStation, List.empty)
    }

  def zoneManagerLogic(
      ctx: ActorContext[Command | Receptionist.Listing],
      serviceKey: String,
      fireStation: ActorRef[Command],
      pluviometers: List[ActorRef[Command]]
  ): Behavior[Command | Receptionist.Listing] =
    Behaviors.receiveMessage {
      case msg: Receptionist.Listing =>
        val pluviometersList: List[ActorRef[Command]] = msg.serviceInstances(ServiceKey[Command](serviceKey)).toList
        ctx.log.info("list = " + pluviometersList)
        if (pluviometersList == pluviometers)
          Behaviors.same
        else
          zoneManagerLogic(ctx, serviceKey, fireStation, pluviometersList)
      case NotifyAlarm() =>
        ctx.log.info("received alarm, size = " + pluviometers.size)

        pluviometers.foreach(_ ! CheckAlarm())
        manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometers, Notification(0, 0))
      case _ => Behaviors.same
    }

  def manageAlarmBehaviour(
      ctx: ActorContext[Command | Receptionist.Listing],
      serviceKey: String,
      fireStation: ActorRef[Command],
      pluviometers: List[ActorRef[Command]],
      notifications: Notification
  ): Behavior[Command | Receptionist.Listing] = Behaviors.receiveMessage {
    case msg: Receptionist.Listing =>
      val pluviometersList: List[ActorRef[Command]] = msg.serviceInstances(ServiceKey[Command](serviceKey)).toList
      if (pluviometersList == pluviometers)
        Behaviors.same
      else
        manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometersList, notifications)
    case NotifyState(state) =>
      val updatedNotifications: Notification =
        state match
          case true => Notification(notifications.alarm + 1, notifications.notAlarm)
          case _ => Notification(notifications.alarm, notifications.notAlarm + 1)
      if (updatedNotifications.alarm + updatedNotifications.notAlarm == pluviometers.size)
        if (updatedNotifications.alarm > updatedNotifications.notAlarm)
          pluviometers.foreach(_ ! AlarmConfirmed())
          //behavior per aspettare messaggio firestation
          manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometers, updatedNotifications)
        else zoneManagerLogic(ctx, serviceKey, fireStation, pluviometers)
      else
        manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometers, updatedNotifications)
    case _ => Behaviors.same
  }
