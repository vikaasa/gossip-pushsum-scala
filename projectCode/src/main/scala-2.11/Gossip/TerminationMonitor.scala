package Gossip

import akka.actor.Actor

import scala.util.Random

class TerminationMonitor(numNodes: Int) extends Actor {
  private var terminationCount = 0
  private var gossipers: IndexedSeq[GossiperNode] = null
  private var startTime = 0L
  private var timeTaken = 0L
  private var failureStats : FailureStats = null;

  override def receive = {
    case ValueConverged =>
      terminationCount += 1
      if (terminationCount == numNodes){
        timeTaken = System.currentTimeMillis() - startTime
        val randomGossiper = gossipers(Random.nextInt(gossipers.length))
        if(failureStats == NoFailureNetwork)
          println(timeTaken/1000.0 + ","+ numNodes + "," + randomGossiper.s/randomGossiper.w)
        else println(timeTaken/1000.0 + ","+ numNodes + "," + randomGossiper.s/randomGossiper.w + "," + failureStats.nodeFailureRatio)
        System.exit(0)
      }

    case HeardRumour =>
      terminationCount += 1
      timeTaken = System.currentTimeMillis() - startTime
      //if 90% of the nodes heard the rumor
      if (terminationCount/numNodes.toDouble >= 0.9){
        if(failureStats == NoFailureNetwork)
          println(timeTaken/1000.0 + ","+numNodes + "," + terminationCount/numNodes.toDouble)
        else println(timeTaken/1000.0 + ","+numNodes + "," + terminationCount/numNodes.toDouble + "," + failureStats.nodeFailureRatio)
        System.exit(0)
      }

    case TerminationMonitorInit(gsprs, failureStats) =>
      gossipers = gsprs
      this.failureStats = failureStats
      startTime = System.currentTimeMillis()
  }
}

case class ValueConverged()
case class HeardRumour()
case class TerminationMonitorInit(gossipers: IndexedSeq[GossiperNode], failureStats: FailureStats)