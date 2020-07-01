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

	enum State {
		INITIAL,
		INTERSECTION_REACHED,
		PICKING_UP,
		DROPPING_OFF,
	}

	// The location at which the event is triggered.
	LocationOnRoad loc;

	ResourceEvent assignedResource;

	boolean isPickup = false;

	State state = State.INITIAL;

	/*
	 * The time at which the agent started to search for a resource. This is also the
	 * time at which the agent drops off a resource.
	 */
	long startSearchTime;

	long assignTime;

	LocationOnRoad assignLocation;

	/**
	 * Constructor for class AgentEvent.
	 *
	 * @param loc this agent's location when it becomes empty.
	 */
	public AgentEvent(LocationOnRoad loc, long startedSearch, Simulator simulator, FleetManager fleetManager) {
		super(startedSearch, simulator, fleetManager);
		this.loc = loc;
		this.startSearchTime = startedSearch;
	}

	@Override
	Event trigger() throws Exception {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "******** AgentEvent id = " + id+ " triggered at time " + time, this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loc = " + loc, this);

		switch (state) {
			case INITIAL:
				navigateToNearestIntersection();
				break;
			case INTERSECTION_REACHED:
				navigate();
				break;
			case PICKING_UP:
				if (assignedResource == null) {
					moveToEndIntersection();
				} else {
					pickup();
				}
				break;
			case DROPPING_OFF:
				dropOff();
				break;
		}
		return this;
	}

	boolean hasResPickup() {
		return isPickup;
	}

	void assignTo(ResourceEvent resourceEvent, long time) throws UnsupportedOperationException {
		this.assignedResource = resourceEvent;
		this.assignTime = time;
		this.assignLocation = getCurrentLocation(time);

		if (isOnSameRoad(loc, assignedResource.pickupLoc)) {

			long currentLocTravelFromStart = loc.road.travelTime - (this.time - time);
			if (currentLocTravelFromStart <= assignedResource.pickupLoc.travelTimeFromStartIntersection) {

				long nextEventTime =
						assignedResource.pickupLoc.travelTimeFromStartIntersection - currentLocTravelFromStart + time;
				update(nextEventTime, assignedResource.pickupLoc, State.PICKING_UP);

				simulator.removeEvent(this);
				simulator.getEvents().add(this);
			}
		}
	}

	/**
	 * Compute this agent's current location based on the current time. This should work for agent's
	 * in all states.
	 * @param time This should be the current simulation time.
	 * @return the current location
	 */
	private LocationOnRoad getCurrentLocation(long time) throws UnsupportedOperationException {

		switch(state) {
			case INTERSECTION_REACHED:
			case PICKING_UP:
			case DROPPING_OFF:
				break;
			default:
				throw new UnsupportedOperationException("getCurrentLocation called on unsupported state");
		}
		return new LocationOnRoad(loc.road,
				loc.travelTimeFromStartIntersection - (this.time - time));
	}

	void abortResource() {
		assignedResource = null;
		isPickup = false;
		if (state == State.PICKING_UP) {
			moveToEndIntersection();
		}
	}

	void navigate() throws Exception {
		assert loc.travelTimeFromStartIntersection == loc.road.travelTime : "Agent not at an intersection.";

		if (isArrivingPickupLoc()) {
			long nextEventTime = time + assignedResource.pickupLoc.travelTimeFromStartIntersection;
			update(nextEventTime, assignedResource.pickupLoc, State.PICKING_UP);
			return;
		}

		if (isArrivingDropOffLoc()) {
			long nextEventTime = time + assignedResource.dropoffLoc.travelTimeFromStartIntersection;
			update(nextEventTime, assignedResource.dropoffLoc, State.DROPPING_OFF);
			return;
		}


		Intersection nextIntersection;
		if (isPickup && assignedResource != null) {
			nextIntersection = fleetManager.onReachIntersectionWithResource(id, time, simulator.agentCopy(loc),
					assignedResource.copyResource());
		} else {
			nextIntersection = fleetManager.onReachIntersection(id, time, simulator.agentCopy(loc));
		}

		if (nextIntersection == null) {
			throw new Exception("FleetManager did not return a next location");
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
	}

	private void navigateToNearestIntersection() {
		startSearchTime = time;

		fleetManager.onAgentIntroduced(id, simulator.agentCopy(loc), time);

		moveToEndIntersection();
	}

	private boolean isArrivingPickupLoc() {
		return !isPickup && assignedResource != null && assignedResource.pickupLoc.road.from.equals(loc.road.to);
	}

	private boolean isArrivingDropOffLoc() {
		return isPickup && assignedResource != null && assignedResource.dropoffLoc.road.from.equals(loc.road.to);
	}

	/*
	 * The handler of a pick up event.
	 */
	private void pickup() throws UnsupportedOperationException {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Pickup at " + loc, this);

		isPickup = true;
		long searchTime = time - startSearchTime;
		long approachTime = time - assignTime;
		long staticApproachTime = simulator.map.travelTimeBetween(assignLocation, loc);

		simulator.approachTimeCheckRecords.add(new Simulator.IntervalCheckRecord(
				assignTime, approachTime, staticApproachTime));

		// Resource had been wiating from introductionTime (i.e. when it was available) to now (time that this
		// pickup event triggered).
		long waitTime = time - assignedResource.availableTime;

		simulator.totalAgentSearchTime += searchTime;
		simulator.totalResourceWaitTime += waitTime;
		simulator.totalAgentCruiseTime += assignTime - startSearchTime;
		simulator.totalAgentApproachTime += time - assignTime;

		assignedResource.pickup(this, time);

		AgentAction action = fleetManager.onResourceAvailabilityChange(assignedResource.copyResource(), FleetManager.ResourceState.PICKED_UP, simulator.agentCopy(loc), time);

		if (isValidAssignmentAction(action)) {
			ResourceEvent resourceEvent = simulator.resMap.get(action.resId);
			AgentEvent agentEvent = simulator.agentMap.get(action.agentId);
			agentEvent.assignTo(resourceEvent, time);
			resourceEvent.assignTo(agentEvent);
		}

		if (isOnSameRoad(assignedResource.dropoffLoc, loc) && loc.travelTimeFromStartIntersection <= assignedResource.dropoffLoc.travelTimeFromStartIntersection) {
			long nextEventTime = time + (assignedResource.dropoffLoc.travelTimeFromStartIntersection - loc.travelTimeFromStartIntersection);
			update(nextEventTime, assignedResource.dropoffLoc, State.DROPPING_OFF);
		} else {
			moveToEndIntersection();
		}
	}

	/*
	 * The handler of a drop off event.
	 */
	private void dropOff() throws UnsupportedOperationException {
		startSearchTime = time;
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Dropoff at " + loc, this);

		isPickup = false;
		assignedResource.dropOff(time);

		AgentAction action = fleetManager.onResourceAvailabilityChange(assignedResource.copyResource(), FleetManager.ResourceState.DROPPED_OFF, simulator.agentCopy(loc), time);

		if (!isValidAssignmentAction(action)) {
			assignedResource = null;
			moveToEndIntersection();
			return;
		}


		ResourceEvent resourceEvent = simulator.resMap.get(action.resId);
		if (action.agentId == id) {
			assignedResource = resourceEvent;
			assignedResource.assignTo(this);
			assignTime = time;
			assignLocation = loc;

			if (isOnSameRoad(loc, assignedResource.pickupLoc) && loc.travelTimeFromStartIntersection <= assignedResource.pickupLoc.travelTimeFromStartIntersection) {
				// Reach resource pickup location before reach the end intersection
				long nextEventTime = assignedResource.pickupLoc.travelTimeFromStartIntersection - loc.travelTimeFromStartIntersection + time;
				update(nextEventTime, assignedResource.pickupLoc, State.PICKING_UP);
			} else {
				moveToEndIntersection();
			}
		} else {
			AgentEvent agentEvent = simulator.agentMap.get(action.agentId);
			agentEvent.assignTo(resourceEvent, time);
			resourceEvent.assignTo(agentEvent);

			assignedResource = null;
			moveToEndIntersection();
		}
	}

	private void moveToEndIntersection() {
		long nextEventTime = time + loc.road.travelTime - loc.travelTimeFromStartIntersection;
		LocationOnRoad nextLoc = new LocationOnRoad(loc.road, loc.road.travelTime);
		update(nextEventTime, nextLoc, State.INTERSECTION_REACHED);
	}

	private void update(long time, LocationOnRoad loc, State state) {
		this.time = time;
		this.loc = loc;
		this.state = state;
	}

	private boolean isValidAssignmentAction(AgentAction agentAction) {
		if (agentAction == null) return false;

        long agentId = agentAction.agentId;
        long resId = agentAction.resId;

        AgentEvent agentEvent = simulator.agentMap.get(agentId);
		ResourceEvent resEvent = simulator.resMap.get(resId);

		return agentEvent != null && resEvent != null && !agentEvent.hasResPickup() && agentAction.getType() == AgentAction.Type.ASSIGN;
	}

	private boolean isOnSameRoad(LocationOnRoad loc1, LocationOnRoad loc2) {
		return loc1.road.equals(loc2.road);
	}
}
