package it.unibo.pcd.firestation

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.pcd.utils.Protocol.{AlarmOver, Command, InterventionRequest, PluviometersChange}

object FireStationFrontend:

  val width = 800
  val height = 600

  def apply(): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    val gui = FireStationGUI(width, height)
    frontendLogic(ctx)
  }

  def frontendLogic(ctx: ActorContext[Command]): Behavior[Command] = ???
