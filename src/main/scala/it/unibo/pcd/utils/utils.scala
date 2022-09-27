package it.unibo.pcd.utils
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory

def startup[X](file: String = "application", port: Int)(root: => Behavior[X]): ActorSystem[X] =
  // Override the configuration of the port
  val config = ConfigFactory
    .parseString(s"""akka.remote.artery.canonical.port=$port""")
    .withFallback(ConfigFactory.load(file))
  // Create an Akka system
  ActorSystem(root, "ClusterSystem", config)

def startupWithRole[X](role: String, port: Int)(root: => Behavior[X]): ActorSystem[X] =
  val config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
    .withFallback(ConfigFactory.load("application"))
  // Create an Akka system
  ActorSystem(root, "ClusterSystem", config)
