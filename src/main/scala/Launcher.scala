import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.*
import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import concurrent.duration.DurationInt

import scala.util.Random
import it.unibo.pcd.pluviometer.Pluviometer
import it.unibo.pcd.firestation.FireStation
import it.unibo.pcd.utils.startupWithRole

import scala.language.postfixOps

object Launcher:

  case class Zone(rangeX: (Int, Int), rangeY: (Int, Int))

  val citySize: (Int, Int) = (10, 10)
  val cityZones: List[Zone] =
    List(Zone((0, 5), (0, 5)), Zone((5, 10), (0, 5)), Zone((0, 5), (5, 10)), Zone((5, 10), (5, 10)))
  val pluviometersPerZone: Int = 3
  var currentAvailablePort: Int = 2551

  @main def launchAll(): Unit =
    for
      zone <- cityZones
      _ = createRoleNode("fireStation")(FireStation())
      _ <- 0 until pluviometersPerZone
      pluviometerPosition = (
        Random.between(zone.rangeX._1, zone.rangeX._2),
        Random.between(zone.rangeY._1, zone.rangeY._2)
      )
      _ = createRoleNode("pluviometer")(
        Pluviometer(pluviometerPosition, 60 milliseconds, "zone" + cityZones.indexOf(zone))
      )
    yield ()
    createRoleNode("fireStationFrontend")(FireStation())

  def createRoleNode[X](role: String)(root: => Behavior[X]): ActorSystem[X] =
    currentAvailablePort = currentAvailablePort + 1
    println("port" + currentAvailablePort + root.behavior)
    startupWithRole(role, currentAvailablePort)(root)
