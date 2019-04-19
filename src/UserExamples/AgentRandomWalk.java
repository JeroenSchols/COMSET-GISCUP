package UserExamples;

import COMSETsystem.BaseAgent;
import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.LocationOnRoad;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Random walk search algorithm:
 * At each intersection choose a random adjacent intersection to go to.
 */
public class AgentRandomWalk extends BaseAgent {

	// search route stored as a list of intersections.
	LinkedList<Intersection> route = new LinkedList<Intersection>();

	// random number generator
	Random rnd;

	// a static singleton object of a data model, shared by all agents
	static DummyDataModel dataModel = null;

	/**
	 * AgentRandomWalk constructor. 
	 *
	 * @param id An id that is unique among all agents and resources
	 * @param map The map
	 */
	public AgentRandomWalk(long id, CityMap map) {
		super(id, map);
		rnd = new Random(id);
		if (dataModel == null) {
			dataModel = new DummyDataModel(map);
		}
	}

	/**
	 * From all the adjacent intersections randomly choose one to go.
	 * Notice that the method produces a route that contains only one intersection.
	 */

	@Override
	public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {

		String pattern = dataModel.foo(); // Pretend we are using some data model for routing.

		route.clear();
		Intersection currentIntersection = currentLocation.road.to;
		int s = currentIntersection.getAdjacentFrom().size();
		int j = rnd.nextInt(s);
		Intersection nextIntersection = (Intersection) currentIntersection.getAdjacentFrom().toArray()[j];
		route.add(nextIntersection); 
	}

	/**
	 * This method polls the first intersection in the current route and returns this intersection.
	 * 
	 * This method is a callback method which is called when the agent reaches an intersection. The Simulator 
	 * will move the agent to the returned intersection and then call this method again, and so on. 
	 * This is how a planned route (in this case randomly planned) is executed by the Simulator.
	 *
	 * @return Intersection that the Agent is going to move to.
	 */

	@Override
	public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
		if (route.size() != 0) {
			// Route is not empty, take the next intersection.
			Intersection nextIntersection = route.poll();
			return nextIntersection;
		} else {
			// Finished the planned route. Plan a new route.
			planSearchRoute(currentLocation, currentTime);
			return route.poll();
		}		
	}

	/**
	 * A dummy implementation of the assignedTo callback function which does nothing but clearing the current route.
	 * assignedTo is called when the agent is assigned to a resource.
	 */

	@Override
	public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePikcupLocation, LocationOnRoad resourceDropoffLocation) {
		// Clear the current route.
		route.clear();

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Agent " + this.id + " assigned to resource " + resourceId);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "currentLocation = " + currentLocation);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "currentTime = " + currentTime);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "resourcePickupLocation = " + resourcePikcupLocation);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "resourceDropoffLocation = " + resourceDropoffLocation);
	}

}