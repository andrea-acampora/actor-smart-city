package it.unibo.pcd.firestation

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.pcd.utils.Protocol.{
  AlarmOver,
  Command,
  FireStationAction,
  FireStationActionOver,
  InterventionRequest,
  NotifyFrontEnd,
  PluviometersChange,
  ZoneInAlarm,
  NotifyFireStation
}
import it.unibo.pcd.firestation.FireStation

import scala.collection.mutable

object FireStationFrontend:

  val width = 800
  val height = 600

  def apply(): Behavior[Command | Receptionist.Listing] =
    Behaviors.setup[Command | Receptionist.Listing] { ctx =>
      ctx.system.receptionist ! Receptionist.Subscribe(FireStation.service, ctx.self)
//      val gui = FireStationGUI(width, height)
      frontendLogic(ctx, Map.empty)
    }

  def frontendLogic(
      ctx: ActorContext[Command | Receptionist.Listing],
      fireStations: Map[Int, ActorRef[Command]]
  ): Behavior[Command | Receptionist.Listing] =
    Behaviors.receiveMessage {
      case msg: Receptionist.Listing =>
        val fireStationList: List[ActorRef[Command]] = msg.serviceInstances(FireStation.service).toList
        if (fireStationList == fireStations.values.toList) Behaviors.same
        else
          fireStationList.foreach(_ ! NotifyFrontEnd(ctx.self))
          frontendLogic(ctx, fireStations)
      case NotifyFireStation(zone: Int, fireStation: ActorRef[Command]) =>
        frontendLogic(ctx, fireStations + (zone -> fireStation))
      case ZoneInAlarm(zone) => ??? //display della zone in allarme
      case FireStationAction(zone: Int) =>
        if (fireStations.contains(zone))
          fireStations(zone) ! FireStationAction(zone)
        Behaviors.same
      case FireStationActionOver(zone: Int) =>
        if (fireStations.contains(zone))
          fireStations(zone) ! FireStationActionOver(zone)
        Behaviors.same
    }
