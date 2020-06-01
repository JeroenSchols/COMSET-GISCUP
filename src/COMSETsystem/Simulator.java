package COMSETsystem;

import MapCreation.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.*;
import me.tongfei.progressbar.*;


import DataParsing.*;

/**
 * The Simulator class defines the major steps of the simulation. It is
 * responsible for loading the map, creating the necessary number of agents,
 * creating a respective AgentEvent for each of them such that they are added
 * to the events PriorityQueue. Furthermore it is also responsible for dealing 
 * with the arrival of resources, map matching them to the map, and assigning  
 * them to agents. This produces the score according to the scoring rules.
 * <p>
 * The time is simulated by the events having a variable time, this time
 * corresponds to when something will be empty and thus needs some
 * interaction (triggering). There's an event corresponding to every existent
 * Agent and for every resource that hasn't arrived yet. All of this events are
 * in a PriorityQueue called events which is ordered by their time in an
 * increasing way.
 */
public class Simulator {

	// The map that everything will happen on.
	protected CityMap map;

	// A deep copy of map to be passed to agents. 
	// This is a way to make map unmodifiable.
	protected CityMap mapForAgents;

	// The event queue.
	protected PriorityQueue<Event> events = new PriorityQueue<>();

	// The set of empty agents.
	protected TreeSet<AgentEvent> emptyAgents = new TreeSet<>(new AgentEventComparator());

	// The set of resources that with no agent assigned to it yet.
	protected TreeSet<ResourceEvent> waitingResources = new TreeSet<>(new ResourceEventComparator());

	// The maximum life time of a resource in seconds. This is a parameter of the simulator. 
	public long ResourceMaximumLifeTime; 

	// Full path to an OSM JSON map file
	protected String mapJSONFile;


	// Full path to a TLC New York Yellow trip record file
	protected String resourceFile = null;

	// Full path to a KML defining the bounding polygon to crop the map
	protected String boundingPolygonKMLFile;

	// The beginning time of the simulation
	protected long simulationStartTime;

	// The current Simulation Time
	protected long simulationTime;

	// The simulation end time is the expiration time of the last resource.
	protected long simulationEndTime; 

	// Total trip time of all resources to which agents have been assigned.
	protected long totalResourceTripTime = 0;

	// Total wait time of all resources. The wait time of a resource is the amount of time
	// since the resource is introduced to the system until it is picked up by an agent.
	protected long totalResourceWaitTime = 0;

	// Total search time of all agents. The search time of an agent for a research is the amount of time 
	// since the agent is labeled as empty, i.e., added to emptyAgents, until it picks up a resource.  
	protected long totalAgentSearchTime = 0;

	// Total cruise time of all agents. The cruise time of an agent for a research is the amount of time 
	// since the agent is labeled as empty until it is assigned to a resource.
	protected long totalAgentCruiseTime = 0;

	// Total approach time of all agents. The approach time of an agent for a research is the amount of time
	// since the agent is assigned to a resource until agent reaches the resource.
	protected long totalAgentApproachTime = 0;

	// The number of expired resources.
	protected long expiredResources = 0;

	// The number of resources that have been introduced to the system.
	protected long totalResources = 0;

	// The number of agents that are deployed (at the beginning of the simulation). 
	protected long totalAgents;

	// The number of assignments that have been made.
	protected long totalAssignments = 0;

	// A list of all the agents in the system. Not really used in COMSET, but maintained for
	// a user's debugging purposes.
	ArrayList<BaseAgent> agents;

	// A class that extends BaseAgent and implements a search routing strategy
	protected final Class<? extends BaseAgent> agentClass;

	/**
	 * Constructor of the class Main. This is made such that the type of
	 * agent/resourceAnalyzer used is not hardcoded and the users can choose
	 * whichever they wants.
	 *
	 * @param agentClass the agent class that is going to be used in this
	 * simulation.
	 */
	public Simulator(Class<? extends BaseAgent> agentClass) {
		this.agentClass = agentClass;
	}

	/**
	 * Configure the simulation system including:
	 * 
	 * 1. Create a map from the map file and the bounding polygon KML file.
	 * 2. Load the resource data set and map match.
	 * 3. Create the event queue. 
	 *
	 * See Main.java for detailed description of the parameters.
	 * 
	 * @param mapJSONFile The map file 
	 * @param resourceFile The dataset file
	 * @param totalAgents The total number of agents to deploy
	 * @param boundingPolygonKMLFile The KML file defining a bounding polygon of the simulated area
	 * @param maximumLifeTime The maximum life time of a resource
	 * @param agentPlacementRandomSeed The see for the random number of generator when placing the agents
	 * @param speedReduction The speed reduction to accommodate traffic jams and turn delays
	 */
	public void configure(String mapJSONFile, String resourceFile, Long totalAgents, String boundingPolygonKMLFile,
						  Long maximumLifeTime, long agentPlacementRandomSeed, double speedReduction) {

		this.mapJSONFile = mapJSONFile;

		this.totalAgents = totalAgents;

		this.boundingPolygonKMLFile = boundingPolygonKMLFile;

		this.ResourceMaximumLifeTime = maximumLifeTime;

		this.resourceFile = resourceFile;

		MapCreator creator = new MapCreator(this.mapJSONFile, this.boundingPolygonKMLFile, speedReduction);
		System.out.println("Creating the map...");

		creator.createMap();

		// Output the map
		map = creator.outputCityMap();

		// Pre-compute shortest travel times between all pairs of intersections.
		System.out.println("Pre-computing all pair travel times...");
		map.calcTravelTimes();

		// Make a map copy for agents to use so that an agent cannot modify the map used by
		// the simulator
		mapForAgents = map.makeCopy();

		MapWithData mapWD = new MapWithData(map, this.resourceFile, agentPlacementRandomSeed);

		// map match resources
		System.out.println("Loading and map-matching resources...");

		// The simulation end time is the expiration time of the last resource.
		// which is return by createMapWithData
		this.simulationEndTime = mapWD.createMapWithData(this);

		// Deploy agents at random locations of the map.
		System.out.println("Randomly placing " + this.totalAgents + " agents on the map...");
		agents = mapWD.placeAgentsRandomly(this);

		// Initialize the event queue.
		events = mapWD.getEvents();
	}

	/**
	 * This method corresponds to running the simulation. An object of ScoreInfo
	 * is created in order to keep track of performance in the current
	 * simulation. Go through every event until the simulation is over.
	 *
	 */
	public void run() {
		System.out.println("Running the simulation...");

		ScoreInfo score = new ScoreInfo();
		if (map == null) {
			System.out.println("map is null at beginning of run");
		}
		try (ProgressBar pb = new ProgressBar("Progress:", 100, ProgressBarStyle.ASCII)) {
			assert events.peek() != null;
			simulationStartTime = simulationTime = events.peek().time;
			while (simulationTime <= simulationEndTime) {
				assert events.peek() != null;
				simulationTime = events.peek().time;
				Event toTrigger = events.poll();
				pb.stepTo((long)(((float)(toTrigger.time - simulationStartTime))
						/ (simulationEndTime - simulationStartTime) * 100.0));
				Event e = toTrigger.trigger();
				if (e != null) { 
					events.add(e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Simulation finished.");

		score.end();
	}

	/**
	 * Get the closest resource that will not expire before the agent reaches it
	 *
	 * @param agentLoc  Location of agent.
	 * @return PickUp instance with ResourceAgent and time of pickup, or empty PickUp
	 */
	public PickUp FindEarliestPickup(final LocationOnRoad agentLoc) {
		// Check if there are resources waiting to be picked up by an agent.
		if (waitingResources.size() > 0) {
			ResourceEvent resource = null;
			long earliest = Long.MAX_VALUE;
			for (ResourceEvent res : waitingResources) {
				// If res is in waitingResources, then it must have not expired yet
				// testing null pointer exception
				long travelTime = Long.MAX_VALUE;
				if (agentLoc == null) {
					System.out.println("loc is null");
				} else if (res.pickupLoc == null) {
					System.out.println("res.loc is null");
				} else {
					travelTime = map.travelTimeBetween(agentLoc, res.pickupLoc);
				}

				if (travelTime != Long.MAX_VALUE) {
					// if the resource is reachable before expiration
					long arriveTime = simulationTime + travelTime;
					if (arriveTime <= res.expirationTime && arriveTime < earliest) {
						earliest = arriveTime;
						resource = res;
					}
				}
			}
			return new PickUp(resource, earliest);
		} else {
			return new PickUp(null, 0);
		}
	}

	protected static class PickUp {
		private final ResourceEvent resource;
		private final long time;

		public PickUp(ResourceEvent resource, long time) {
			this.resource = resource;
			this.time = time;
		}

		public ResourceEvent getResource() {
			return resource;
		}

		public long getTime() {
			return time;
		}

	}

	/**
	 * This class is used to give a performance report and the score. It prints
	 * the total running time of the simulation, the used memory and the score.
	 * It uses Runtime which allows the application to interface with the
	 * environment in which the application is running. 
	 */
	class ScoreInfo {

		Runtime runtime = Runtime.getRuntime();
		NumberFormat format = NumberFormat.getInstance();
		StringBuilder sb = new StringBuilder();

		long startTime;
		long allocatedMemory;

		/**
		 * Constructor for ScoreInfo class. Runs beginning, this method
		 * initializes all the necessary things.
		 */
		ScoreInfo() {
			startTime = System.nanoTime();
			// Suppress memory allocation information display
			// beginning();
		}

		/**
		 * Initializes and gets the max memory, allocated memory and free
		 * memory. All of these are added to the Performance Report which is
		 * saved in the StringBuilder. Furthermore also takes the time, such
		 * that later on we can compare to the time when the simulation is over.
		 * The allocated memory is also used to compare to the allocated memory
		 * by the end of the simulation.
		 */
		void beginning() {
			// Getting the memory used
			long maxMemory = runtime.maxMemory();
			allocatedMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();

			// probably unnecessary
			sb.append("Performance Report: " + "\n");
			sb.append("free memory: ").append(format.format(freeMemory / 1024)).append("\n");
			sb.append("allocated memory: ").append(format.format(allocatedMemory / 1024)).append("\n");
			sb.append("max memory: ").append(format.format(maxMemory / 1024)).append("\n");

			// still looking into this one "freeMemory + (maxMemory -
			// allocatedMemory)"
			sb.append("total free memory: ")
					.append(format.format(
							(freeMemory + (maxMemory - allocatedMemory)) / 1024))
					.append("\n");

			System.out.print(sb.toString());
		}

		/**
		 * Calculate the time the simulation took by taking the time right now
		 * and comparing to the time when the simulation started. Add the total
		 * time to the report and the score as well. Furthermore, calculate the
		 * allocated memory by the participant's implementation by comparing the
		 * previous allocated memory with the current allocated memory. Print
		 * the Performance Report.
		 */
		void end() {
			// Empty the string builder
			sb.setLength(0);

			long endTime = System.nanoTime();
			long totalTime = (endTime - startTime) / 1000000000;

			System.out.println("\nrunning time: " + totalTime);

			System.out.println("\n***Simulation environment***");
			System.out.println("JSON map file: " + mapJSONFile);
			System.out.println("Resource dataset file: " + resourceFile);
			System.out.println("Bounding polygon KML file: " + boundingPolygonKMLFile);
			System.out.println("Number of agents: " + totalAgents);
			System.out.println("Number of resources: " + totalResources);
			System.out.println("Resource Maximum Life Time: " + ResourceMaximumLifeTime + " seconds");
			System.out.println("Agent class: " + agentClass.getName());

			System.out.println("\n***Statistics***");
		
			if (totalResources != 0) {
				// Collect the "search" time for the agents that are empty at the end of the simulation.
				// These agents are in search status and therefore the amount of time they spend on
				// searching until the end of the simulation should be counted toward the total search time.
				long totalRemainTime = 0;
				for (AgentEvent ae: emptyAgents) {
					totalRemainTime += (simulationEndTime - ae.startSearchTime); 
				}

				sb.append("average agent search time: ")
						.append(Math.floorDiv(totalAgentSearchTime + totalRemainTime,
								(totalAssignments + emptyAgents.size())))
						.append(" seconds \n");
				sb.append("average resource wait time: ")
						.append(Math.floorDiv(totalResourceWaitTime, totalResources))
						.append(" seconds \n");
				sb.append("resource expiration percentage: ")
						.append(Math.floorDiv(expiredResources * 100, totalResources))
						.append("%\n");
				sb.append("\n");
				sb.append("average agent cruise time: ")
						.append(Math.floorDiv(totalAgentCruiseTime, totalAssignments)).append(" seconds \n");
				sb.append("average agent approach time: ")
						.append(Math.floorDiv(totalAgentApproachTime, totalAssignments)).append(" seconds \n");
				sb.append("average resource trip time: ")
						.append(Math.floorDiv(totalResourceTripTime, totalAssignments))
						.append(" seconds \n");
				sb.append("total number of assignments: ")
						.append(totalAssignments)
						.append("\n");
			} else {
				sb.append("No resources.\n");
			}

			System.out.print(sb.toString());
		}
	}

	/**
	 * Compares agent events
	 */
	static class AgentEventComparator implements Comparator<AgentEvent> {

		/**
		 * Checks if two agentEvents are the same by checking their ids.
		 * 
		 * @param a1 The first agent event
		 * @param a2 The second agent event
		 * @return returns 0 if the two agent events are the same, 1 if the id of
		 * the first agent event is bigger than the id of the second agent event,
		 * -1 otherwise
		 */
		public int compare(AgentEvent a1, AgentEvent a2) {
			return Long.compare(a1.id, a2.id);
		}
	}

	/**
	 * Compares resource events
	 */
	static class ResourceEventComparator implements Comparator<ResourceEvent> {
		/**
		 * Checks if two resourceEvents are the same by checking their ids.
		 * 
		 * @param a1 The first resource event
		 * @param a2 The second resource event
		 * @return returns 0 if the two resource events are the same, 1 if the id of
		 * the resource event is bigger than the id of the second resource event,
		 * -1 otherwise
		 */
		public int compare(ResourceEvent a1, ResourceEvent a2) {
			return Long.compare(a1.id, a2.id);
		}
	}

	/**
	 * Retrieves the total number of agents
	 * 
	 * @return {@code totalAgents }
	 */
	public long totalAgents() {
		return totalAgents;
	}

	/**
	 * Retrieves the CityMap instance of this simulation
	 * 
	 * @return {@code map }
	 */
	public CityMap getMap() {
		return map;
	}

	/**
	 * Sets the events of the simulation.
	 * 
	 * @param events The PriorityQueue of events
	 */
	public void setEvents(PriorityQueue<Event> events) {
		this.events = events;
	}

	/**
	 * Retrieves the queue of events of the simulation.
	 * 
	 * @return {@code events }
	 */
	public PriorityQueue<Event> getEvents() {
		return events;
	}

	/**
	 * Gets the empty agents in the simulation
	 * 
	 * @return {@code emptyAgents }
	 */
	public TreeSet<AgentEvent> getEmptyAgents() {
		return emptyAgents;
	}

	/**
	 * @param agent Add this agent to the set of empty agents
	 */
	public void addEmptyAgent(AgentEvent agent) {
		this.emptyAgents.add(agent);
	}

	/**
	 * Sets the empty agents in the simulation
	 * 
	 * @param emptyAgents The TreeSet of agent events to set.
	 */
	public void setEmptyAgents(TreeSet<AgentEvent> emptyAgents) {
		this.emptyAgents = emptyAgents;
	}

	/**
	 * Make an agent copy of locationOnRoad so that an agent cannot modify the attributes of the road.
	 * 
	 * @param locationOnRoad the location to make a copy for
	 * @return an agent copy of the location 
	 */
	public LocationOnRoad agentCopy(LocationOnRoad locationOnRoad) {
		Intersection from = mapForAgents.intersections().get(locationOnRoad.road.from.id);
		Intersection to = mapForAgents.intersections().get(locationOnRoad.road.to.id);
		Road roadAgentCopy = from.roadsMapFrom.get(to);
		return new LocationOnRoad(roadAgentCopy, locationOnRoad.travelTimeFromStartIntersection);
	}

	public BaseAgent MakeAgent(long id) {
		try {
			Constructor<? extends BaseAgent> cons = this.agentClass.getConstructor(Long.TYPE, CityMap.class);
			return cons.newInstance(id, this.mapForAgents);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
				InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}


}
