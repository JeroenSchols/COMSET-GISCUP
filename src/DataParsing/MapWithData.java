package DataParsing;

import COMSETsystem.*;

import java.time.ZoneId;
import java.util.*;

/**
 * The MapWithData class is responsible for loading a resource dataset file,
 * map matching resources, and create a list of resource events.  
 */
public class MapWithData {

	// Map without any data added to it
	public CityMap map;     

	// Full path of the file containing the resources to be loaded to the simulator
	private String resourceFile;    

	// Priority queue of events
	public PriorityQueue<Event> events;

	// The earliest resource introduction time. The time is used to determine the time at which
	// agents are to be deployed. The agents are to be deployed at time earliestResourceTime - 1.
	public long earliestResourceTime = Long.MAX_VALUE;

	// The latest possible time that a resource is dropped off. The time is used to determine the time at which
	// the simulation is to stop.
	public long latestResourceTime = -1; 
	
	// Seed for the random number generator when placing agents.
	public long agentPlacementRandomSeed;
	
	// Time Zone ID of the map; for conversion from the time stamps in a resource dataset file to Linux epochs.
	protected ZoneId zoneId;

	private ArrayList<Resource> resourcesParsed;

	/**
	 * Constructor of MapWithData
	 * @param map reference to the map
	 * @param resourceFile full path to the resource file
	 * @param agentPlacementRandomSeed Seed for randome number that generates agent placements
	 */
	public MapWithData(CityMap map, String resourceFile, long agentPlacementRandomSeed) {
		this.map = map;
		this.resourceFile = resourceFile;
		this.agentPlacementRandomSeed = agentPlacementRandomSeed;
		events = new PriorityQueue<>();
		zoneId = map.computeZoneId();
	}

	/**
	 * Maps each agent and each resource onto the nearest location on the map
	 * according to the agent/resource's longitude and latitude. Creates resource events 
	 * for each passenger record obtained from the resource file and adds them to the events
	 * priority queue.
	 *
	 * @param simulator Simulator object with whose methods agent and resource events can
	 * be created.
	 * @return long the latest resource time
	 */
	// FIXME: Pass in configuration here too instead of accessing it with the singleton.
	public long createMapWithData(Simulator simulator, FleetManager fleetManager) {


		CSVNewYorkParser parser = new CSVNewYorkParser(resourceFile, zoneId);
		resourcesParsed = parser.parse(Configuration.get().timeResolution);
		try {
            for (Resource resource : resourcesParsed) {
				// map matching
				LocationOnRoad pickupMatch = mapMatch(resource.getPickupLon(), resource.getPickupLat());
				LocationOnRoad dropoffMatch = mapMatch(resource.getDropoffLon(), resource.getDropoffLat());

				// TODO: won't need trip time
				long staticTripTime = simulator.mapForAgents.travelTimeBetween(pickupMatch, dropoffMatch);

				resource.setPickupLocation(pickupMatch);
				resource.setDropoffLocation(dropoffMatch);

				ResourceEvent ev = new ResourceEvent(pickupMatch, dropoffMatch, resource.getTime(), staticTripTime,
						simulator, fleetManager);
				events.add(ev);

				//  track earliestResourceTime and latestResourceTime
				if (resource.getTime() < earliestResourceTime) {
					earliestResourceTime = resource.getTime();
				}


				final long resourceMaximumLifeTime = Configuration.get().resourceMaximumLifeTime;
				if (resource.getTime() + resourceMaximumLifeTime +ev.staticTripTime > latestResourceTime) {
					latestResourceTime = resource.getTime() + resourceMaximumLifeTime + ev.staticTripTime;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return latestResourceTime;
	}

	public TrafficPattern getTrafficPattern() {
		System.out.println("Building traffic patterns...");
		return buildSlidingTrafficPattern(resourcesParsed,
				Configuration.get().trafficPatternEpoch,
				Configuration.get().trafficPatternStep, Configuration.get().dynamicTrafficEnabled);
	}

	/**
	 * Match a point to the closest location on the map
	 */
	public LocationOnRoad mapMatch(double longitude, double latitude) {
		Link link = map.getNearestLink(longitude, latitude);
		double [] xy = map.projector().fromLatLon(latitude, longitude);
		double [] snapResult = snap(link.from.getX(), link.from.getY(), link.to.getX(), link.to.getY(), xy[0], xy[1]);
		double distanceFromStartVertex = this.distance(snapResult[0], snapResult[1], link.from.getX(), link.from.getY());

		// find the begin distance of link
		double distanceFromStartIntersection = 0;
		for (Link aLink : link.road.links) {
			if (aLink.id == link.id) {
				distanceFromStartIntersection += distanceFromStartVertex;
				break;
			} else {
				distanceFromStartIntersection += aLink.length;
			}
		}
		return new LocationOnRoad(link.road, distanceFromStartIntersection);
	}

	/**
	 * Find the closest point on a line segment with end points (x1, y1) and
	 * (x2, y2) to a point (x ,y), a procedure called snap.
	 *
	 * @param x1 x-coordinate of an end point of the line segment
	 * @param y1 y-coordinate of an end point of the line segment
	 * @param x2 x-coordinate of another end point of the line segment
	 * @param y2 y-coordinate of another end point of the line segment
	 * @param x x-coordinate of the point to snap
	 * @param y y-coordinate of the point to snap
	 *
	 * @return the closest point on the line segment.
	 */
	public double[] snap(double x1, double y1, double x2, double y2, double x, double y) {
		double[] snapResult = new double[3];
		double dist;
		double length = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);

		if (length == 0.0) {
			dist = this.distance(x1, y1, x, y);
			snapResult[0] = x1;
			snapResult[1] = y1;
			snapResult[2] = dist;
		} else {
			double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / length;
			if (t < 0.0) {
				dist = distance(x1, y1, x, y);
				snapResult[0] = x1;
				snapResult[1] = y1;
				snapResult[2] = dist;
			} else if (t > 1.0) {
				dist = distance(x2, y2, x, y);
				snapResult[0] = x2;
				snapResult[1] = y2;
				snapResult[2] = dist;
			} else {
				double proj_x = x1 + t * (x2 - x1);
				double proj_y = y1 + t * (y2 - y1);
				dist = distance(proj_x, proj_y, x, y);
				snapResult[0] = proj_x;
				snapResult[1] = proj_y;
				snapResult[2] = dist;
			}
		}
		return snapResult;
	}

	/**
	 * Compute the Euclidean distance between point (x1, y1) and point (x2, y2).
	 */
	public double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	/**
	 * Creates agent events that are randomly placed on map.
	 *
	 * @param simulator a reference to the simulator object
	 */
	public void placeAgentsRandomly(Simulator simulator, FleetManager fleetManager) {
		long deployTime = earliestResourceTime - 1;

		Random generator = new Random(agentPlacementRandomSeed);
		for (int i = 0; i < Configuration.get().numberOfAgents; i++) {
			int road_id = generator.nextInt(map.roads().size());
			Road road = map.roads().get(road_id);
//			long travelTimeFromStartIntersection;
//			if (road.travelTime != 0L) {
//				travelTimeFromStartIntersection = generator.nextInt((int) road.travelTime);
//			} else {
//				travelTimeFromStartIntersection = 0L;
//			}

			//Link link = road.links.get(generator.nextInt(road.links.size()));
			//double distanceFromStartVertex = generator.nextDouble() * link.length;
			//LocationOnRoad locationOnRoad = LocationOnRoad.createFromRoadStart(road);
			double distanceFromStartIntersection = generator.nextDouble() * road.length;
			LocationOnRoad locationOnRoad = new LocationOnRoad(road, distanceFromStartIntersection);
			//DistanceLocationOnLink locationOnRoad = new DistanceLocationOnLink(link, distanceFromStartVertex);

			AgentEvent ev = new AgentEvent(locationOnRoad, deployTime, simulator, fleetManager);
			simulator.addEmptyAgent(ev);
			events.add(ev);
		}
	}

//	/**
//	 * Creates agent events that are randomly placed on map.
//	 *
//	 * @param simulator a reference to the simulator object
//	 */
//	public ArrayList<BaseAgent> placeAgentsRandomly(Simulator simulator) {
//		ArrayList<BaseAgent> agents = new ArrayList<BaseAgent>();
//		long deployTime = earliestResourceTime - 1;
//
//		Random generator = new Random(agentPlacementRandomSeed);
//		for (int i = 0; i < simulator.totalAgents(); i++) {
//			Road road = map.roads().get(generator.nextInt(map.roads().size()));
//            long travelTimeFromStartIntersection;
//            if (road.travelTime != 0) {
//                travelTimeFromStartIntersection = (long) (generator.nextInt((int) road.travelTime));
//            } else {
//                travelTimeFromStartIntersection = 0L;
//            }
//			LocationOnRoad locationOnRoad = new LocationOnRoad(road, travelTimeFromStartIntersection);
//			AgentEvent ev = new AgentEvent(locationOnRoad, deployTime, simulator);
//			simulator.addEmptyAgent(ev);
//			events.add(ev);
//			agents.add(ev.agent);
//		}
//		return agents;
//	}

	/**
	 * 
	 * @return events
	 */
	public PriorityQueue<Event> getEvents() {
		return events;
	}

	public TrafficPattern buildSlidingTrafficPattern(ArrayList<Resource> resources, long epoch, long step,
													 boolean dynamicTraffic) {
		// sort resources by pickup

		resources.sort(Comparator.comparingLong(TimestampAbstract::getTime));
		TrafficPattern trafficPattern = new TrafficPattern(epoch, step);
		long epochBeginTime = resources.get(0).getPickupTime();
		int beginResourceIndex = 0;
		while (true) {
			ArrayList<Resource> epochResources = new ArrayList<>();
			long epochEndTime = epochBeginTime + epoch;
			int resourceIndex = beginResourceIndex;
			while (resourceIndex < resources.size() && resources.get(resourceIndex).getPickupTime() < epochEndTime) {
				if (resources.get(resourceIndex).getDropoffTime() < epochEndTime) {
					epochResources.add(resources.get(resourceIndex));
				}
				resourceIndex += 1;
			}
			if (resourceIndex == resources.size()) {
				break;
			} else {
				if (dynamicTraffic) {
					double speedFactor = getSpeedFactor(epochResources);
					trafficPattern.addTrafficPatternItem(epochBeginTime, speedFactor);
				} else {
					trafficPattern.addTrafficPatternItem(epochBeginTime, 1.0);
				}
				//System.out.println(epochResources.size()+","+speedFactor);
				epochBeginTime += step;
				while (beginResourceIndex < resources.size() && resources.get(beginResourceIndex).getPickupTime() < epochBeginTime) {
					beginResourceIndex += 1;
				}
			}
		}
		//trafficPattern.printOut();
		trafficPattern.setTrafficPatternArray();
		return trafficPattern;
	}

	public double getSpeedFactor(ArrayList<Resource> resources) {
		long totalActualTravelTime = 0;
		long totalSimulatedTravelTime = 0;
		for (Resource r : resources) {
			long pickupTime = r.getPickupTime();
			long dropoffTime = r.getDropoffTime();
			long actualTravelTime = dropoffTime - pickupTime;

			long simulatedTravelTime = map.travelTimeBetween(r.getPickupLocation(), r.getDropoffLocation());

			totalActualTravelTime += actualTravelTime;
			totalSimulatedTravelTime += simulatedTravelTime;
		}
		return ((double)totalSimulatedTravelTime) / totalActualTravelTime;
	}
}
