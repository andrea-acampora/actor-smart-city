package it.unibo.pcd.pluviometer

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.*
import it.unibo.pcd.utils.Message

import scala.concurrent.duration.FiniteDuration

object Pluviometer:

  sealed trait Command extends Message
  final case class Discover(actorRef: ActorRef[_]) extends Message with Command
  private case object CheckWaterLevel extends Command

  def apply(
      position: (Int, Int),
      period: FiniteDuration,
      serviceKey: String,
      neighbours: List[ActorRef[_]] = List.empty
  ): Behavior[Command | Receptionist.Listing] =
    Behaviors.setup[Command | Receptionist.Listing] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(ServiceKey[Discover](serviceKey), ctx.self)
      ctx.system.receptionist ! Receptionist.Subscribe(ServiceKey[Discover](serviceKey), ctx.self)
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(CheckWaterLevel, period)
        pluviometerLogic(position, ctx, serviceKey, neighbours)
      }
    }

  def pluviometerLogic(
      position: (Int, Int),
      ctx: ActorContext[Command | Receptionist.Listing],
      key: String,
      neighbours: List[ActorRef[_]]
  ): Behavior[Command | Receptionist.Listing] = Behaviors.receiveMessage {
    case msg: Receptionist.Listing => Behaviors.same
    case CheckWaterLevel => Behaviors.same
  }
