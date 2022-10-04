package it.unibo.pcd.firestation

import javax.swing.JFrame

class FireStationGUI(val width: Int, val height: Int):
  self => // self-types, used to take the val reference inside the inner class
  private val frame = JFrame()
  frame.setSize(width, height)
  frame.setVisible(true)
  frame.setLocationRelativeTo(null)
