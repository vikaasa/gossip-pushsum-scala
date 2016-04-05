package Gossip

import akka.actor.{Props, ActorSystem, ActorRef}

import scala.util.Random

class GossiperNode(val index: Int){
  private var neighbourIndices: IndexedSeq[Int] = null
  var s:Double = index + 1.0
  var w:Double = 1.0
  var rumourCount: Int = 0
  private var actorRef: ActorRef = null
  protected var allGossipers: IndexedSeq[GossiperNode] = null

  def ActorRef = actorRef
  def setNeighbourIndices(neighbourIndices: IndexedSeq[Int]):Unit = this.neighbourIndices = neighbourIndices
  def getRandomNeighbour : GossiperNode = allGossipers(neighbourIndices(Random.nextInt(neighbourIndices.length)))
  def init(actorSystem: ActorSystem, terminationMonitor: ActorRef, allGossipers: IndexedSeq[GossiperNode], reliableMessaging: Boolean) ={
    this.allGossipers = allGossipers
    if(reliableMessaging)
      actorRef = actorSystem.actorOf(Props(new ReliableGossiperActor(this, terminationMonitor)), name="ReliableGossiperActor:"+index)
    else actorRef = actorSystem.actorOf(Props(new GossiperActor(this, terminationMonitor)), name="GossiperActor:"+index)
  }

}

class FullTopologyGossiperNode(override val index: Int) extends GossiperNode(index){

  override def setNeighbourIndices(neighbourIndices: IndexedSeq[Int]):Unit = {} //No-Op as not neccesary
  override def getRandomNeighbour : GossiperNode = {
    var randomIndex = Random.nextInt(allGossipers.length)
    if(allGossipers.length == 1)
      throw new RuntimeException("Random neighbour cannot be called when there is only one gossiper")
    while(randomIndex == index){
      randomIndex = Random.nextInt(allGossipers.length)
    }
    allGossipers(randomIndex)
  }
}
