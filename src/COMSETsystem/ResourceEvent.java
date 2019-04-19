package COMSETsystem;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TijanaKlimovic
 */
/**
 * The ResourceEvent class represents the moment a resource becomes available or expired in
 * the simulation. Therefore there are two cases in which a ResourceEvent object is triggered:
 * 
 * 1. When the resource is introduced to the system and becomes available. In this case, the 
 * resource is assigned (if there are agents available and within reach) to an agent.
 * 2. When the resource gets expired.
 */
public class ResourceEvent extends Event {

	// Constants representing two causes for which the ResourceEvent can be triggered.
	public final static int BECOME_AVAILABLE = 0;
	public final static int EXPIRED = 1;

	// The location at which the resource is introduced.
	public final LocationOnRoad pickupLoc;
	// The destination of the resource.
	public final LocationOnRoad dropoffLoc;

	// The time at which the resource is introduced
	final long availableTime; 

	// The time at which the resource will be expired
	final long expirationTime;

	// The cause of the AgentEvent to be triggered, either BECOME_AVAILABLE or EXPIRED
	int eventCause;

	// The shortest travel time from pickupLoc to dropoffLoc
	public long tripTime;

	/**
	 * Constructor for class ResourceEvent.
	 *
	 * @param availableTime time when this agent is introduced to the system.
	 * @param pickupLoc this resource's location when it becomes available.
	 * @param dropoffLoc this resource's destination location.
	 * @param simulator the simulator object.
	 */
	public ResourceEvent(LocationOnRoad pickupLoc, LocationOnRoad dropoffLoc, long availableTime, Simulator simulator) {
		super(availableTime, simulator);
		this.pickupLoc = pickupLoc;
		this.dropoffLoc = dropoffLoc;
		this.availableTime = availableTime;
		this.eventCause = BECOME_AVAILABLE;
		this.expirationTime = availableTime + simulator.ResourceMaximumLifeTime;
		this.tripTime = simulator.map.travelTimeBetween(pickupLoc, dropoffLoc);
	}

	/**
	 * Whenever a resource arrives/becomes available an event corresponding to
	 * it gets triggered. When it triggers it checks for all the active agents
	 * which agent can get to the resource the fastest. The closest agent is
	 * saved in the variable bestAgent. If there are no active agents or no
	 * agent can get in time to the resource, the current resource gets added to
	 * waitingResources such that once an agent gets available it will check if
	 * it can get to the resource in time. Furthermore, calculate the score of
	 * this assignment according to the scoring rules. Also remove the assigned
	 * agent from the PriorityQueue and from activeAgents.
	 */
	@Override
	Event trigger() throws Exception {

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "******** ResourceEvent id = "+Long.toString(id) + " triggered at time " + time, this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loc = " + this.pickupLoc + "," + this.dropoffLoc, this);
		if (simulator.map == null) {
			System.out.println("map is null in resource");
		}
		if (pickupLoc == null) {
			System.out.println("intersection is null");
		}
		if (eventCause == BECOME_AVAILABLE) {
			Event e = becomeAvailableHandler();
			return e;
		} else {
			expireHandler();
			return null;
		}

	}

	/*
	 * Handler of a BECOME_AVAILABLE event
	 */
	public Event becomeAvailableHandler() {
		//total number of resources from dataset appearing through the simulation increases
		++simulator.totalResources;

		// finds the agent with least travel time between itself and this resource
		AgentEvent bestAgent = null;
		long earliest = Long.MAX_VALUE;
		LocationOnRoad bestAgentLocationOnRoad = null;
		for (AgentEvent agent : simulator.emptyAgents) {

			// Calculate the travel time from the agent's current location to resource.
			// Assumption: agent.time is the arrival time at the end intersection of agent.loc.road. 
			// This assumption is true for empty agents. Notice that when agents are initially introduced
			// to the system, they are empty and agent.time is not necessarily the time to arrive at the end intersection.
			// However, all the agents are triggered once before the earliest resource (see MapWithData.createMapWithData).
			// When that happens, agent.time is updated to the end intersection arrival time. 
			// Thus the assumption is still true.
			long travelTimeToEndIntersection = agent.time - time;
			long travelTimeFromStartIntersection = agent.loc.road.travelTime - travelTimeToEndIntersection;
			LocationOnRoad agentLocationOnRoad = new LocationOnRoad(agent.loc.road, travelTimeFromStartIntersection);
			long travelTime = simulator.map.travelTimeBetween(agentLocationOnRoad, pickupLoc);
			long arriveTime = travelTime + time;
			if (arriveTime < earliest) {
				bestAgent = agent;
				earliest = arriveTime;
				bestAgentLocationOnRoad = agentLocationOnRoad;
			}
		}

		if (earliest > availableTime + simulator.ResourceMaximumLifeTime) {
			simulator.waitingResources.add(this);
			this.time += simulator.ResourceMaximumLifeTime;
			this.eventCause = EXPIRED;
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Setup expiration event at time " + this.time, this);
			return this;
		} else { // make assignment
			// update the statistics       	
			long cruiseTime = time - bestAgent.startSearchTime;
			long approachTime = earliest - time;
			long searchTime = cruiseTime + approachTime;
			long waitTime = earliest - availableTime;

			simulator.totalAgentCruiseTime += cruiseTime;
			simulator.totalAgentApproachTime += approachTime;
			simulator.totalAgentSearchTime += searchTime;
			simulator.totalResourceWaitTime += waitTime;
			simulator.totalResourceTripTime += tripTime;
			simulator.totalAssignments++;


			// Inform the assignment to the agent.
			bestAgent.assignedTo(bestAgentLocationOnRoad, time, id, pickupLoc, dropoffLoc);

			// "Label" the agent as occupied.
			simulator.emptyAgents.remove(bestAgent);

			simulator.events.remove(bestAgent);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Assigned to agent id = " + bestAgent.id + " currently at " + bestAgent.loc, this);

			bestAgent.setEvent(earliest + tripTime, dropoffLoc, AgentEvent.DROPPING_OFF);

			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "From agent to resource = " + approachTime + " seconds.", this);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "From pickupLoc to dropoffLoc = " + tripTime + " seconds.", this);            
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "cruise time = " + cruiseTime + " seconds.", this);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "approach time = " + approachTime + " seconds.", this);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "search time = " + searchTime + " seconds.", this);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "wait time = " + waitTime + " seconds.", this);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Next agent trigger time = " + bestAgent.time, this);

			// Add the event back to the event queue.
			return bestAgent;
		}
	}

	/*
	 * Handler of an EXPIRED event.
	 */
	public void expireHandler() {
		simulator.expiredResources ++;
		simulator.totalResourceWaitTime += simulator.ResourceMaximumLifeTime;
		simulator.waitingResources.remove(this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Expired.", this);

	}
}
