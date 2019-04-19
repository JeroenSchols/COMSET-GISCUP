# COMSET

This project provides the COMSET simulator described in the <a href="https://sigspatial2019.sigspatial.org/giscup2019/problem"> 2019 GISCUP Problem Definition</a>. COMSET simulates crowdsourced taxicabs (called <i>agents</i>) searching for customers (called <i>resources</i>) to pick up in a city. The simulator serves as a standard testbed such that CUP contestants can develop and test their own search algorithms. It will also be the testbed for the evaluation of submissions.

## Overall Logic

At the beginning of a simulation, a map is created from an input OSM JSON map file and cropped by an input bounding polygon. Resources (passengers) are read from an input <a href="https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page"> TLC Trip Record Data Yellow</a> data file<sup>1</sup>. Each resource corresponds to one trip record in the file. Resources are cropped as well by the input bounding polygon so that only those with both pickup location and dropoff location inside the bounding polygon are kept. A certain number of agents (taxicabs) are deployed at random locations on the map. 

The simulation is event driven. In <b>COMSETsystem.Simulator</b> there is a priority queue called events, ordered by time, such that the event that has the smallest time will be processed earliest. Thus there isn’t a global variable time; the simulator just goes from event to event (we can know what time it is by checking the time of the event currently being processed).

Concerning the assignment of resources to agents, resources get assigned to the nearest agent if this agent can get to the resource in less than this resource’s <b>ResourceMaximumLifeTime</b>. Assignments are performed by the callback methods of <b>COMSETsystem.ResourceEvent</b> and <b>COMSETsystem.AgentEvent</b>. These callback methods collectively assume the responsibility of the <b>Assignment Authority</b> module described in <a href="https://sigspatial2019.sigspatial.org/giscup2019/problem"> 2019 GISCUP Problem Definition</a>.

The performance measures, including the average search time, the average wait time, and the expiration percentage, are printed out at the end of the simulation.

<sup>1</sup> Apparently TLC changed the trip record data format starting from July of 2016. The data before July of 2016 contains the latitude and longitude coordinates of the pickup and dropoff locations. Starting from July of 2016 the pickup and dropoff locations are represented by TLC Taxi Zones. For example, Manhattan is divided into 69 Taxi Zones. Clearly, the location resolution of Taxi Zones is insufficient for the CUP problem. Thus COMSET is designed to only work with the data before July of 2016.

## Getting Started

In order to run the COMSET simulator, the project should simply be cloned, built, and then it is ready to run. The simulator comes up with a naive search strategies implemented in <b>UserExamples.AgentRandomDestination</b>. With this strategy, an empty agent randomly chooses an intersection on the map as a destination and travels to the destination along the shortest travel time path. When the destination is reached, the agent randomly chooses another intersection as the next destination. This procedure is repeated until the agent is assigned to a resource. 

The simulator provides a class called <b>COMSETsystem.BaseAgent</b> which defines a base class of an agent implementation. The CUP contestants should extend from this class to implement their own sub-class, just as <b>UserExamples.AgentRandomDestination</b> extends <b>COMSETsystem.BaseAgent</b>.

### Prerequisites

COMSETsystem requires JAVA 8 or up.

When you clone the project Maven should be supported. If it is not, that is no problem, as it is quite easy.
Netbeans usually suports it right away. In Intellij you can just follow the following link.

https://www.jetbrains.com/help/idea/maven-support.html#maven_import_project_start

In Eclipse you can just follow the following link.

https://www.lagomframework.com/documentation/1.4.x/java/EclipseMavenInt.html

### Installing, building, and running COMSET

To install COMSET, <a href="https://github.com/Chessnl/COMSET-GISCUP/archive/V1.0.zip">download</a> from GitHub and unzip. 

Run "<b>mvn install</b>" or "<b>mvn package</b>" to build.

To run COMSET, the main class is Main. The configurable system parameters are defined in etc/config.properties. The project can be run with mvn as follows:

<b>mvn exec:java -Dexec.mainClass="Main"</b>

With the configuration file coming up with the system, the above command will run simulation on the Manhattan road network with 5000 agents using the naive random-destination search strategy. The resources are the trip records for June 1st, 2016 starting from 8:00am until 10:00pm. The simulation should be finished in a few minutes, and you should get something like the following:

average agent search time: 484 seconds<br>
average resource wait time: 283 seconds<br>
resource expiration percentage: 13%<br>

In fact, if you run the simulator without changing anything in the code that is downloaded from GitHub, you should get exactly the same results as shown above. This is because the seed for the random placement of agents is fixed in Main.java. If you want to do truly random experiments, follow the instructions in etc/configure.properties to make the seed be generated by a random number generator.

If you run into "java.lang.OutOfMemoryError: Java heap space", increase the maximum heap space using the -Xmx option in the command line (e.g., -Xmx1024m). If you are using Eclipse, change the -Xmx setting in eclipse.ini.   

## Implementing a Search Strategy
The only way to add a search strategy to COMSET is to build a sub-class of <b>COMSETsystem.BaseAgent</b> and implement <b>planSearchRoute(...)</b> and <b>nextIntersection(...)</b>. 

<b>planSearchRoute</b> is invoked in either of the following two cases: 1. The agent becomes empty and needs to decide a route to use to search for the next resource; 2. The agent reaches the end of the existing route and needs to plan a new route. The output of planSearchRoute is a route to be used until the agent is assigned to a next resource. 

<b>nextIntersection</b> is invoked whenever the agent is empty and reaches an intersection. The method returns the next intersection along the route computed by <b>planSearchRoute</b>.

To run COMSET with a search strategy (i.e., a sub-class of <b>COMSETsystem.BaseAgent</b>), supply the sub-class name through the <b>comset.agent_class</b> parameter defined in <b>etc/configure.properties</b>. The binding to the search strategy occurs at run time; there is no need to change any code of COMSET itself.

## Submission
A contestant should submit the entire COMSET system including the proposed solution implemented as a sub-class of COMSETsystem.BaseAgent. Store this sub-class in the <b>UserExamples</b> folder or another folder created by the participant. There should not be any modifications to the COMSET code base as released at GitHub. A contestant may modify the COMSET code for debugging purposes during the development of their solution. However, when submitting the solution, the COMSET code should be exactly the same as released at GitHub. The only file that a contestant is allowed to modify is <b>etc/configure.properties</b>. A contestant should set <b>comset.agent_class</b> to point to the proposed solution and may add properties that are needed by the solution. 

## Travel Speeds of Agents
In COMSET, each road segment has a pre-defined constant travel speed independent of the time of a day and how many agents are traveling on it. The travel speed at a road segment is based on the road segment's speed limit and is calibrated by the TLC Trip Record data to accommodate for average traffic condition and turn delays in the studied area. The calibration goes as follows. 

1. Compute the average trip duration of all trips recorded in the TLC Trip Record data; call it the <i>TLC_average_trip_duration</i>. 
2. For each trip, compute the shortest travel time from the pickup location of the trip to the dropoff location using speed limits.
3. Compute the average shortest travel time of all trips; call it the <i>map_average_trip_duration</i>. 
4. For each road segment, <i>travel_speed</i> = <i>speed_limit</i> * ((<i>map_average_trip_duration</i>)/(<i>TLC_average_trip_duration</i>)). 

In other words, we slow down the travel speeds so that the average trip time produced by COMSET is the same as that by the real data.

## Authors

* **Robert van Barlingen**
* **João Ferreira**
* **Tijana Klimovic**
* **Jeroen Schols**
* **Wouter de Vries**
* **Bo Xu**
