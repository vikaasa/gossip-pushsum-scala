package Gossip

import java.util.concurrent.TimeUnit

import akka.actor.{Cancellable, Actor}

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.util.Random

class DisconnectSimulator() extends Actor{

  val randomNodeIndices = new mutable.HashSet[Int]()
  var disconnectCancelToken : Cancellable= null
  override def receive = {

    case StartDisconnect(failureStats, allGossipers) =>
      val numNodes = allGossipers.length
      val numDcNodes = (failureStats.nodeFailureRatio * numNodes).toInt
      var count = 0
      while(count < numDcNodes){
        val randomIdx = Random.nextInt(numNodes)
        if(!randomNodeIndices.contains(randomIdx)){
          randomNodeIndices += randomIdx
          count += 1
        }
      }

      //Start disconnecting random nodes every 10 milliseconds
      disconnectCancelToken = context.system.scheduler.schedule(Duration.create(10,TimeUnit.MILLISECONDS), Duration.create(10,TimeUnit.MILLISECONDS), new Runnable(){
        override def run(): Unit = {
          if(!randomNodeIndices.isEmpty){
            val randomIndex = randomNodeIndices.head
            randomNodeIndices.remove(randomIndex)
            //println(randomIndex)
            allGossipers(randomIndex).ActorRef ! Disconnect(failureStats.durationOfFailure)
          }
          else {
            disconnectCancelToken.cancel();
          }
        }
      }) (context.system.dispatcher);
  }
}


case class StartDisconnect(failureStats: FailureStats, allGossipers:IndexedSeq[GossiperNode])
case class Disconnect(duration: FiniteDuration)
case class DisconnectHeartBeat()

