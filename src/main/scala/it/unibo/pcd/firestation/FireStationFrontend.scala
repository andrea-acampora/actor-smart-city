package it.unibo.pcd.firestation

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.pcd.Launcher.Zone
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
import it.unibo.pcd.firestation.FireStation

import scala.collection.mutable

object FireStationFrontend:

  val width = 800
  val height = 600

  def apply(nZones: List[Zone]): Behavior[Command | Receptionist.Listing] =
    Behaviors.setup[Command | Receptionist.Listing] { ctx =>
      ctx.system.receptionist ! Receptionist.Subscribe(FireStation.service, ctx.self)
      val gui = FireStationGUI(width, height, nZones, ctx.self)
      frontendLogic(ctx, gui, Map.empty, Map.empty)
    }

  def frontendLogic(
      ctx: ActorContext[Command | Receptionist.Listing],
      gui: FireStationGUI,
      fireStations: Map[Int, ActorRef[Command]],
      pluviometersPerZone: Map[Int, Int]
  ): Behavior[Command | Receptionist.Listing] =
    Behaviors.receiveMessage {
      case msg: Receptionist.Listing =>
        val fireStationList: List[ActorRef[Command]] = msg.serviceInstances(FireStation.service).toList
        if (fireStationList == fireStations.values.toList) Behaviors.same
        else
          fireStationList.foreach(_ ! NotifyFrontEnd(ctx.self))
          frontendLogic(ctx, gui, fireStations, pluviometersPerZone)
      case NotifyFireStation(fireStationInZone: FireStationInZone, fireStation: ActorRef[Command]) =>
        gui.updatePluviometers((fireStationInZone.zone, fireStationInZone.numberOfPluviometers))
        frontendLogic(
          ctx,
          gui,
          fireStations + (fireStationInZone.zone -> fireStation),
          pluviometersPerZone + (fireStationInZone.zone -> fireStationInZone.numberOfPluviometers)
        )
      case ZoneInAlarm(zone: Int) =>
        gui.updateZoneStatus(zone, false)
        Behaviors.same
      case FireStationAction(zone: Int) =>
        if (fireStations.contains(zone))
          fireStations(zone) ! FireStationAction(zone)
        Behaviors.same
      case FireStationActionOver(zone: Int) =>
        if (fireStations.contains(zone))
          fireStations(zone) ! FireStationActionOver(zone)
        Behaviors.same
      case _ => Behaviors.same
    }
