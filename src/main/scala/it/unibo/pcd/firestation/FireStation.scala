package it.unibo.pcd.firestation

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.pluviometer.Pluviometer.Command

object FireStation:
  def apply(): Behavior[Command] = Behaviors.empty
