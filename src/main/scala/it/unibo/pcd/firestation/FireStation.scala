package it.unibo.pcd.firestation

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.pcd.utils.Protocol.{AlarmOver, Command, InterventionRequest, PluviometersChange}

object FireStation:

  def apply(): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    fireStationLogic(ctx, false, false, List.empty)
  }

  def fireStationLogic(
      ctx: ActorContext[Command],
      zoneInAlarm: Boolean,
      fireStationState: Boolean,
      pluviometers: List[ActorRef[Command]]
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case InterventionRequest(zoneManager: ActorRef[Command]) =>
        Thread.sleep(1000)
        zoneManager ! AlarmOver()
        Behaviors.same
      case PluviometersChange(pluviometerList) => fireStationLogic(ctx, zoneInAlarm, fireStationState, pluviometerList)
      case _ => Behaviors.same
    }
