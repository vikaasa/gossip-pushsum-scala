Source Files Location:

src/main/scala-2.11/project2.scala

Usage:
	
	sbt "run numNodes { full | 3D | line | imp3D } { gossip | push-sum }"
	
	For bonus question:
	sbt "run numNodes { full | 3D | line | imp3D } { gossip | push-sum } (failure-ratio) (duration of temp failure in milliseconds)"	
	where failure-ratio is the ratio of the number of failed nodes to the number of working nodes.
	
Working:

	1. 	Convergence of Gossip algorithm for all topologies.
	2. 	Convergence of Push-Sum algorithm for all topologies.
	
	Bonus part:
	3. Implementation of node failure for both algorithms in all topologies.
	
	Please refer to the attached PDF file for a thorough report that covers the working of this project in detail.
	
Largest network used:

	1. For Gossip algorithm:
		a) Full network topology: 1,000,000 nodes 
		b) 3D network topology: 1,000,000 nodes
		c) Imperfect 3D topology: 1,000,000 nodes
		d) Line topology: 10,000 nodes

	2. For Push-Sum algorithm:
		a) Full network topology: 1,000,000 nodes 
		b) 3D network topology: 100,000 nodes
		c) Imperfect 3D topology: 1,000,000 nodes
		d) Line topology: 1,000 nodes

*NOTE: To run the algorithms for large networks, you may have increase the heap space by setting the environment variable JAVA_OPTS="-Xmx4G"
	
Sample Outputs: 

$sbt "run numNodes full push-sum"
0.926,100,50.4999999999995

Column1: Convergence time
Column2: Number of nodes
Column3: Computed Average of any random node in network

$sbt "run numNodes full push-sum 0.1 1000"
1.777,100,50.500000000000234,0.1

Column1: Convergence time
Column2: Number of nodes
Column3: Computed Average of any random node in network
Column4: Temporary Node failure ratio 

$sbt "run numNodes full gossip"
0.129,100,0.9

Column1: Convergence time
Column2: Number of nodes
Column3: Ratio of nodes that have heard the rumor(we terminate after 90% of the nodes have heard the rumor. This would always be 0.9)

$sbt "run numNodes full gossip 0.1 1000"
0.285,100,0.9,0.1

Column1: Convergence time
Column2: Number of nodes
Column3: Ratio of nodes that have heard the rumor(we terminate after 90% of the nodes have heard the rumor. This would always be 0.9)
Column4: Temporary Node failure ratio 



