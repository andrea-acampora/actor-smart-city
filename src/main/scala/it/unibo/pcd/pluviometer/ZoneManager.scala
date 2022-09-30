package it.unibo.pcd.pluviometer

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.pcd.pluviometer.Pluviometer.NotifyAlarm
import it.unibo.pcd.pluviometer.Pluviometer.NotifyState
import it.unibo.pcd.pluviometer.Pluviometer.Command

object ZoneManager:

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
        if (pluviometersList == pluviometers)
          Behaviors.same
        else
          zoneManagerLogic(ctx, serviceKey, fireStation, pluviometersList)
      case NotifyAlarm() => manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometers)
      case _ => Behaviors.same
    }

  def manageAlarmBehaviour(
      ctx: ActorContext[Command | Receptionist.Listing],
      serviceKey: String,
      fireStation: ActorRef[Command],
      pluviometers: List[ActorRef[Command]]
  ): Behavior[Command | Receptionist.Listing] = Behaviors.receiveMessage {
    case msg: Receptionist.Listing =>
      val pluviometersList: List[ActorRef[Command]] = msg.serviceInstances(ServiceKey[Command](serviceKey)).toList
      if (pluviometersList == pluviometers)
        Behaviors.same
      else
        manageAlarmBehaviour(ctx, serviceKey, fireStation, pluviometersList)
    case NotifyState(state) => Behaviors.same
    case _ => Behaviors.same
  }
