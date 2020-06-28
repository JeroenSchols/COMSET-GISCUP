package UserExamples;

import COMSETsystem.*;

import java.util.*;

public class RandomDestinationFleetManager extends FleetManager {
    private Map<Long, Long> agentLastAppearTime = new HashMap<>();
    private Map<Long, DistanceLocationOnLink> agentLastLocation = new HashMap<>();
    private Map<Long, Resource> resourceAssignment = new HashMap<>();
    private Set<Resource> waitingResources = new TreeSet<>(Comparator.comparingLong((Resource r) -> r.id));
    private Set<Long> availableAgent = new TreeSet<>(Comparator.comparingLong((Long id) -> id));
    private Map<Long, Random> agentRnd = new HashMap<>();


    Map<Long, LinkedList<Intersection>> agentRoutes = new HashMap<>();



    @Override
    public void onAgentIntroduced(long agentId, DistanceLocationOnLink currentLoc, long time) {
        agentLastAppearTime.put(agentId, time);
        agentLastLocation.put(agentId, currentLoc);
        availableAgent.add(agentId);
    }

    @Override
    public void onMapStateChanged(Road road, FleetManager.MapState state) {

    }

    @Override
    public AgentAction onResourceAvailabilityChange(Resource resource,
                                                    ResourceState state,
                                                    DistanceLocationOnLink currentLoc,
                                                    long time) {

        AgentAction action = AgentAction.doNothing();

        if (state == ResourceState.AVAILABLE) {
            Long assignedAgent = getNearestAvailableAgent(resource, time);
            if (assignedAgent != null) {
                resourceAssignment.put(assignedAgent, resource);
                agentRoutes.put(assignedAgent, new LinkedList<>());
                availableAgent.remove(assignedAgent);
                action = AgentAction.assignTo(assignedAgent, resource.id);
            } else {
                waitingResources.add(resource);
            }
        } else if (state == ResourceState.DROPPED_OFF) {
            Resource bestResource =  null;
            long earliest = Long.MAX_VALUE;
            for (Resource res : waitingResources) {
                // If res is in waitingResources, then it must have not expired yet
                // testing null pointer exception

                long travelTime = map.travelTimeBetween(currentLoc, res.pickupLoc);
                // multiple by 4 so we get similar travel time as last year's version. for test only.
                // TODO: remove this statement and uncomment the above
                //long travelTime = map.travelTimeBetween(currentLoc, res.pickupLoc)*4;

                // if the resource is reachable before expiration
                long arriveTime = time + travelTime;
                if (arriveTime <= res.expirationTime && arriveTime < earliest) {
                    earliest = arriveTime;
                    bestResource = res;
                }

            }

            if (bestResource != null) {
                waitingResources.remove(bestResource);
                action = AgentAction.assignTo(resource.assignedAgentId, bestResource.id);
            } else {
                availableAgent.add(resource.assignedAgentId);
                action = AgentAction.doNothing();
            }
            resourceAssignment.put(resource.assignedAgentId, bestResource);
            agentLastLocation.put(resource.assignedAgentId, currentLoc);
            agentLastAppearTime.put(resource.assignedAgentId, time);
        } else if (state == ResourceState.EXPIRED) {
            waitingResources.remove(resource);
        } else if (state == ResourceState.PICKED_UP) {
            agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
        }

        return action;
    }

    @Override
    public Intersection onReachIntersection(long agentId, long time, DistanceLocationOnLink currentLoc) {
        if (agentId == 240902L && time == 1464800008L) {
            System.out.println("here");
        }
        agentLastAppearTime.put(agentId, time);

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRoute(agentId, currentLoc);
            agentRoutes.put(agentId, route);
        }

        Intersection nextLocation = route.poll();
        Road nextRoad = currentLoc.link.road.to.roadTo(nextLocation);
        DistanceLocationOnLink locationOnRoad = new DistanceLocationOnLink(nextRoad.links.get(0), 0);
        agentLastLocation.put(agentId, locationOnRoad);
        return nextLocation;
    }

    @Override
    public Intersection onReachIntersectionWithResource(long agentId, long time, DistanceLocationOnLink currentLoc,
                                                        Resource resource) {
        agentLastAppearTime.put(agentId, time);

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRouteToTarget(resource.pickupLoc, resource.dropOffLoc);
            agentRoutes.put(agentId, route);
        }

        Intersection nextLocation = route.poll();
        Road nextRoad = currentLoc.link.road.to.roadTo(nextLocation);
        DistanceLocationOnLink locationOnRoad = new DistanceLocationOnLink(nextRoad.links.get(0), 0);
        agentLastLocation.put(agentId, locationOnRoad);
        return nextLocation;
    }

    Long getNearestAvailableAgent(Resource resource, long time) {
        long earliest = Long.MAX_VALUE;
        Long bestAgent = null;
        for (Long id : availableAgent) {
            if (!agentLastLocation.containsKey(id)) continue;

            long elapseTime = time - agentLastAppearTime.get(id);
            DistanceLocationOnLink locationOnRoad = agentLastLocation.get(id);
            DistanceLocationOnLink curLoc = trafficPattern.travelRoadForTime(agentLastAppearTime.get(id), locationOnRoad, elapseTime);

            long travelTime = map.travelTimeBetween(curLoc, resource.pickupLoc);
            // multiple by 4 so we get similar travel time as last year's version. for test only.
            // TODO: remove this statement and uncomment the above
            //long travelTime = map.travelTimeBetween(curLoc, resource.pickupLoc)*4;
            long arriveTime = travelTime + time;
            if (arriveTime < earliest) {
                bestAgent = id;
                earliest = arriveTime;
            }
        }

        if (earliest <= resource.expirationTime) {
            return bestAgent;
        } else {
            return null;
        }
    }

    LinkedList<Intersection> planRoute(long agentId, DistanceLocationOnLink currentLocation) {
        Resource assignedRes = resourceAssignment.get(agentId);

        if (assignedRes != null) {
            Intersection sourceIntersection = currentLocation.link.road.to;
            Intersection destinationIntersection = assignedRes.pickupLoc.link.road.from;
            LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                    destinationIntersection);
            shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
            return shortestTravelTimePath;
        } else {
            return getRandomRoute(agentId, currentLocation);
        }
    }

    LinkedList<Intersection> planRouteToTarget(DistanceLocationOnLink source, DistanceLocationOnLink destination) {
        Intersection sourceIntersection = source.link.road.to;
        Intersection destinationIntersection = destination.link.road.from;
        LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                destinationIntersection);
        shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
        return shortestTravelTimePath;
    }

    LinkedList<Intersection> getRandomRoute(long agentId, DistanceLocationOnLink currentLocation) {
        Random random = agentRnd.getOrDefault(agentId, new Random(agentId));
        agentRnd.put(agentId, random);

        Intersection sourceIntersection = currentLocation.link.road.to;
        int destinationIndex = random.nextInt(map.intersections().size());
        Intersection[] intersectionArray =
                map.intersections().values().toArray(new Intersection[0]);
        for (int i = 0; i < intersectionArray.length; i++)
            for (int j = i + 1; j < intersectionArray.length; j++) {
                if (intersectionArray[i].id == intersectionArray[j].id) {
                    System.out.println("something is wrong");
                }
            }
        Intersection destinationIntersection = intersectionArray[destinationIndex];
        if (destinationIntersection == sourceIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom =
                    sourceIntersection.roadsMapFrom.values().toArray(new Road[0]);
            destinationIntersection = roadsFrom[0].to;
        }
        LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                destinationIntersection);
        shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
        return shortestTravelTimePath;
    }

    public RandomDestinationFleetManager(CityMap map) {
        super(map);
    }
}
