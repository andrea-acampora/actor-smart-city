package it.unibo.pcd.pluviometer

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.cluster.typed.{Cluster, Join}
import it.unibo.pcd.utils.Protocol.{
  AlarmConfirmed,
  AlarmOver,
  CheckAlarm,
  Command,
  InterventionRequest,
  NotifyAlarm,
  NotifyState,
  PluviometersChange
}

object ZoneManager:

  case class Notification(alarm: Int, notAlarm: Int)

  def apply(
      serviceKey: String,
      fireStation: ActorRef[Command]
  ): Behavior[Command | Receptionist.Listing] =
    Behaviors.setup[Command | Receptionist.Listing] { ctx =>
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
        if (pluviometersList == pluviometers) Behaviors.same
        else
          ctx.log.info("PROTOCOL: ZONE_MANAGER of " + serviceKey + " received pluviometers: " + pluviometersList.size)
          fireStation ! PluviometersChange(pluviometersList)
          zoneManagerLogic(ctx, serviceKey, fireStation, pluviometersList)
      case NotifyAlarm() =>
        ctx.log.info("PROTOCOL: manager of " + serviceKey + " notified an alarm")
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
      if (pluviometersList == pluviometers) Behaviors.same
      else
        ctx.log.info("PROTOCOL: ZONE_MANAGER of " + serviceKey + " received pluviometers: " + pluviometersList.size)
        fireStation ! PluviometersChange(pluviometersList)
        manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometersList, notifications)
    case NotifyState(state) =>
      val updatedNotifications: Notification =
        state match
          case true => Notification(notifications.alarm + 1, notifications.notAlarm)
          case _ => Notification(notifications.alarm, notifications.notAlarm + 1)
      if (updatedNotifications.alarm + updatedNotifications.notAlarm == pluviometers.size)
        if (updatedNotifications.alarm > updatedNotifications.notAlarm)
          pluviometers.foreach(_ ! AlarmConfirmed())
          fireStation ! InterventionRequest(ctx.self)
          waitingFireFightersBehaviour(ctx, serviceKey, fireStation, pluviometers, updatedNotifications)
        else zoneManagerLogic(ctx, serviceKey, fireStation, pluviometers)
      else
        manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometers, updatedNotifications)
    case _ => Behaviors.same
  }

  def waitingFireFightersBehaviour(
      ctx: ActorContext[Command | Receptionist.Listing],
      serviceKey: String,
      fireStation: ActorRef[Command],
      pluviometers: List[ActorRef[Command]],
      notifications: Notification
  ): Behavior[Command | Receptionist.Listing] = Behaviors.receiveMessage {
    case msg: Receptionist.Listing =>
      val pluviometersList: List[ActorRef[Command]] = msg.serviceInstances(ServiceKey[Command](serviceKey)).toList
      if (pluviometersList == pluviometers) Behaviors.same
      else
        ctx.log.info("PROTOCOL: ZONE_MANAGER of " + serviceKey + " received pluviometers: " + pluviometersList.size)
        fireStation ! PluviometersChange(pluviometersList)
        waitingFireFightersBehaviour(ctx, serviceKey, fireStation, pluviometersList, notifications)
    case AlarmOver() =>
      pluviometers.foreach(_ ! AlarmOver())
      zoneManagerLogic(ctx, serviceKey, fireStation, pluviometers)
    case _ => Behaviors.same
  }
