package Gossip

import scala.collection.mutable.ListBuffer
import scala.util.Random

trait TopologyBuilder{
  def buildTopology(numNodes:Int): IndexedSeq[GossiperNode]
  def createGossipers(numNodes:Int) : IndexedSeq[GossiperNode] = {
    val gossipersBuffer = new ListBuffer[GossiperNode]()
    for(i <- 0 to numNodes -1){
      gossipersBuffer += new GossiperNode(i)
    }
    gossipersBuffer.toIndexedSeq
  }

}

class FullTopologyBuilder extends TopologyBuilder{
  def buildTopology(numNodes:Int):IndexedSeq[GossiperNode] = {
    createGossipers(numNodes) //Neighbours managed by sub-class of Gossiper
  }

  override def createGossipers(numNodes:Int) : IndexedSeq[GossiperNode] = {
    val gossipersBuffer = new ListBuffer[GossiperNode]()
    for(i <- 0 to numNodes -1){
      gossipersBuffer += new FullTopologyGossiperNode(i)
    }
    gossipersBuffer.toIndexedSeq
  }
}

class ThreeDTopologyBuilder extends TopologyBuilder{
  def buildTopology(numNodes:Int):IndexedSeq[GossiperNode] = {
    val gossipers = createGossipers(numNodes)
    val cbrtOfNumNodes = Math.cbrt(numNodes)
    var zMax = cbrtOfNumNodes.toInt // truncates the cuberoot values nearest integer i.e 2.99 gets truncated to 2
    var xMax = zMax
    var yMax = zMax
    if(!cbrtOfNumNodes.isValidInt){
      //Measure if the cbrt(numNode) is near to the next integer or truncated integer
      if((numNodes - Math.pow(zMax,3)) < (Math.pow(zMax+1,3) - numNodes)){
        //lower truncated cbrt is nearer
        val roundedCube = Math.pow(zMax,3)
        val offset = numNodes - roundedCube
        zMax = zMax + (offset / Math.pow(xMax,2)).toInt + 1
      } else{
        //next cube is nearer
        xMax += 1; yMax += 1; zMax += 1
        val roundedCube = Math.pow(xMax,3)
        val offset = roundedCube - numNodes
        zMax = zMax - ((offset / Math.pow(xMax,2)).toInt - 1)
      }
    }

    //subracted 1 to fix indexing from 0
    var i=0
    val zMulti =  Math.pow(xMax,2).toInt
    val yMulti = xMax
    val xLimit = xMax - 1
    val yLimit = yMax - 1
    val zLimit = zMax - 1
    for(z <- 0 to zLimit){
      for(y <-0  to yLimit){
        for(x<-0 to xLimit) {
          i = z * zMulti + y * yMulti + x
          if (i < numNodes) {
            val gossiper = gossipers(i)
            val neighbours = new ListBuffer[Int]
            if (x > 0) neighbours += (i - 1)
            if (x < xLimit && (i + 1) < numNodes) neighbours += (i + 1)
            if (y > 0) neighbours += (i - yMulti)
            if (y < yLimit && (i + yMulti) < numNodes) neighbours += (i + yMulti)
            if (z > 0) neighbours += (i - zMulti)
            if (z < zLimit && (i + zMulti) < numNodes) neighbours += (i + zMulti)
            setNeighbourIndices(gossiper, neighbours.toIndexedSeq, gossipers)
          }
        }
      }
    }
    gossipers
  }

  protected def setNeighbourIndices(gossiper: GossiperNode, neighbourIndices: IndexedSeq[Int], allGossipers: IndexedSeq[GossiperNode]):Unit = {
    gossiper.setNeighbourIndices(neighbourIndices)
  }
}

class LineTopologyBuilder extends TopologyBuilder {
  def buildTopology(numNodes:Int):IndexedSeq[GossiperNode] = {
    val gossipers = createGossipers(numNodes)
    for (i <- 0 to numNodes - 1) {
      val gossiper = gossipers(i)
      val neighbours = new ListBuffer[Int]
      if (i != 0) {
        neighbours += (i-1)
      }
      if(i != numNodes -1){
        neighbours += (i+1)
      }
      gossiper.setNeighbourIndices(neighbours.toIndexedSeq)
    }
    gossipers
  }

}

class Imperfect3DTopologyBuilder extends ThreeDTopologyBuilder{

  //When set neighbour indices on super class is called we just add another random neighbour
  override def setNeighbourIndices(gossiper: GossiperNode, neighbourIndices: IndexedSeq[Int], allGossipers: IndexedSeq[GossiperNode]):Unit = {
    var j = 0
    var notFound  = true
    val numNodes = allGossipers.length
    //Here, j is used just as a precaution so that the algo does not try indefinately to find a random neighbour who is not
    //a 3D topology neighbour
    while(j < numNodes && notFound){
      val randomGossiperIndex = Random.nextInt(numNodes)
      if(!neighbourIndices.contains(randomGossiperIndex) && randomGossiperIndex != gossiper.index){
        notFound = false
        //Add the random neighbour
        gossiper.setNeighbourIndices(neighbourIndices :+ randomGossiperIndex)
      }
      j+=1
    }

  }
}

