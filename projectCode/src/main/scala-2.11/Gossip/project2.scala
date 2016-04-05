package Gossip

import java.util.concurrent.TimeUnit
import akka.actor._

import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.util.Random

object project2 extends App{

  val NonReliableNetwork = false
  val ReliableNetwork = true

  if(args.length < 3) {
    println("Invalid Arguments")
    showUsage()
  }
  else{
    val actorSystem = ActorSystem("GossipSystem")
    val (numNodes, topologyBuilder, algo, failureStats) = parseCLArgs(args)
    val terminationMonitor = actorSystem.actorOf(Props(new TerminationMonitor(numNodes)), name="TerminationMonitor")

    var startTime = System.currentTimeMillis()
    val gossipers = topologyBuilder.buildTopology(numNodes)

    //println("Build Complete: "+ (System.currentTimeMillis() - startTime)/1000.0)

    startTime = System.currentTimeMillis()
    if(failureStats == NoFailureNetwork){
      gossipers.foreach((gossiper)=>{
        gossiper.init(actorSystem,terminationMonitor, gossipers, NonReliableNetwork)
      })
    } else {
      gossipers.foreach((gossiper)=>{
        gossiper.init(actorSystem,terminationMonitor, gossipers, ReliableNetwork)
      })
      //Create a disconnect manager that disconnects random gossipers
      val disconnectManager = actorSystem.actorOf(Props(new DisconnectSimulator()), name="DisconnectSimulator")
      actorSystem.scheduler.scheduleOnce(Duration.create(10, TimeUnit.MILLISECONDS),
        disconnectManager, StartDisconnect(failureStats, gossipers)) (actorSystem.dispatcher)
    }

    //println("Initialization Complete"+ (System.currentTimeMillis() - startTime)/1000.0)

    //starts the timer and manages the time it takes to run the algorithm
    terminationMonitor ! TerminationMonitorInit(gossipers, failureStats)

    algo.toLowerCase  match {
      case "gossip" =>
        gossipers(Random.nextInt(gossipers.length)).ActorRef ! StartGossip("Some Gossip Msg")
      case "push-sum" =>
        //gossipers(0).ActorRef ! StartPushSum
        gossipers(Random.nextInt(gossipers.length)).ActorRef ! StartPushSum
    }



  }

  def parseCLArgs(args : Array[String]) : (Int, TopologyBuilder, String, FailureStats) = {
    var numNodes = 0
    try{
      numNodes = args(0).toInt
    }catch{
      case e: NumberFormatException =>
        println("Error: numNodes must be an Integer")
        showUsage()
        System.exit(1)
    }

    val topology = args(1)
    var topologyBuilder: TopologyBuilder = null
    topology.toLowerCase match {
      case "full" =>
        topologyBuilder = new FullTopologyBuilder
      case "line" =>
        topologyBuilder = new LineTopologyBuilder
      case "3d" =>
        topologyBuilder = new ThreeDTopologyBuilder
      case "imp3d" =>
        topologyBuilder = new Imperfect3DTopologyBuilder
      case _ =>
        showUsage()
        System.exit(1)
    }

    val algoStr = args(2)
    algoStr.toLowerCase  match {
      case "gossip" =>
      case "push-sum" =>
      case _ =>
        showUsage()
        System.exit(1)
    }
    var failureStats:FailureStats = NoFailureNetwork

    if(args.length == 5){
      failureStats = new FailureStats(args(3).toDouble,  Duration.create(args(4).toInt,TimeUnit.MILLISECONDS))
    }

    (numNodes, topologyBuilder, algoStr, failureStats)
  }
  protected def showUsage() :Unit ={
    println("\tUsage:")
    println("\t\tsbt \"run numNodes { full | 3D | line | imp3D } { gossip | push-sum }\"")
    println("\t\t\t\t\tOR ")
    println("\t\tsbt \"run numNodes { full | 3D | line | imp3D } { gossip | push-sum } tmpNodeFailureRatio failureDurationInMillis\"")
  }

}

case class FailureStats(nodeFailureRatio:Double, durationOfFailure:FiniteDuration)
object NoFailureNetwork extends FailureStats(0.0, Duration.create(0,TimeUnit.MILLISECONDS))
