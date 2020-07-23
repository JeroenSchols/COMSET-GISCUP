# COMSET

This project provides the COMSET simulator described in the
[2020 GISCUP Problem Definition](https://sigspatial2020.sigspatial.org/giscup2020/problem). 
COMSET simulates taxicabs (called <i>agents</i>) searching for customers
(called <i>resources</i>) to pick up in a city. 
The simulator serves as a standard testbed such that GISCUP contestants can develop
and test their own fleet management algorithms. It will also serve as a testbed to evaluate submissions.
### Licensing

The GISCUP follows the rules and policies of ACM regarding Code of Ethics, Conflict of Interest Policy,
Policy Against Harassment, and all the other 
guides as specified in 
https://www.acm.org/special-interest-groups/volunteer-resources.

COMSET is licensed under an MIT license. The participants of GISCUP'20 shall license their CUP submissions under an
MIT license as well.

## Overall Logic

At the beginning of a simulation, a map is created from an input OSM JSON map file
and cropped by an input bounding polygon.
Resources (passengers) are read from an input
[TLC Trip Record Data Yellow](https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page)
data file<sup>1</sup>.
Each resource corresponds to one trip record in the file, except that any trip such that its pickup location is identical to its dropoff location is discarded.
Resources are cropped such that only those with
both pickup and dropoff locations within the bounding polygon are kept.
A certain number of agents (taxicabs) are deployed at random locations on the map. 

The simulation is event driven.
In **COMSETsystem.Simulator**
there is a priority queue called events, ordered by time,
such that the event that has the smallest time will be processed earliest.
Thus there isn’t a global variable time; the simulator just goes from event to event
(we can know what time it is by checking the time of the event currently being processed).

For GISCUP 2020 Competition, contestants will write a **FleetManager** class to control the entire fleet
of agents.
After the system randomly places an agent it notifies the **FleetManager** of the agent's location,
at which point the **FleetManager** begins planning the agent's movements.
When a resource becomes available, the **FleetManager** can assign an agent to pickup the resource.
After resource pickup, the **FleetManager** plans the agent's path to the resource's dropoff location.
The **FleetManager** also controls the cruising paths of empty agents.
Essentially, the **FleetManager** has full control of every agent's detailed movement.

Resources will expire **ResourceMaximumLiftTime** seconds after its introduction if it
has not been picked up.
Expired resources are not eligible for assignment to agents.
Picked up resources will never expire.
Consequently, the **FleetManager** must plan the path of an agent to reach the resource's pickup
location before expiration. But after pick-up the **FleetManager** no longer needs to consider expiraton.

The performance measures, including the average search time, the average wait time,
and the expiration percentage, are printed out at the end of the simulation.

<sup>1</sup> Apparently TLC changed the trip record data format starting
from July of 2016. The data before July of 2016 contains the latitude and longitude coordinates
of the pickup and dropoff locations.
Starting from July of 2016 the pickup and dropoff locations are represented by TLC Taxi Zones.
For example, Manhattan is divided into 69 Taxi Zones. Clearly, the location resolution of Taxi Zones
is insufficient for the CUP problem.
Thus COMSET is designed to only work with the data before July of 2016.



## Getting Started

In order to run the COMSET simulator, the project should simply be cloned,
built, and then it is ready to run.
We provide a naive **UserExamples.RandomDestinationFleetManager** as an example and as documentation.
For empty agents
the **RandomDestinationFleetManager** randomly chooses an intersection on the map as a
destination and travels to the destination along the shortest travel time path.
When the destination is reached, the agent randomly chooses another intersection as the next destination.
This procedure is repeated until the agent is assigned to a resource.

When a resource becomes available, **RandomDestinationFleetManager** will assign the agent that can
reach the resource in the shortest amount of time, and direct that agent on
the shortest travel time path to the pickup
location. Upon picking up the resource, **RandomDestinationFleetManager** will direct the agent on
the shortest travel time path to the dropoff point.

The simulator provides an abstract class called <b>COMSETsystem.FleetManager</b>
which defines a base class
of a **FleetManager** implementation.
The CUP contestants should extend from this class to implement their own sub-class,
just as <b>UserExamples.RandomDestinationFleetManager</b> extends <b>COMSETsystem.FleetManager</b>.

### Fleet Manager Interface

Contestants will supply a Fleet Manager class to control the agents and assign resource.
Here are the class methods that contestants must provide:

    public abstract void onAgentIntroduced(long agentId, LocationOnRoad currentLoc, long time);

    public abstract AgentAction onResourceAvailabilityChange(Resource resource, ResourceState state,
                                                             LocationOnRoad currentLoc, long time);

    public abstract Intersection onReachIntersection(long agentId, long time, LocationOnRoad currentLoc);

    public abstract Intersection onReachIntersectionWithResource(long agentId, long time, LocationOnRoad currentLoc,
                                                                 Resource resource);


#### onAgentIntroduced
The simulation calls this method to notify the **FleetManager** that a new agent has been randomly
placed and is available for assignment.

#### onResourceAvailablilityChange
The simulation calls this method to notify the **FleetManager** that the resource's state has changed:

* resource becomes available for pickup
* resource expired
* resource has been dropped off by its assigned agent
* resource has been picked up by an agent.

#### onReachIntersection
Calls to this method notifies that an agent has reached an intersection and is ready for new travel
directions.
This is called whenever any agent without an assigned resources reaches an intersection.
This method allows the **FleetManager** to plan any agent's cruising path, the path it takes when it
has no assigned resource.
The intention is that the **FleetManager** will plan the cruising, to minimize the time it takes to
reach resources for pickup.

#### OnReachIntersectionWithResource
Calls to this method notifies that an agent with an picked up resource reaches an intersection.
This method allows the **FleetMangaer** to plan the path of the agent to the resource's dropoff point.

### Prerequisites

COMSETsystem requires JAVA 8 or up.

When you clone the project Maven should be supported.
If it is not, that is no problem, as it is quite easy.
Netbeans usually suports it right away. In Intellij you can just follow the following link.

https://www.jetbrains.com/help/idea/maven-support.html#maven_import_project_start

In Eclipse you can just follow the following link.

https://www.lagomframework.com/documentation/1.4.x/java/EclipseMavenInt.html

### Installing, building, and running COMSET

To install COMSET, <a href="https://github.com/Chessnl/COMSET-GISCUP/archive/V1.0.zip">download</a>
from GitHub and unzip. 

Run "<b>mvn install</b>" or "<b>mvn package</b>" to build.

To run COMSET, the main class is Main.
The configurable system parameters are defined in etc/config.properties.
The project can be run with mvn as follows:

<b>mvn exec:java -Dexec.mainClass="Main"</b>

With the configuration file coming up with the system, 
the above command will run simulation on the Manhattan road network with 5000 agents
using the naive random-destination search strategy.
The resources are the trip records for June 1st, 2016 starting from 8:00am until 10:00pm.
The simulation should be finished in a few minutes, and you should get something close to the following:

```
average agent search time: 446 seconds
average resource wait time: 169 seconds
resource expiration percentage: 2%

average agent cruise time: 273 seconds
average agent approach time: 157 seconds
average resource trip time: 672 seconds
total number of assignments: 229979
total number of abortions: 6367
total number of searches: 229979
```

In fact, if you run the simulator without changing anything in the code
that is downloaded from GitHub, you should get exactly the same results as shown above.
This is because the seed for the random placement of agents is fixed in Main.java. 
If you want to do truly random experiments, follow the instructions in etc/configure.properties
to make the seed be generated by a random number generator.

If you run into "java.lang.OutOfMemoryError: Java heap space",
increase the maximum heap space using the -Xmx option in the command line (e.g., -Xmx1024m).
If you are using Eclipse, change the -Xmx setting in eclipse.ini.

## Submission
A contestant should submit the entire COMSET system including the proposed solution
implemented as a sub-class of **COMSETsystem.FleetManager**.
Store this sub-class in the <b>UserExamples</b> folder or another folder created by the participant.
There should not be any modifications to the COMSET code base as released at GitHub.
A contestant may modify the COMSET code for debugging purposes during the development of their solution.
However, when submitting the solution, the COMSET code should be exactly the same as released at GitHub.
The only file that a contestant is allowed to modify is <b>etc/configure.properties</b>.
A contestant should set <b>comsset.fleetmanager_class</b> to point to the proposed solution and may add properties
that are needed by the solution. 

## Dynamic Travel Speeds of Agents
Unlike 2019, the travel speeds of agents on road segments will vary over the course of a day based on the traffic pattern reflected in the TLC Trip Record data. The travel speed will not depend on the number of agents in
the road segment. 

The travel speed at a road segment will be
updated every one minute during a simulation.
The travel speed is computed based on the road segment's speed limit and the
TLC Trip Record data to reflect the traffic pattern over the time of a day.
The calibration goes as follows.

1. For every minute of a day, compute the average trip duration of all trips recorded in the TLC Trip Record data that fall into a 15-minute time window starting at the current minute; call it the `TLC_average_trip_duration`.
2. For each trip, compute the shortest travel time from the pickup location of the trip to the dropoff location using speed limits.
3. Compute the average shortest travel time of all trips; call it the `map_average_trip_duration`.
4. For each road segment, `travel_speed_of_current_minute = speed_limit * ((map_average_trip_duration)/(TLC_average_trip_duration))`.

In other words, we adjust the travel speeds so that the average trip time produced by
COMSET is consistent with that of the real data.

<b>Import Notice:</b> COMSET provides built-in functions CityMap::travelTimeBetween() and CityMap::shortestTravelTimePath() for computing the shortest travel time and the shortest travel time path between two locations on the map, respectively. It should be noted that the results returned by these functions are based on the speed limits, not the dynamic travel speed. 

## Authors
## 2020 Authors

* **Po-Han Chen**
* **Steven Tjiang**
* **Bo Xu**

## 2019 Authors

* **Robert van Barlingen**
* **João Ferreira**
* **Tijana Klimovic**
* **Jeroen Schols**
* **Wouter de Vries**
* **Bo Xu**
