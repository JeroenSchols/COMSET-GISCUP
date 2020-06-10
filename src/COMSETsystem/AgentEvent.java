package COMSETsystem;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TijanaKlimovic
 * <p>
 * The AgentEvent class represents a moment an agent is going to perform an
 * action in the simmulation, such as becoming empty and picking up a
 * resource, or driving to some other Intersection.
 * <p>
 * An AgentEvent is triggered in either of the following cases:
 * <p>
 * 1. The agent reaches an intersection.
 * 2. The agent drops off a resource.
 * <p>
 * In the case that the agent reaches an intersection, the AgentEvent invokes agent.nextIntersection()
 * to let the agent determine which of the neighboring intersections to go to. The AgentEvent is triggered
 * again when the agent reaches the next intersection, and so on. This is how the agent's search route
 * is executed. The searching ends when the agent is assigned to a resource, in which case the AgentEvent
 * is set to be triggered at the time when the agent drops off the resource to its destination.
 * <p>
 * In the case that the agent drops off a resource, the AgentEvent checks if there are waiting resources. If so,
 * the AgentEvent assigns the agent to the closest waiting resource if the travel time from the agent's current location
 * to the resource is smaller than the resource's remaining life time. Otherwise the AgentEvent moves the agent to
 * the end intersection of the current road.
 */
public class AgentEvent extends Event {

	// Constants representing two causes for which the AgentEvent can be triggered.
	public final static int INTERSECTION_REACHED = 0;
	public final static int DROPPING_OFF = 1;
	enum State {
		INTERSECTION_REACHED,
		PICKING_UP,
		DROPPING_OFF
	}

	// The location at which the event is triggered.
	LocationOnRoad loc;

	ResourceEvent assignedResource;

	State state = State.INTERSECTION_REACHED;

	// The Agent object that the event pertains to.
//	public BaseAgent agent;

	// The cause of the AgentEvent to be triggered, either INTERSECTION_REACHED or DROPPING_OFF.
	public int eventCause;

	/*
	 * The time at which the agent started to search for a resource. This is also the
	 * time at which the agent drops off a resource.
	 */
	long startSearchTime;

	/**
	 * Constructor for class AgentEvent.
	 *
	 * @param loc this agent's location when it becomes empty.
	 */
	public AgentEvent(LocationOnRoad loc, long startedSearch, Simulator simulator, FleetManager fleetManager) {
		super(startedSearch, simulator, fleetManager);
		this.loc = loc;
		this.startSearchTime = startedSearch;
		this.eventCause = DROPPING_OFF; // The introduction of an agent is considered a drop-off event.
	}

	@Override
	Event trigger() throws Exception {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "******** AgentEvent id = " + id+ " triggered at time " + time, this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loc = " + loc, this);

		Event e = null;

		switch (state) {
			case INTERSECTION_REACHED:
				if (assignedResource == null) {
					e = navigate();
				} else {
					e = navigateWithResource();
				}
				break;
			case PICKING_UP:
				e = pickup();
				break;
			case DROPPING_OFF:
				e = dropOff();
				break;
		}
		return e;
	}

	Event navigate() throws Exception {
		assert loc.travelTimeFromStartIntersection == loc.road.travelTime : "Agent not at an intersection.";

		ResourceEvent reachedResource = null;

		for (ResourceEvent resourceEvent : simulator.waitingResources) {
			if (resourceEvent.pickupLoc.road.from.equals(loc.road.to)) {
				reachedResource = resourceEvent;
				break;
			}
		}

		if (reachedResource != null) {
			this.assignedResource = reachedResource;
			simulator.waitingResources.remove(reachedResource);
			// Add cruising time
			long nextEventTime = time + assignedResource.pickupLoc.travelTimeFromStartIntersection;
			update(nextEventTime, assignedResource.pickupLoc, State.PICKING_UP);
			return this;
		}

		Intersection nextIntersection = fleetManager.onReachIntersection(id, time, simulator.agentCopy(loc));



		if (nextIntersection == null) {
			throw new Exception("agent.move() did not return a next location");
		}

		if (!loc.road.to.isAdjacent(nextIntersection)) {
			throw new Exception("move not made to an adjacent location");
		}

		// set location and time of the next trigger
		Road nextRoad = loc.road.to.roadTo(nextIntersection);
		LocationOnRoad nextLocation = new LocationOnRoad(nextRoad, nextRoad.travelTime);
		update(time + nextRoad.travelTime, nextLocation, State.INTERSECTION_REACHED);

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Move to " + nextRoad.to, this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Next trigger time = " + time, this);
		return this;
	}

	Event navigateWithResource() throws Exception {
		assert loc.travelTimeFromStartIntersection == loc.road.travelTime : "Agent not at an intersection.";

		// Check if agent reach dropoff location
		if (loc.road.to.equals(assignedResource.dropoffLoc.road.from)) {
			long nextEventTime = time + assignedResource.dropoffLoc.travelTimeFromStartIntersection;
			update(nextEventTime, assignedResource.dropoffLoc, State.DROPPING_OFF);
			return this;
		}

		Intersection nextIntersection = fleetManager.onReachIntersectionWithResource(id, assignedResource.id, time, simulator.agentCopy(loc));

		if (nextIntersection == null) {
			throw new Exception("agent.move() did not return a next location");
		}

		if (!loc.road.to.isAdjacent(nextIntersection)) {
			throw new Exception("move not made to an adjacent location");
		}

		// set location and time of the next trigger
		Road nextRoad = loc.road.to.roadTo(nextIntersection);
		LocationOnRoad nextLocation = new LocationOnRoad(nextRoad, nextRoad.travelTime);
		update(time + nextRoad.travelTime, nextLocation, State.INTERSECTION_REACHED);

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Move to " + nextRoad.to, this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Next trigger time = " + time, this);
		return this;
	}

	/*
	 * The handler of a pick up event.
	 */
	Event pickup() {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Pickup at " + loc, this);

		long searchTime = time - startSearchTime;
		long waitTime = assignedResource.time - assignedResource.availableTime;

		simulator.totalAgentSearchTime += searchTime;
		simulator.totalResourceWaitTime += waitTime;

		assignedResource.assignedTo(this, time);

		// move to the end intersection of the current road
		long nextEventTime = time + loc.road.travelTime - loc.travelTimeFromStartIntersection;
		LocationOnRoad nextLoc = new LocationOnRoad(loc.road, loc.road.travelTime);
//		setEvent(nextEventTime, nextLoc, INTERSECTION_REACHED);
		update(nextEventTime, nextLoc, State.INTERSECTION_REACHED);

		return this;
	}

	/*
	 * The handler of a DROPPING_OFF event.
	 */
	Event dropOff() {
		startSearchTime = time;
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Dropoff at " + loc, this);

		assignedResource.dropOff(this, time);
		assignedResource = null;

		// move to the end intersection of the current road
		long nextEventTime = time + loc.road.travelTime - loc.travelTimeFromStartIntersection;
		LocationOnRoad nextLoc = new LocationOnRoad(loc.road, loc.road.travelTime);
//		setEvent(nextEventTime, nextLoc, INTERSECTION_REACHED);
		update(nextEventTime, nextLoc, State.INTERSECTION_REACHED);

		return this;
	}

	public void resourceExpired() {
		assignedResource = null;
		state = State.INTERSECTION_REACHED;
	}

	public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePickupLocation, LocationOnRoad resourceDropoffLocation) {
		LocationOnRoad currentLocationAgentCopy = simulator.agentCopy(currentLocation);
		LocationOnRoad resourcePickupLocationAgentCopy = simulator.agentCopy(resourcePickupLocation);
		LocationOnRoad resourceDropoffLocationAgentCopy = simulator.agentCopy(resourceDropoffLocation);
//		agent.assignedTo(currentLocationAgentCopy, currentTime, resourceId, resourcePickupLocationAgentCopy, resourceDropoffLocationAgentCopy);
	}
	
	public void setEvent(long time, LocationOnRoad loc, int eventCause) {
		this.time = time;
		this.loc = loc;
		this.eventCause = eventCause;
	}

	void update(long time, LocationOnRoad loc, State state) {
		this.time = time;
		this.loc = loc;
		this.state = state;
	}
}
