package it.unibo.pcd.firestation

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.pcd.utils.Protocol.{
  AlarmOver,
  Command,
  FireStationAction,
  FireStationActionOver,
  FireStationInZone,
  InterventionRequest,
  NotifyFireStation,
  NotifyFrontEnd,
  PluviometersChange,
  ZoneInAlarm
}

object FireStation:

  val service: ServiceKey[Command] = ServiceKey[Command]("fireStation")

  def apply(zone: Int): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    ctx.system.receptionist ! Receptionist.Register(service, ctx.self)
    fireStationLogic(ctx, Option.empty, false, false, List.empty, zone, List.empty)
  }

  def fireStationLogic(
      ctx: ActorContext[Command],
      zoneManager: Option[ActorRef[Command]],
      zoneInAlarm: Boolean,
      fireStationState: Boolean,
      pluviometers: List[ActorRef[Command]],
      zone: Int,
      frontends: List[ActorRef[Command]]
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case InterventionRequest(zoneManager: ActorRef[Command]) =>
        frontends.foreach(_ ! ZoneInAlarm(zone))
        fireStationLogic(ctx, Some(zoneManager), true, fireStationState, pluviometers, zone, frontends)
      case PluviometersChange(pluviometerList) =>
        frontends.foreach(_ ! NotifyFireStation(FireStationInZone(zone, pluviometerList.size), ctx.self))
        fireStationLogic(ctx, zoneManager, zoneInAlarm, fireStationState, pluviometerList, zone, frontends)
      case FireStationAction(zone: Int) =>
        fireStationInAction(ctx, zoneManager, zoneInAlarm, fireStationState, pluviometers, zone, frontends)
      case NotifyFrontEnd(frontEnd: ActorRef[Command]) =>
        frontEnd ! NotifyFireStation(FireStationInZone(zone, pluviometers.size), ctx.self)
        if (frontends.contains(frontEnd))
          Behaviors.same
        else
          fireStationLogic(ctx, zoneManager, zoneInAlarm, fireStationState, pluviometers, zone, frontEnd :: frontends)
      case _ => Behaviors.same
    }

  def fireStationInAction(
      ctx: ActorContext[Command],
      zoneManager: Option[ActorRef[Command]],
      zoneInAlarm: Boolean,
      fireStationState: Boolean,
      pluviometers: List[ActorRef[Command]],
      zone: Int,
      frontends: List[ActorRef[Command]]
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case PluviometersChange(pluviometerList) =>
        frontends.foreach(_ ! NotifyFireStation(FireStationInZone(zone, pluviometerList.size), ctx.self))
        fireStationInAction(ctx, zoneManager, zoneInAlarm, fireStationState, pluviometerList, zone, frontends)
      case FireStationActionOver(zone: Int) =>
        if (zoneManager.isDefined) zoneManager.get ! AlarmOver()
        fireStationLogic(ctx, zoneManager, false, fireStationState, pluviometers, zone, frontends)
      case NotifyFrontEnd(frontEnd: ActorRef[Command]) =>
        frontEnd ! NotifyFireStation(FireStationInZone(zone, pluviometers.size), ctx.self)
        if (frontends.contains(frontEnd))
          Behaviors.same
        else
          fireStationInAction(
            ctx,
            zoneManager,
            zoneInAlarm,
            fireStationState,
            pluviometers,
            zone,
            frontEnd :: frontends
          )
      case _ => Behaviors.same
    }
