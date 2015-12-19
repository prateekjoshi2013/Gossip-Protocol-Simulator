name:Prateek Joshi|
ufid:77813106	  |		
-------------------

The project is about verifying the properties of Gossip style communication and Push Sum Aggregate Algorithms between the nodes in different topologies.Namely:
1)Line
2)Full
3)3D
4)3D imperfect.



What I learnt in this project:

Source: http://doc.akka.io/docs/akka/snapshot/scala/scheduler.html for ticker implementation plus the hint dropped by prof. Dobra in the lecture 
about implementing helped.Implementation of topologies was a bigger hurdle and 3d topology implementation was challenging too.I even got a taste 
of fault tolerance aspect of akka framework wanted to shutdown and restart the nodes in the bonus code using supervisory strategies my attempts 
can be seen in the comments but will have study deeper.I also got to know how to create a logical 3d structure using a one dimensional array while 
implementing the 3-D and 3-D imperfect topologies.  

----------------------------------------------------What is working---------------------------------------------------------------------------------

All the topologies and both the algorithms are working.
imperfect 3-d may not be perfect exactly 6+1 at times, it can be more 6+2 or 6+3 at times.

-------------------------------------------------------Instructions for running the code---------------------------------------------------------------

just run the following command in the sbt console.

sbt run "main GossipSimulator <no. of nodes> <topology> <Algorithm> "
topology=(Full,Line,3d,imp3d)
Algorithms=(gossip ,pushsum)

for ex: sbt "run-main GossipSimulator 8 Full gossip"

Following are the biggest size networks i could run on my computer. 

------------------------------------------------------------------------------------------------------------------------------------------------------
-----------------------------------------------Instructions for running the project bonus code.--------------------------------------------------------
1)Change directory to GossipSimulatorFailure
2)now run the following command: sbt "run-main GossipSimulatorFailureModel 1000 Full <gossip/pushsum>"
-------------------------------------------------------------------------------------------------------------------------------------------------------

Networks stop converging after 1000 number of nodes, for Line topology way earlier than 1000 and then for 3d.


For Gossip:
imperfect 3-d - with 2303 nodes left to converge in 65654 milliseconds for total of 10000 nodes
imperfect 3d - with 4 nodes left to converge in 6754 milliseconds for 1000 nodes
3d- with 16 nodes left to converge in 6287 milliseconds for 1000 nodes
Full -full convergence for 5000 in 34567 milliseconds
Full - Full convergence in 3232 milliseconds for 1000 nodes 
Line - 791 nodes left to converge in  16148 milliseconds
-----------------------------------------------------------------------------------------------------------------------------------------------------
For PushSum:
for 1000 nodes

Line - 987 nodes left to converge in 386 milliseconds
Full - Full Convergence  2338 milliseconds
3-d - 46 nodes left to converge with 2381 milliseconds left
imperfect 3d - Full Convergence -1437 milliseconds

for 5000 nodes:
full - full convergence 10226 milliseconds
imp3d- 717 nodes left to converge in 18573 milliseconds

--------------------------------------------------------------------Output-----------------------------------------------------------------------------
Both the codes will give the current no. of active nodes and the time elapsed.
-------------------------------------------------------------------------------------------------------------------------------------------------------



**This code was tested and all the data was collected on windows machine.**


