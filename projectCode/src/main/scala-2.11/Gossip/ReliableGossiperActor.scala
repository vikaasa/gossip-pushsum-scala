package Gossip

import java.util.concurrent.TimeUnit

import akka.actor.{Cancellable, ActorRef}

import scala.concurrent.duration.{FiniteDuration, Duration}

//Gossiper Actor used to simulate disconnect of nodes by sending a disconnect message
//When a message is received this actor sends the acknowledgement to the sender to indicate that message was successfully received
//If the node is disconnect the ACK is not sent and the nodes does not send message on tick
class ReliableGossiperActor(override val parent: GossiperNode, terminationMonitor: ActorRef)
  extends GossiperActor(parent, terminationMonitor){

  //private val log = Logging(context.system, this)
  protected var reconnectToken:Cancellable = null
  override protected val sendHBDuration = 10
  protected val resetSendDuration = Duration.create(5,TimeUnit.MILLISECONDS)

  override def receive = {
    case StartGossip(msg: String) => onStartGossip(msg)
    case SendMessageHeartBeat => onSendMessageHeartBeat()
    case GossipRumour(rumourMsg) =>
      if(!isDisconnected){
        sender!ACKGossip
        onGossipRumour(rumourMsg)
      }
    case ACKGossip => isSendReady = true
    case ACKPushSum(s, w) =>
      parent.s -= s
      parent.w -= w
      isSendReady = true
    case StartPushSum => onStartPushSum()
    case PushSumMessage(s, w)=>
      if(!isDisconnected){
        sender!ACKPushSum(s, w)
        onPushSumMessage(s, w)
      }
    case Disconnect(duration) => onDisconnect(duration)
    case ACKWaitTimeout =>
      //Note that in this Failure model we are not remembering failed nodes
      if(!isSendReady){
        isSendReady = true
      }

  }

  protected override def startHBTimer(): Unit = {
    if(!isDisconnected)
      sendHBCancelToken = context.system.scheduler.schedule(Duration.create(sendHBDuration, TimeUnit.MILLISECONDS),
        Duration.create(sendHBDuration, TimeUnit.MILLISECONDS),
        self, SendMessageHeartBeat) (context.system.dispatcher, self)
  }

  protected def onDisconnect(duration: FiniteDuration):Unit = {
    isDisconnected = true
    if(sendHBCancelToken != null)
      sendHBCancelToken.cancel()

    //reconnect after finiteDuration to simulate failure model
    reconnectToken = context.system.scheduler.scheduleOnce(duration, new Runnable(){
      override def run(): Unit = {
        isDisconnected = false
        //Re-Initialize send message heart beat, if the algorithm is initialized
        if(!algorithmType.isEmpty)
          sendHBCancelToken = context.system.scheduler.schedule(Duration.create(sendHBDuration, TimeUnit.MILLISECONDS),
            Duration.create(sendHBDuration, TimeUnit.MILLISECONDS),
            parent.ActorRef, SendMessageHeartBeat) (context.system.dispatcher, parent.ActorRef)
      }
    }) (context.system.dispatcher)
  }

  override def sendMessage(neighbour: GossiperNode):Unit={
    //Set the flag indicating that the actor should wait until the ACK for the last message is received
    isSendReady = false
    algorithmType match {
      case this.gossipAlgo =>
        neighbour.ActorRef ! GossipRumour(rumourMsg)
      case this.pushSumAlgo =>
        //Send a message to any random neighbour but update sum and weight only when
        //the ACK for the message is received
        neighbour.ActorRef ! PushSumMessage(this.parent.s/2, this.parent.w/2)
    }
    context.system.scheduler.scheduleOnce(resetSendDuration,this.parent.ActorRef,ACKWaitTimeout)(context.dispatcher)
  }
}


case class ACKPushSum(s:Double, w:Double)
case class ACKGossip()
case class ACKWaitTimeout()
