package it.unibo.pcd.firestation

import javax.swing.JFrame
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.*
import it.unibo.pcd.Launcher.Zone

import javax.swing.{BoxLayout, JButton, JFrame, JLabel, JPanel, SwingUtilities}
import java.awt.{BorderLayout, Canvas, Color, Dimension, Graphics, GridLayout}
import it.unibo.pcd.utils.Protocol.{Command, FireStationAction, FireStationActionOver}

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.geom.Rectangle2D

class FireStationGUI(val width: Int, val height: Int, cityZones: List[Zone], frontendActor: ActorRef[Command]):
  self =>
  var zoneButtons: List[JButton] = List.empty
  private val frame = JFrame()
  frame.setSize(width, height)
  frame.setTitle("Firestation Frontend")
  val layout = GridLayout(2, 2, 1, 1)
  frame.setLayout(layout)
  frame.setLocationRelativeTo(null)
  frame.setVisible(true)
  cityZones.foreach { zone =>
    val button: JButton = JButton("Zone " + cityZones.indexOf(zone) + " - " + "Pluoviometers = ?")
    button.setBackground(Color.GREEN)
    zoneButtons = zoneButtons ++ List(button)
  }
  zoneButtons.foreach(frame.add(_))

  def updateZoneStatus(zone: Int, state: Boolean): Unit =
    state match
      case true =>
        self.zoneButtons(zone).setBackground(Color.GREEN)
        self.zoneButtons(zone).getActionListeners.foreach(self.zoneButtons(zone).removeActionListener(_))
      case false =>
        self.zoneButtons(zone).setBackground(Color.RED)
        self.zoneButtons(zone).addActionListener(_ => fireStationAction(zone))

  def updatePluviometers(pluviometersPerZone: (Int, Int)): Unit =
    zoneButtons(pluviometersPerZone._1).setText(
      "Zone " + pluviometersPerZone._1 + " - " + "Pluoviometers = " + pluviometersPerZone._2
    )

  def fireStationAction(zone: Int): Unit =
    self.zoneButtons(zone).setBackground(Color.ORANGE)
    self.frontendActor ! FireStationAction(zone)
    self.zoneButtons(zone).getActionListeners.foreach(self.zoneButtons(zone).removeActionListener(_))
    self.zoneButtons(zone).addActionListener(_ => fireStationActionOver(zone))

  def fireStationActionOver(zone: Int): Unit =
    self.zoneButtons(zone).getActionListeners.foreach(self.zoneButtons(zone).removeActionListener(_))
    self.zoneButtons(zone).setBackground(Color.GREEN)
    self.frontendActor ! FireStationActionOver(zone)
