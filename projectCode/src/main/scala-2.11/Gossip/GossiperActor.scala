package Gossip

import java.util.concurrent.TimeUnit

import akka.actor.{Cancellable, Actor, ActorRef}
import akka.event.Logging

import scala.concurrent.duration.Duration

class GossiperActor(val parent: GossiperNode, terminationMonitor: ActorRef) extends Actor{
  private val log = Logging(context.system, this)
  protected val gossipAlgo = "gossip"
  protected val pushSumAlgo = "push-sum"

  protected val gossipTerminationLimit = 10
  protected var algorithmType = ""
  protected var rumourMsg = ""
  protected var convergenceCount = 0

  protected val sendHBDuration = 10
  protected val valueConvergenceLimit = 3
  protected var sendHBCancelToken : Cancellable = null

  protected var isTerminated: Boolean = false

  protected var isDisconnected: Boolean = false
  protected var isSendReady: Boolean = true

  protected def initPushSumAlgo():Unit = {
    algorithmType = pushSumAlgo
    startHBTimer()
  }

  protected def initGossipAglo(rumour : String):Unit = {
    rumourMsg = rumour
    algorithmType = gossipAlgo
    startHBTimer()
    terminationMonitor ! HeardRumour
  }

  protected def startHBTimer(): Unit = {
    sendHBCancelToken = context.system.scheduler.schedule(Duration.create(sendHBDuration, TimeUnit.MILLISECONDS),
      Duration.create(sendHBDuration, TimeUnit.MILLISECONDS),
      self, SendMessageHeartBeat) (context.system.dispatcher, self)
  }

  protected def onStartGossip(msg: String): Unit = {
    log.debug("Started gossip:" + this.parent.index)
    this.parent.rumourCount += 1
    initGossipAglo(msg)
  }

  protected def onSendMessageHeartBeat():Unit = {
    if(!isDisconnected && isSendReady && !algorithmType.isEmpty){
      val randomNeighbour = this.parent.getRandomNeighbour
      if(randomNeighbour != null)
        sendMessage(randomNeighbour)
    }
  }

  protected def onStartPushSum() = {
    this.parent.w = 1.0
    log.debug("Started push-sum:" + this.parent.index)
    initPushSumAlgo()
  }

  protected def onPushSumMessage(s:Double, w:Double) = {
    val newSi = s + this.parent.s
    val newWi = w + this.parent.w

    //To avoid divide by zero error
    if(this.parent.w != 0.0) {
      val change = newSi/newWi - parent.s/parent.w
      if (change.abs < 1E-10) {
        convergenceCount += 1
      } else convergenceCount = 0

      if(convergenceCount == valueConvergenceLimit && !isTerminated){
        terminationMonitor ! ValueConverged
        isTerminated = true
        //We don't stop message transmission when a node converges in push-sum algorithm.
        //If we do, other nodes may not achieve convergence
      }
    }

    this.parent.s = newSi
    this.parent.w = newWi

    if(algorithmType.isEmpty)
      initPushSumAlgo()
  }
  

  protected def onGossipRumour(msg: String) ={
    if(algorithmType.isEmpty)
      initGossipAglo(msg)
    this.parent.rumourCount += 1
    if(this.parent.rumourCount == gossipTerminationLimit){
      isTerminated = true
      sendHBCancelToken.cancel()
    }
  }


  override def receive = {
    case StartGossip(msg: String) => onStartGossip(msg)
    case SendMessageHeartBeat => onSendMessageHeartBeat()
    case GossipRumour(rumourMsg) => onGossipRumour(rumourMsg)
    case StartPushSum => onStartPushSum()
    case PushSumMessage(sum, weight) => onPushSumMessage(sum, weight)
  }

  def sendMessage(neighbour: GossiperNode):Unit={
    algorithmType match{
      case this.gossipAlgo =>
        neighbour.ActorRef ! GossipRumour(rumourMsg)
      case this.pushSumAlgo =>
        this.parent.s /= 2
        this.parent.w /= 2
        neighbour.ActorRef ! PushSumMessage(this.parent.s, this.parent.w)
    }
  }

}

case class StartPushSum()
case class StartGossip(msg: String)
case class SendMessageHeartBeat()
case class GossipRumour(msg: String)
case class PushSumMessage(s: Double, w: Double)