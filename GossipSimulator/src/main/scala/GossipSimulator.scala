

import akka.actor.Cancellable
import akka.actor._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.math._
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeUnit
/**
 * @author Prateek
 *
 *
 */
case object Failure
case object sendNeighbourList
case object Rumour
case object SendRumour
case class setNode(nodes: ArrayBuffer[ActorRef])
case class addState(s: Double, w: Double)
case object StartRumour
case object TerminateNode
case object NodeTerminated
case object EndSimulation
case object GossipInfo
case object GossipAggre
case class printNeighbours(actorName: String, neighbourArray: ArrayBuffer[ActorRef])
case object SetState
case class setNeighbours(neighbourArray: ArrayBuffer[ActorRef])

object GossipSimulator extends App {

  override def main(args: Array[String]) {

    println("enter the parameters")
    if (args.length != 3) {
      println("invalid parameters")
      System.exit(1)
    } else {
      val system = ActorSystem("Gossip_Simulator")

      var Env = system.actorOf(Props(new Master(args(0).trim.toInt, args(1).trim, args(2).trim, system)), name = "Gossip_Environment")
      //println("adadssdad")
      Env ! "Start"
      //System.exit(1)

    }

  }

}

class Master(n: Int, topology: String, algorithm: String, sys: ActorSystem) extends Actor {
  var convergence_time = 0
  var nodes = new ArrayBuffer[ActorRef]()
  var randomNodeList = new ArrayBuffer[ActorRef]()
  var no_of_active = 0
  var init_time = 0.0
  var final_time = 0.0
  var temp = new ArrayBuffer[ActorRef]()
  var randList = new ArrayBuffer[Int]()
  /* for(i<-0 until n)
    nodes +=context.actorOf(Props(new gossipNode),name="node"+i)*/
  println(n + " " + topology + " " + algorithm)
  def setRandomNode(i: Int, l: Int, nodes: ArrayBuffer[ActorRef], randList: ArrayBuffer[Int]) = {
    temp = new ArrayBuffer[ActorRef]()
    var randomNode = 0
    randomNodeList = new ArrayBuffer[ActorRef]()
    // println(randList)
    if (randList(i) == 99999) {
      if (i + l * l <= (l * l * l - 1)) {
        temp += nodes(i + l * l)
      }
      if (i - l * l >= 0) {
        temp += nodes(i - l * l)
      }
      if (i + l <= l * l * l - 1) { temp += nodes(i + l) }
      if (i - l >= 0) { temp += nodes(i - l) }
      if (i + 1 <= l * l * l - 1) { temp += nodes(i + 1) }
      if (i - 1 >= 0) { temp += nodes(i - 1) }
      randomNodeList = (nodes -- temp) - nodes(i)
      //  println(randomNodeList)
      temp = new ArrayBuffer[ActorRef]()
      randomNode = Random.nextInt(randomNodeList.length)
      nodes(i) ! setNeighbours(temp += randomNodeList(randomNode))
      temp = new ArrayBuffer[ActorRef]()
      randomNodeList(randomNode) ! setNeighbours(temp += nodes(i))
      temp = new ArrayBuffer[ActorRef]()
      randList(randomNodeList(randomNode).path.name.substring(4).trim.toInt) = i
      randomNodeList = new ArrayBuffer[ActorRef]()
      // println("update for i->"+i+"->"+randList)

    } else {
      nodes(i) ! setNeighbours(temp += nodes(randList(i)))
      temp = new ArrayBuffer[ActorRef]()
      nodes(randList(i)) ! setNeighbours(temp += nodes(i))
    }
  }
  def receive = {

    case "Start" =>
      {
        no_of_active = n

        algorithm match {

          case "gossip" => {

            if (topology == "Line") {

              for (i <- 0 to n - 1)
                nodes += context.actorOf(Props(new gossipNode), name = "node" + i)

              //setting up the first node
              nodes(0) ! setNeighbours(temp += nodes(1))

              temp = new ArrayBuffer[ActorRef]()

              //end of if Line  
              //setting up the next nodes until the last one 
              for (i <- 1 to n - 2) {
                nodes(i) ! setNeighbours(temp += (nodes(i - 1), nodes(i + 1)))
                temp = new ArrayBuffer[ActorRef]()
              }
              //setting up the last node
              nodes(n - 1) ! setNeighbours(temp += (nodes(n - 2)))
              temp = new ArrayBuffer[ActorRef]()
              //topology complete
              var randomNode = Random.nextInt(n)

              println("selecting the first node randomly to start gossip")
              nodes(randomNode) ! Rumour

              init_time = System.currentTimeMillis()

            }

            if (topology == "Full") {
              for (i <- 0 to n - 1)
                nodes += context.actorOf(Props(new gossipNode), name = "node" + i)

              for (i <- 0 to n - 1) {
                {
                  nodes(i) ! setNeighbours(nodes - nodes(i))
                  temp = new ArrayBuffer[ActorRef]()

                }
              }

              var randomNode = Random.nextInt(n)

              println("selecting the first node randomly to start gossip")
              nodes(randomNode) ! Rumour

              //   sys.scheduler.schedule(0 seconds, 5 minutes, actor, Message())

              init_time = System.currentTimeMillis()
              // end of full
            }

            if (topology == "3d") {

              var p: Double = 0.33
              var l = ceil(pow(n, (p))).toInt
              println((l))
              for (i <- 0 to (l * l * l) - 1)
                nodes += context.actorOf(Props(new gossipNode), name = "node" + i)

              //for starting nodes 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * l * l)) {
                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l), nodes(i + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }
              //for the nodes between starting and end nodes
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- 1 + i to l + i - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l), nodes(j + (l * l))))
                    temp = new ArrayBuffer[ActorRef]()
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l)))
                    temp = new ArrayBuffer[ActorRef]()
                  }

                }
              }

              //for ending nodes

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l), nodes(i + l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              // for all nodes other than the first and last rows on a plane 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                // inter planar 

                for (j <- i + l to i + (l * l) - l - 1) {
                  if (j % l == 0) {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1), nodes(j + (l * l))))
                      temp = new ArrayBuffer[ActorRef]()

                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()
                    }

                  } else if (j % l == l - 1) {

                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1)))
                      temp = new ArrayBuffer[ActorRef]()
                    }
                  } else {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                    } else {

                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()

                    }
                  }

                }

              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1), nodes(i + l * l - l + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- i + l * l - l + 1 to i + l * l - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l), nodes(j + l * l)))
                    temp = new ArrayBuffer[ActorRef]()
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l)))
                    temp = new ArrayBuffer[ActorRef]()
                  }

                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l), nodes(i + l * l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              for (i <- (l - 1) * (l * l) to l * l by -1 * (l * l)) {
                for (j <- i + 0 to i + l * l - 1) {
                  nodes(j) ! setNeighbours(temp += (nodes(j - l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              var randomNode = Random.nextInt(l * l * l)

              println("selecting the first node randomly to start gossip")
              nodes(randomNode) ! Rumour
              init_time = System.currentTimeMillis()
              //End of 3D topology set up
            }

            //End of gossip

            if (topology == "imp3d") {

              var p: Double = 0.33
              var l = ceil(pow(n, (p))).toInt
              println((l))
              for (i <- 0 to (l * l * l) - 1)
                nodes += context.actorOf(Props(new gossipNode), name = "node" + i)

              for (i <- 0 to (l * l * l) - 1)
                randList += 99999
              //  println("randlist"+randList.length)
              //for starting nodes 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * l * l)) {

                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l), nodes(i + l * l)))
                  // setRandomNode(i, l, nodes,randList)
                  temp = new ArrayBuffer[ActorRef]()
                  // nodes(i)! setNode(nodes)
                } else {
                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l)))
                  //  setRandomNode(i, l, nodes,randList)
                  temp = new ArrayBuffer[ActorRef]()
                  // nodes(i)! setNode(nodes)
                }
              }
              //for the nodes between starting and end nodes
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- 1 + i to l + i - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l), nodes(j + (l * l))))
                    //  setRandomNode(j, l, nodes,randList)
                    temp = new ArrayBuffer[ActorRef]()
                    // nodes(i)! setNode(nodes)
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l)))
                    temp = new ArrayBuffer[ActorRef]()
                    //setRandomNode(j, l, nodes,randList)

                    // nodes(i)! setNode(nodes)
                  }

                }
              }

              //for ending nodes

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l), nodes(i + l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //  nodes(i)! setNode(nodes)
                  //setRandomNode(i + l - 1, l, nodes,randList)
                } else {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //setRandomNode(i + l - 1, l, nodes,randList)
                }
              }

              // for all nodes other than the first and last rows on a plane 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                // inter planar 

                for (j <- i + l to i + (l * l) - l - 1) {
                  if (j % l == 0) {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1), nodes(j + (l * l))))
                      temp = new ArrayBuffer[ActorRef]()
                      //  setRandomNode(j, l, nodes,randList)
                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()
                      // setRandomNode(j, l, nodes,randList)
                    }

                  } else if (j % l == l - 1) {

                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                      //setRandomNode(j, l, nodes,randList)
                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1)))
                      temp = new ArrayBuffer[ActorRef]()
                      //  setRandomNode(j, l, nodes,randList)
                    }
                  } else {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                      // setRandomNode(j, l, nodes,randList)
                    } else {

                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()
                      //  setRandomNode(j, l, nodes,randList)
                    }
                  }

                }

              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1), nodes(i + l * l - l + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(i + (l * l) - l, l, nodes,randList)
                } else {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(i + (l * l) - l, l, nodes,randList)
                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- i + l * l - l + 1 to i + l * l - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l), nodes(j + l * l)))
                    temp = new ArrayBuffer[ActorRef]()
                    //   setRandomNode(j, l, nodes,randList)
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l)))
                    temp = new ArrayBuffer[ActorRef]()
                    //   setRandomNode(j, l, nodes,randList)
                  }

                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l), nodes(i + l * l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //  setRandomNode(i + l * l - 1, l, nodes,randList)
                } else {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(i + l * l - 1, l, nodes,randList)
                }
              }

              for (i <- (l - 1) * (l * l) to l * l by -1 * (l * l)) {
                for (j <- i + 0 to i + l * l - 1) {
                  nodes(j) ! setNeighbours(temp += (nodes(j - l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(j,l,nodes,randList)
                }
              }
              for (j <- 0 to ((l * l * l) - 1)) {
                //  nodes(j) ! setNeighbours(temp += (nodes(j - l * l)))
                temp = new ArrayBuffer[ActorRef]()
                setRandomNode(j, l, nodes, randList)
              }
              var randomNode = Random.nextInt(l * l * l)

              println("selecting the first node randomly to start gossip")
              nodes(randomNode) ! Rumour

              //End of 3Dimp topology set up
              init_time = System.currentTimeMillis()
            }

          }

          case "pushsum" => {

            if (topology == "Line") {

              for (i <- 0 to n - 1)
                nodes += context.actorOf(Props(new pushSumNode(i + 1)), name = "node" + i)

              //setting up the first node
              nodes(0) ! setNeighbours(temp += nodes(1))

              temp = new ArrayBuffer[ActorRef]()

              //end of if Line  
              //setting up the next nodes until the last one 
              for (i <- 1 to n - 2) {
                nodes(i) ! setNeighbours(temp += (nodes(i - 1), nodes(i + 1)))
                temp = new ArrayBuffer[ActorRef]()
              }
              //setting up the last node
              nodes(n - 1) ! setNeighbours(temp += (nodes(n - 2)))
              temp = new ArrayBuffer[ActorRef]()
              //topology complete
              var randomNode = Random.nextInt(n)

              println("selecting the first node randomly to start gossip")
              nodes(randomNode) ! addState(0, 0)

              init_time = System.currentTimeMillis()

            }

            if (topology == "Full") {

              for (i <- 0 to n - 1)
                nodes += context.actorOf(Props(new pushSumNode(i + 1)), name = "node" + i)

              for (i <- 0 to n - 1) {
                {
                  nodes(i) ! setNeighbours(nodes - nodes(i))
                  temp = new ArrayBuffer[ActorRef]()

                }
              }

              var randomNode = Random.nextInt(n)

              println("selecting the first node randomly to start gossip")
              nodes(randomNode) ! addState(0, 0)

              //   sys.scheduler.schedule(0 seconds, 5 minutes, actor, Message())

              init_time = System.currentTimeMillis()
              // end of full
            }

            if (topology == "3d") {

              var p: Double = 0.33
              var l = ceil(pow(n, (p))).toInt
              println((l))
              for (i <- 0 to (l * l * l) - 1)
                nodes += context.actorOf(Props(new pushSumNode(i + 1)), name = "node" + i)

              //for starting nodes 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * l * l)) {
                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l), nodes(i + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }
              //for the nodes between starting and end nodes
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- 1 + i to l + i - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l), nodes(j + (l * l))))
                    temp = new ArrayBuffer[ActorRef]()
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l)))
                    temp = new ArrayBuffer[ActorRef]()
                  }

                }
              }

              //for ending nodes

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l), nodes(i + l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              // for all nodes other than the first and last rows on a plane 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                // inter planar 

                for (j <- i + l to i + (l * l) - l - 1) {
                  if (j % l == 0) {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1), nodes(j + (l * l))))
                      temp = new ArrayBuffer[ActorRef]()

                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()
                    }

                  } else if (j % l == l - 1) {

                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1)))
                      temp = new ArrayBuffer[ActorRef]()
                    }
                  } else {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                    } else {

                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()

                    }
                  }

                }

              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1), nodes(i + l * l - l + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- i + l * l - l + 1 to i + l * l - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l), nodes(j + l * l)))
                    temp = new ArrayBuffer[ActorRef]()
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l)))
                    temp = new ArrayBuffer[ActorRef]()
                  }

                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l), nodes(i + l * l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                } else {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              for (i <- (l - 1) * (l * l) to l * l by -1 * (l * l)) {
                for (j <- i + 0 to i + l * l - 1) {
                  nodes(j) ! setNeighbours(temp += (nodes(j - l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                }
              }

              var randomNode = Random.nextInt(l * l * l)

              println("selecting the first node randomly to start gossip")
              nodes(randomNode) ! addState(0, 0)

              //End of 3D topology set up
              init_time = System.currentTimeMillis()
            }

            if (topology == "imp3d") {

              var p: Double = 0.33
              var l = ceil(pow(n, (p))).toInt
              println((l))
              for (i <- 0 to (l * l * l) - 1)
                nodes += context.actorOf(Props(new pushSumNode(i + 1)), name = "node" + i)

              for (i <- 0 to (l * l * l) - 1)
                randList += 99999
              //  println("randlist"+randList.length)
              //for starting nodes 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * l * l)) {

                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l), nodes(i + l * l)))
                  // setRandomNode(i, l, nodes,randList)
                  temp = new ArrayBuffer[ActorRef]()
                  // nodes(i)! setNode(nodes)
                } else {
                  nodes(i) ! setNeighbours(temp += (nodes(i + 1), nodes(i + l)))
                  //  setRandomNode(i, l, nodes,randList)
                  temp = new ArrayBuffer[ActorRef]()
                  // nodes(i)! setNode(nodes)
                }
              }
              //for the nodes between starting and end nodes
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- 1 + i to l + i - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l), nodes(j + (l * l))))
                    //  setRandomNode(j, l, nodes,randList)
                    temp = new ArrayBuffer[ActorRef]()
                    // nodes(i)! setNode(nodes)
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j + l)))
                    temp = new ArrayBuffer[ActorRef]()
                    //setRandomNode(j, l, nodes,randList)

                    // nodes(i)! setNode(nodes)
                  }

                }
              }

              //for ending nodes

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l), nodes(i + l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //  nodes(i)! setNode(nodes)
                  //setRandomNode(i + l - 1, l, nodes,randList)
                } else {
                  nodes(i + l - 1) ! setNeighbours(temp += (nodes(i + l - 2), nodes(i + l - 1 + l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //setRandomNode(i + l - 1, l, nodes,randList)
                }
              }

              // for all nodes other than the first and last rows on a plane 
              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                // inter planar 

                for (j <- i + l to i + (l * l) - l - 1) {
                  if (j % l == 0) {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1), nodes(j + (l * l))))
                      temp = new ArrayBuffer[ActorRef]()
                      //  setRandomNode(j, l, nodes,randList)
                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()
                      // setRandomNode(j, l, nodes,randList)
                    }

                  } else if (j % l == l - 1) {

                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                      //setRandomNode(j, l, nodes,randList)
                    } else {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1)))
                      temp = new ArrayBuffer[ActorRef]()
                      //  setRandomNode(j, l, nodes,randList)
                    }
                  } else {
                    if (j < ((l - 1) * (l * l))) {
                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1), nodes(j + l * l)))
                      temp = new ArrayBuffer[ActorRef]()
                      // setRandomNode(j, l, nodes,randList)
                    } else {

                      nodes(j) ! setNeighbours(temp += (nodes(j - l), nodes(j + l), nodes(j - 1), nodes(j + 1)))
                      temp = new ArrayBuffer[ActorRef]()
                      //  setRandomNode(j, l, nodes,randList)
                    }
                  }

                }

              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1), nodes(i + l * l - l + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(i + (l * l) - l, l, nodes,randList)
                } else {
                  nodes(i + (l * l) - l) ! setNeighbours(temp += (nodes(i + (l * l) - (2 * l)), nodes(i + (l * l) - l + 1)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(i + (l * l) - l, l, nodes,randList)
                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                for (j <- i + l * l - l + 1 to i + l * l - 2) {
                  if (j < ((l - 1) * (l * l))) {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l), nodes(j + l * l)))
                    temp = new ArrayBuffer[ActorRef]()
                    //   setRandomNode(j, l, nodes,randList)
                  } else {
                    nodes(j) ! setNeighbours(temp += (nodes(j - 1), nodes(j + 1), nodes(j - l)))
                    temp = new ArrayBuffer[ActorRef]()
                    //   setRandomNode(j, l, nodes,randList)
                  }

                }
              }

              for (i <- 0 to (l - 1) * (l * l) by (l * l)) {
                if (i < ((l - 1) * (l * l))) {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l), nodes(i + l * l - 1 + l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //  setRandomNode(i + l * l - 1, l, nodes,randList)
                } else {
                  nodes(i + l * l - 1) ! setNeighbours(temp += (nodes(i + l * l - 2), nodes(i + l * l - 1 - l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(i + l * l - 1, l, nodes,randList)
                }
              }

              for (i <- (l - 1) * (l * l) to l * l by -1 * (l * l)) {
                for (j <- i + 0 to i + l * l - 1) {
                  nodes(j) ! setNeighbours(temp += (nodes(j - l * l)))
                  temp = new ArrayBuffer[ActorRef]()
                  //   setRandomNode(j,l,nodes,randList)
                }
              }
              for (j <- 0 to ((l * l * l) - 1)) {
                //  nodes(j) ! setNeighbours(temp += (nodes(j - l * l)))
                temp = new ArrayBuffer[ActorRef]()
                setRandomNode(j, l, nodes, randList)
              }
              var randomNode = Random.nextInt(l * l * l)

              nodes(randomNode) ! addState(0, 0)

              //   sys.scheduler.schedule(0 seconds, 5 minutes, actor, Message())

              init_time = System.currentTimeMillis()
              //End of 3Dimp topology set up

            }

            //End of pushSum
          }

        }

        //end of start
      }
    /* case "removeNode" => 
      {
        var k=3
        for(i<-0 to k-1)
        {var randomKill = Random.nextInt(k) 
        nodes(randomKill)! Kill       
        }
        
      }
    case Failure =>
      {
      context.system.scheduler.scheduleOnce(0 milliseconds, self, Fail)
      }
      */

    case TerminateNode => {

      no_of_active -= 1

      // println(sender.path.name + "is inactive")
      println("number of active nodes" + no_of_active)
      final_time = System.currentTimeMillis() - init_time
      println("time taken to converge:" + final_time)
      if (no_of_active < 0) {
        final_time = System.currentTimeMillis() - init_time
        println("time taken to converge:" + final_time)
        println("shutting down")
        context.system.shutdown()
      }

    }

  }

}

class pushSumNode(index: Int) extends Actor {
  var neighbourTemp = new ArrayBuffer[ActorRef]()
  var init_time = System.currentTimeMillis()
  var neighbours = new ArrayBuffer[ActorRef]()
  var neighbourCount = 0
  var rumourCount = 0
  var master: ActorRef = null
  var s: Double = index
  var w: Double = 1
  var active = true
  var nextNode = 0
  var tick: Cancellable = null
  var curr_ratio = 0.0
  var last_ratio = 0.0

  def receive = {

    case addState(m, n) =>
      {

        //  println("state(" + m + "," + n + ") from " + sender.path.name + "->" + self.path.name)

        s = (s + m)
        w = (w + n)
        curr_ratio = s / w
        tick = context.system.scheduler.schedule(Duration.create(10, TimeUnit.MILLISECONDS), Duration.create(50, TimeUnit.MILLISECONDS), self, SetState)
        if (rumourCount < 3 && active && abs(curr_ratio - last_ratio) <= 0.0000000001 && neighbourCount > 0) {
          rumourCount += 1
        } else if (abs(curr_ratio - last_ratio) > 0.0000000001 && active && neighbourCount > 0) {
          rumourCount = 0
          last_ratio = curr_ratio
        } else if (rumourCount >= 3 && neighbourCount <= 0) {
          // for(i<-0 to neighbours.length)
          // neighbours(i) ! NodeTerminated
          active = false
          // master ! TerminateNode
          // context.stop(self)
        } else {
          active = false
          // master ! TerminateNode
          // context.stop(self)
        }
      }
    case SetState => {

      if (active && neighbourCount > 0) {
        // println("insde set state")
        nextNode = Random.nextInt(neighbourTemp.length)

        neighbourTemp(nextNode) ! addState(s / 2, w / 2)
        s = (s / 2)
        w = (w / 2)
        // println("state(" + s + "," + w + ") from " + self.path.name + "->" + neighbours(nextNode).path.name)
        //"state("+m+","+n+") from "+sender.path.name+"->"+self.path.name

      } else {

        master ! TerminateNode
        context.stop(self)
        tick.cancel()

      }
    }

    case setNeighbours(neighBourArray) =>
      {
        neighbours ++= neighBourArray
        master = sender
        neighbourCount += neighbours.length
        neighbourTemp = neighbours
        // println("neighbors of "+self.path.name+"->"+neighbours)
      }

    case NodeTerminated =>
      {
        neighbourCount -= 1

        if (neighbourCount <= 0) { //active=false  
          for (i <- 0 to neighbours.length - 1)
            neighbours(i) ! NodeTerminated
        }
        ///println("current neighbours"+neighbours) 
      }

  }

}

class gossipNode extends Actor {
  var init_time = System.currentTimeMillis()
  var neighbours = new ArrayBuffer[ActorRef]()
  var neighboursTemp = new ArrayBuffer[ActorRef]()
  var neighbourCount = 0
  var rumourCount = 0
  var master: ActorRef = null
  var done, finished, reached = false
  var active = true

  var tick: Cancellable = null

  def receive = {

    case Rumour =>
      {

        // println("rumour from " + sender.path.name + "->" + self.path.name)
        rumourCount += 1
        tick = context.system.scheduler.schedule(Duration.create(0, TimeUnit.MILLISECONDS), Duration.create(50, TimeUnit.MILLISECONDS), self, SendRumour)
        if (rumourCount >= 10 && neighbourCount > 0) {
          for (i <- 0 to neighbours.length - 1)
            neighbours(i) ! NodeTerminated

          active = false

        } else if (rumourCount >= 10) {
          active = false

        } else {}
      }
    case SendRumour => {
      if (active && neighbourCount > 0 && rumourCount < 10) {

        var nextNode = Random.nextInt(neighbours.length)

        neighbours(nextNode) ! Rumour

      } else {

        master ! TerminateNode
        context.stop(self)
        tick.cancel()
      }
    }

    case setNeighbours(neighBourArray) =>
      {
        neighbours ++= neighBourArray
        master = sender
        neighboursTemp = neighbours
        neighbourCount += neighBourArray.length
      }

    case NodeTerminated =>
      {
        neighbourCount -= 1

        if (neighbourCount <= 0) { //active=false  
          for (i <- 0 to neighbours.length - 1)
            neighbours(i) ! NodeTerminated

        }

      }

  }

}