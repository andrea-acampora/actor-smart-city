package it.unibo.pcd

import akka.actor.typed.{ActorSystem, Behavior}
import it.unibo.pcd.firestation.{FireStation, FireStationFrontend}
import it.unibo.pcd.pluviometer.{Pluviometer, ZoneManager}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random
import it.unibo.pcd.utils.startupWithRole

object Launcher:

  case class Zone(rangeX: (Int, Int), rangeY: (Int, Int))

  val citySize: (Int, Int) = (10, 10)
  val cityZones: List[Zone] =
    List(Zone((0, 5), (0, 5)), Zone((5, 10), (0, 5)), Zone((0, 5), (5, 10)), Zone((5, 10), (5, 10)))
  val pluviometersPerZone: Int = 3
  var currentAvailablePort: Int = 2551

  @main def launchAll(): Unit =
    createRoleNode("fireStationFrontend")(FireStationFrontend(cityZones))
    for
      zone <- cityZones
      fireStationRef = createRoleNode("fireStation")(FireStation(cityZones.indexOf(zone)))
      zoneManagerRef = createRoleNode("zoneManager")(ZoneManager("zone" + cityZones.indexOf(zone), fireStationRef))
      _ <- 0 until pluviometersPerZone
      pluviometerPosition = (
        Random.between(zone.rangeX._1, zone.rangeX._2),
        Random.between(zone.rangeY._1, zone.rangeY._2)
      )
      _ = println("creating pluviometer: " + pluviometerPosition + " in zone : " + cityZones.indexOf(zone))
      _ = createRoleNode("pluviometer")(
        Pluviometer(zoneManagerRef, pluviometerPosition, 60 milliseconds, "zone" + cityZones.indexOf(zone))
      )
    yield ()

  def createRoleNode[X](role: String)(root: => Behavior[X]): ActorSystem[X] =
    currentAvailablePort = currentAvailablePort + 1
    startupWithRole(role, currentAvailablePort)(root)
