package COMSETsystem;

import java.util.logging.Level;
import java.util.logging.Logger;
import COMSETsystem.FleetManager.ResourceState;

/**
 *
 * @author TijanaKlimovic
 *
 * The ResourceEvent class represents the moment a resource becomes available or expired in
 * the simulation. Therefore there are two cases in which a ResourceEvent object is triggered:
 * 
 * 1. When the resource is introduced to the system and becomes available. In this case, the 
 * resource is assigned (if there are agents available and within reach) to an agent.
 * 2. When the resource gets expired.
 */
public class ResourceEvent extends Event {

	enum State {
		AVAILABLE,
		EXPIRED
	}

	// The location at which the resource is introduced.
	public final LocationOnRoad pickupLoc;
	// The destination of the resource.
	public final LocationOnRoad dropoffLoc;

	// The time at which the resource is introduced
	final long availableTime;

	// The time at which the resource will be expired
	final long expirationTime;

	long pickupTime;

	AgentEvent agentEvent = null;

	AssignmentManager assignmentManager;

	State state;

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
	public ResourceEvent(LocationOnRoad pickupLoc, LocationOnRoad dropoffLoc, long availableTime, long tripTime, Simulator simulator, FleetManager fleetManager, AssignmentManager assignmentManager) {
		super(availableTime, simulator, fleetManager);
		this.pickupLoc = pickupLoc;
		this.dropoffLoc = dropoffLoc;
		this.availableTime = availableTime;
		this.expirationTime = availableTime + simulator.ResourceMaximumLifeTime;
		this.tripTime = tripTime;
		this.state = State.AVAILABLE;
		this.assignmentManager = assignmentManager;
	}

	/**
	 * Constructor for ResourceEvent that overrides tripTime. Makes it easier to test.
	 *
	 * @param availableTime time when this agent is introduced to the system.
	 * @param pickupLoc this resource's location when it becomes available.
	 * @param dropoffLoc this resource's destination location.
	 * @param tripTime the time it takes to go from pickUpLoc and dropoffLoc
	 * @param simulator the simulator object.
	 */
	protected ResourceEvent(LocationOnRoad pickupLoc, LocationOnRoad dropoffLoc, long availableTime, long tripTime, Simulator simulator) {
		super(availableTime);
		this.pickupLoc = pickupLoc;
		this.dropoffLoc = dropoffLoc;
		this.availableTime = availableTime;
		this.expirationTime = availableTime + simulator.ResourceMaximumLifeTime;
		this.tripTime = tripTime;
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
	Event trigger() {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "******** ResourceEvent id = "+ id + " triggered at time " + time, this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loc = " + this.pickupLoc + "," + this.dropoffLoc, this);

		if (simulator.map == null) {
			System.out.println("map is null in resource");
		}
		if (pickupLoc == null) {
			System.out.println("intersection is null");
		}

		Event e = null;
		switch (state) {
			case AVAILABLE:
				available();
				e = this;
				break;
			case EXPIRED:
				expire();
				break;
		}

		return e;
	}

	Resource copyResource() {
		return new Resource(id, expirationTime, agentEvent == null ? -1 : agentEvent.id, simulator.agentCopy(pickupLoc), simulator.agentCopy(dropoffLoc));
	}

	void pickup(AgentEvent agentEvent, long pickupTime) {
		this.pickupTime = pickupTime;
		this.agentEvent = agentEvent;
		simulator.removeEvent(this);
		AgentAction action = fleetManager.onResourceAvailabilityChange(copyResource(), ResourceState.PICKED_UP, simulator.agentCopy(pickupLoc), pickupTime);
		assignmentManager.processAgentAction(action, pickupTime);
	}

	void dropOff(long dropOffTime) {
		long waitTime = pickupTime - availableTime;
		long tripTime = dropOffTime - pickupTime;

		simulator.totalResourceWaitTime += waitTime;
		simulator.totalResourceTripTime += tripTime;
		simulator.totalAssignments++;

		AgentAction action = fleetManager.onResourceAvailabilityChange(copyResource(), ResourceState.DROPPED_OFF, simulator.agentCopy(dropoffLoc), dropOffTime);
		assignmentManager.processAgentAction(action, dropOffTime);
	}

	private void available() {
		++simulator.totalResources;

		simulator.waitingResources.add(this);
		AgentAction action = fleetManager.onResourceAvailabilityChange(copyResource(), ResourceState.AVAILABLE, simulator.agentCopy(pickupLoc), time);
		assignmentManager.processAgentAction(action, time);
		time = expirationTime;
		state = State.EXPIRED;
	}

	private void expire() {
		System.out.println("Resource " + id + " expired");
		simulator.expiredResources++;
		simulator.totalResourceWaitTime += simulator.ResourceMaximumLifeTime;
		simulator.waitingResources.remove(this);

		AgentAction action = fleetManager.onResourceAvailabilityChange(copyResource(), ResourceState.EXPIRED, simulator.agentCopy(pickupLoc), time);
		assignmentManager.processAgentAction(action, time);
		if (agentEvent != null) {
			agentEvent.abortResource();
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Expired.", this);
	}
}
