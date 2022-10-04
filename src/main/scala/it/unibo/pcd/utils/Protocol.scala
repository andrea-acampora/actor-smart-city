package it.unibo.pcd.utils

import akka.actor.typed.ActorRef

object Protocol:
  sealed trait Command extends Message
  final case class CheckWaterLevel() extends Command
  final case class NotifyAlarm() extends Command
  final case class NotifyState(inAlarm: Boolean) extends Command
  final case class CheckAlarm() extends Command
  final case class AlarmConfirmed() extends Command
  final case class InterventionRequest(zoneManager: ActorRef[Command]) extends Command
  final case class AlarmOver() extends Command
  final case class PluviometersChange(pluviometers: List[ActorRef[Command]]) extends Command
