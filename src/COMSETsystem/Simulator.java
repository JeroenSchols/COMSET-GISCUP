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
	private PriorityQueue<Event> events = new PriorityQueue<>();

	// The set of empty agents.
	protected TreeSet<AgentEvent> emptyAgents = new TreeSet<>(new AgentEventComparator());

	// The set of resources that with no agent assigned to it yet.
	protected TreeSet<ResourceEvent> waitingResources = new TreeSet<>(new ResourceEventComparator());

	// The maximum life time of a resource in seconds. This is a parameter of the simulator. 
	public long resourceMaximumLifeTime;

	// Configuration Properties for this simulation.
	private Configuration configuration;

	public final long timeResolution = 1000000;

	public boolean dynamicTraffic;

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
	protected long numberOfAgents;

	// The number of assignments that have been made, and dropped off
	protected long totalAssignments = 0;

	// The number of times an agent fails to reach an assigned resource before the resource expires
	protected long totalAbortions = 0;

	// A list of all the agents in the system. Not really used in COMSET, but maintained for
	// a user's debugging purposes.
	ArrayList<BaseAgent> agents;

	// A class that extends BaseAgent and implements a search routing strategy
	protected final Class<? extends FleetManager> fleetManagerClass;

	protected FleetManager fleetManager;

	// Traffic pattern epoch in seconds
	public long trafficPatternEpoch;

	// Traffic pattern step in seconds
	public long trafficPatternStep;

	// Traffic pattern
	public TrafficPattern trafficPattern;

	public final Map<Long, AgentEvent> agentMap = new HashMap<>();
	public final Map<Long, ResourceEvent> resMap = new HashMap<>();

	protected static class IntervalCheckRecord {
		public final long time;
		public final long interval;
		public final long expected_interval;

		IntervalCheckRecord(long time, long interval, long expected_interval) {
			this.time = time;
			this.interval = interval;
			this.expected_interval = expected_interval;
		}

		double ratio() {
			return (interval == 0 && expected_interval == 0) ? 1.0 : expected_interval/(double)interval;
		}
	}

	protected final ArrayList<IntervalCheckRecord> approachTimeCheckRecords = new ArrayList<>();
	protected final ArrayList<IntervalCheckRecord> resourcePickupTimeCheckRecords = new ArrayList<>();

	/**
	 * Constructor of the class Main. This is made such that the type of
	 * agent/resourceAnalyzer used is not hardcoded and the users can choose
	 * whichever they wants.
	 *
	 * @param fleetManagerClass the agent class that is going to be used in this
	 * simulation.
	 */
	public Simulator(Class<? extends FleetManager> fleetManagerClass) {
		this.fleetManagerClass = fleetManagerClass;
	}

	public void removeEvent(Event e) {
		events.remove(e);
	}

	public void addEvent(Event e) {
		events.remove(e);
	}

	/**
	 * Configure the simulation system including:
	 * 
	 * 1. Create a map from the map file and the bounding polygon KML file.
	 * 2. Load the resource data set and map match.
	 * 3. Create the event queue. 
	 *
	 * See COMSETsystem.Configuration and Main.java for detailed description of the parameters.
	 *
	 */
	public void configure() {
		configuration = Configuration.get();
		this.numberOfAgents = configuration.numberOfAgents;
		this.resourceMaximumLifeTime = configuration.resourceMaximumLifetime * timeResolution;
		this.dynamicTraffic = configuration.dynamicTraffic;
		this.trafficPatternEpoch = configuration.trafficPatternEpoch * timeResolution;
		this.trafficPatternStep = configuration.trafficPatternStep * timeResolution;

		MapCreator creator = new MapCreator(configuration.mapJSONFile,
											configuration.boundingPolygonKMLFile,
											this.timeResolution);
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

		MapWithData mapWD = new MapWithData(map, configuration.resourceFile, configuration.agentPlacementRandomSeed);

		// map match resources
		System.out.println("Loading and map-matching resources...");

		fleetManager = createFleetManager();

		// The simulation end time is the expiration time of the last resource.
		// which is return by createMapWithData
		this.simulationEndTime = mapWD.createMapWithData(this, fleetManager);

		// Deploy agents at random locations of the map.
		System.out.println("Randomly placing " + this.numberOfAgents + " agents on the map...");
		mapWD.placeAgentsRandomly(this, fleetManager);

		// Initialize the event queue.
		events = mapWD.getEvents();

		mappingEventId();
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
			long totalSimulationTime = simulationEndTime - simulationStartTime;

			while (!events.isEmpty()) {
				assert events.peek() != null;
				simulationTime = events.peek().time;

				// Extend total simulation time for agent which is still delivering resource
				totalSimulationTime = Math.max(totalSimulationTime, simulationTime - simulationStartTime);

				Event toTrigger = events.poll();
				pb.stepTo((long)(((float)(toTrigger.time - simulationStartTime))
						/ totalSimulationTime * 100.0));

				if (simulationTime <= simulationEndTime || (toTrigger instanceof AgentEvent && ((AgentEvent) toTrigger).hasResPickup())) {
					Event e = toTrigger.trigger();
					if (e != null) {
						events.add(e);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Simulation finished.");

		score.end();
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

		final Runtime runtime = Runtime.getRuntime();
		final NumberFormat format = NumberFormat.getInstance();
		final StringBuilder sb = new StringBuilder();

		final long startTime;
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
			System.out.println("JSON map file: " + configuration.mapJSONFile);
			System.out.println("Resource dataset file: " + configuration.resourceFile);
			System.out.println("Bounding polygon KML file: " + configuration.boundingPolygonKMLFile);
			System.out.println("Number of agents: " + numberOfAgents);
			System.out.println("Number of resources: " + totalResources);
			System.out.println("Resource Maximum Life Time: " + resourceMaximumLifeTime / timeResolution + " seconds");
			System.out.println("Fleet Manager class: " + fleetManagerClass.getName());
			System.out.println("Time resolution: " + timeResolution);

			System.out.println("\n***Statistics***");

			if (totalResources != 0) {
				// Collect the "search" time for the agents that are empty at the end of the simulation.
				// These agents are in search status and therefore the amount of time they spend on
				// searching until the end of the simulation should be counted toward the total search time.
				long totalRemainTime = 0;
				for (AgentEvent ae : emptyAgents) {
					totalRemainTime += (simulationEndTime - ae.startSearchTime);
				}

				sb.append("average agent search time: ")
						.append(Math.floorDiv((totalAgentSearchTime + totalRemainTime) / timeResolution,
								(totalAssignments + emptyAgents.size())))
						.append(" seconds \n");
				sb.append("average resource wait time: ")
						.append(Math.floorDiv(totalResourceWaitTime / timeResolution, totalResources))
						.append(" seconds \n");
				sb.append("resource expiration percentage: ")
						.append(Math.floorDiv(expiredResources * 100, totalResources))
						.append("%\n");
				sb.append("\n");
				sb.append("average agent cruise time: ")
						.append(Math.floorDiv(totalAgentCruiseTime / timeResolution, totalAssignments)).append(" seconds \n");
				sb.append("average agent approach time: ")
						.append(Math.floorDiv(totalAgentApproachTime / timeResolution, totalAssignments)).append(" seconds \n");
				sb.append("average resource trip time: ")
						.append(Math.floorDiv(totalResourceTripTime / timeResolution, totalAssignments))
						.append(" seconds \n");
				sb.append("total number of assignments: ")
						.append(totalAssignments)
						.append("\n");
				sb.append("total number of abortions: ")
						.append(totalAbortions)
						.append("\n");
			} else {
				sb.append("No resources.\n");
			}

			System.out.print(sb.toString());

			// TODO: Remove debugging code below when done.

			System.out.println("********** pickup time checks");
			double l2 = 0.0;
			int below_threshold_count = 0;
			int print_limit = 10;
			double threshold = 2.0;
			for (final IntervalCheckRecord checkRecord: resourcePickupTimeCheckRecords) {
				final double ratio = checkRecord.ratio();
				if (ratio < threshold) {
					if (print_limit > 0) {
						System.out.println(checkRecord.time + "," + ratio);
					}
					print_limit--;
					below_threshold_count++;
				}
				l2 += ratio * ratio;
			}
			System.out.println("Threshold =" + threshold + "; Count =" + below_threshold_count);
			System.out.println("Resource Pickup Ratios RMS =" + Math.sqrt(l2 / resourcePickupTimeCheckRecords.size())
					+ "; Count =" + resourcePickupTimeCheckRecords.size());


			System.out.println("********** Approach time checks");
			l2 = 0.0;
			below_threshold_count = 0;
			print_limit = 10;
			threshold = 2.0;
			for (final IntervalCheckRecord checkRecord: approachTimeCheckRecords) {
				final double ratio = checkRecord.ratio();
				if (ratio < threshold) {
					if (print_limit > 0) {
						System.out.println(checkRecord.time + "," + ratio);
					}
					print_limit--;
					below_threshold_count++;
				}
				l2 += ratio * ratio;
			}
			System.out.println("Threshold =" + threshold + "; Count =" + below_threshold_count);
			System.out.println("Agent Approach Ratios RMS =" + Math.sqrt(l2 / approachTimeCheckRecords.size())
					+ "; Count =" + approachTimeCheckRecords.size());
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
		return numberOfAgents;
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
		return new LocationOnRoad(roadAgentCopy, locationOnRoad.distanceFromStartIntersection);
	}

	public FleetManager createFleetManager() {
		try {
			Constructor<? extends FleetManager> cons = this.fleetManagerClass.getConstructor(CityMap.class);
			return cons.newInstance(this.mapForAgents);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
				InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void mappingEventId() {
		for (Event event : events) {
			if (event instanceof AgentEvent) {
				agentMap.put(event.id, (AgentEvent) event);
			} else if (event instanceof ResourceEvent) {
				resMap.put(event.id, (ResourceEvent) event);
			}
		}
	}
}
