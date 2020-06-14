package UserExamples;

import COMSETsystem.*;
import org.apache.log4j.jmx.Agent;

import java.util.*;

public class RandomDestinationFleetManager extends FleetManager {
    Map<Long, Long> agentLastAppearTime = new HashMap<>();
    Map<Long, LocationOnRoad> agentLastLocation = new HashMap<>();
    Map<Long, Resource> resourceAssignment = new HashMap<>();
    Set<Resource> waitingResources = new HashSet<>();
    Map<Long, Random> agentRnd = new HashMap<>();


    Map<Long, LinkedList<Intersection>> agentRoutes = new HashMap<>();

    Map<Long, LocationOnRoad> resourceLocation = new HashMap<>();
    Map<Long, Long> agentAssignment = new HashMap<>();
    Set<Long> availableAgent = new TreeSet<>();
    Set<Long> waitingRes = new TreeSet<>();
    Set<Long> pickedUpRes = new TreeSet<>();
    Set<Long> expiredRes = new TreeSet<>();


    @Override
    public void agentsCreated(Set<Long> agentIds) {
//        availableAgent = agentIds;
    }

    @Override
    public AgentAction onAgentIntroduced(long agentId, LocationOnRoad currentLoc, long time) {
        agentLastAppearTime.put(agentId, time);
        agentLastLocation.put(agentId, currentLoc);

        long bestTravelTime = Long.MAX_VALUE;
        Resource bestRes = null;
        for (Resource res : waitingResources) {
            long travelTime = map.travelTimeBetween(currentLoc, res.pickupLoc);

            if (travelTime < bestTravelTime) {
                bestTravelTime = travelTime;
                bestRes = res;
            }
        }

        if (bestRes != null && isReachableBeforeExpiration(bestRes, bestTravelTime + time)) {
            resourceAssignment.put(agentId, bestRes);
            return AgentAction.assignTo(agentId, bestRes.id);
        } else {
            availableAgent.add(agentId);
            return AgentAction.doNothing();
        }
    }

    @Override
    public void onMapStateChanged(Road road, FleetManager.MapState state) {

    }

    @Override
    public AgentAction onResourceAvailabilityChange(Resource resource, ResourceState state, LocationOnRoad currentLoc,
                                                    long time) {
        resourceLocation.put(resource.id, currentLoc);

        AgentAction action = AgentAction.doNothing();

        if (state == ResourceState.AVAILABLE) {
//            Long assignedAgent = getNearestAvailableAgent(currentLoc, time);
            Long assignedAgent = getNearestAvailableAgent(resource, time);
            if (assignedAgent != null) {
                resourceAssignment.put(assignedAgent, resource);
//                agentAssignment.put(assignedAgent, resource.id);
                agentRoutes.put(assignedAgent, new LinkedList<>());
                availableAgent.remove(assignedAgent);
                action = AgentAction.assignTo(assignedAgent, resource.id);
            } else {
                waitingRes.add(resource.id);
            }
        } else if (state == ResourceState.DROPPED_OFF) {
            Resource bestResource =  null;
            long earliest = Long.MAX_VALUE;
            for (Resource res : waitingResources) {
                // If res is in waitingResources, then it must have not expired yet
                // testing null pointer exception
                long travelTime = map.travelTimeBetween(currentLoc, res.pickupLoc);

                // if the resource is reachable before expiration
                long arriveTime = time + travelTime;
                if (arriveTime <= res.expirationTime && arriveTime < earliest) {
                    earliest = arriveTime;
                    bestResource = res;
                }

            }

            if (bestResource != null) {
                action = AgentAction.assignTo(resource.assignedAgentId, bestResource.id);

            } else {
                availableAgent.add(resource.assignedAgentId);
                action = AgentAction.doNothing();
            }
            resourceAssignment.put(resource.assignedAgentId, bestResource);
            agentLastLocation.put(resource.assignedAgentId, currentLoc);

//            if (waitingRes.isEmpty()) {
//                availableAgent.add(resource.assignedAgentId);
//            } else {
//                Long assignedRes = null;
//                for (Long resId : waitingRes) {
//                    assignedRes = resId;
//
//                    break;
//                }
//
//                if (assignedRes != null) {
//                    waitingRes.remove(assignedRes);
//                    agentAssignment.put(resource.assignedAgentId, assignedRes);
//                    agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
//                    action = AgentAction.assignTo(resource.assignedAgentId, assignedRes);
//                }
//            }
        } else if (state == ResourceState.EXPIRED) {
            if (resource.assignedAgentId != -1) {
                agentAssignment.remove(resource.assignedAgentId);
                availableAgent.add(resource.assignedAgentId);
                agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
            } else {
                waitingRes.remove(resource.id);
            }
            expiredRes.add(resource.id);
        } else if (state == ResourceState.PICKED_UP) {
            agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
            pickedUpRes.add(resource.id);
        }

        return action;
    }

    @Override
    public Intersection onReachIntersection(long agentId, long time, LocationOnRoad currentLoc) {
        agentLastAppearTime.put(agentId, time);


        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRoute(agentId, currentLoc);
            agentRoutes.put(agentId, route);
        }

        Intersection nextLocation = route.poll();
        Road nextRoad = currentLoc.road.to.roadTo(nextLocation);
        LocationOnRoad locationOnRoad = new LocationOnRoad(nextRoad, 0);
        agentLastLocation.put(agentId, locationOnRoad);
        return nextLocation;
    }

    @Override
    public Intersection onReachIntersectionWithResource(long agentId, long time, LocationOnRoad currentLoc, Resource resource) {
        agentLastAppearTime.put(agentId, time);
        agentLastLocation.put(agentId, currentLoc);

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRouteToTarget(resource.pickupLoc, resource.dropOffLoc);
            agentRoutes.put(agentId, route);
        }

        return route.poll();
    }

    private boolean isReachableBeforeExpiration(Resource resource, long travelTime) {
        long tripTime = map.travelTimeBetween(resource.pickupLoc, resource.dropOffLoc);
        return tripTime + travelTime <= resource.expirationTime;
    }

    Long getNearestAvailableAgent(Resource resource, long time) {
        long earliest = Long.MAX_VALUE;
        Long bestAgent = null;
        for (Long id : availableAgent) {
            if (!agentLastLocation.containsKey(id)) continue;

            long elapseTime = time - agentLastAppearTime.get(id);
            LocationOnRoad locationOnRoad = agentLastLocation.get(id);
            LocationOnRoad curLoc = new LocationOnRoad(locationOnRoad.road, locationOnRoad.travelTimeFromStartIntersection + elapseTime);

            long travelTime = map.travelTimeBetween(curLoc, resource.pickupLoc);
            long arriveTime = travelTime + time;
            if (arriveTime < earliest) {
                bestAgent = id;
                earliest = arriveTime;
            }
        }

        return bestAgent;
    }

    Long getNearestAvailableAgent(LocationOnRoad resourceLocation, long time) {
        long earliest = Long.MAX_VALUE;
        Long bestAgent = null;
        for (Long id : availableAgent) {
            if (!agentLastLocation.containsKey(id)) continue;

            long elapseTime = time - agentLastAppearTime.get(id);
            long travelTime = map.travelTimeBetween(agentLastLocation.get(id), resourceLocation) - elapseTime;
            long arriveTime = travelTime + time;
            if (arriveTime < earliest) {
                bestAgent = id;
                earliest = arriveTime;
            }
        }
        return bestAgent;
    }

    LinkedList<Intersection> planRoute(long agentId, LocationOnRoad currentLocation) {
        Resource assignedRes = resourceAssignment.get(agentId);
        if (assignedRes != null) {


            Intersection sourceIntersection = currentLocation.road.to;
            Intersection destinationIntersection = assignedRes.pickupLoc.road.from;
            LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                    destinationIntersection);
            if (shortestTravelTimePath.size() == 1) {
                return getRandomRoute(agentId, currentLocation);
            } else {
                shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
                return shortestTravelTimePath;
            }
        } else {
            return getRandomRoute(agentId, currentLocation);
        }
    }

    private LinkedList<Intersection> planRouteToTarget(LocationOnRoad source, LocationOnRoad destination) {
        Intersection sourceIntersection = source.road.to;
        Intersection destinationIntersection = destination.road.from;
        LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                destinationIntersection);
        shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
        return shortestTravelTimePath;
    }

    private LinkedList<Intersection> getRandomRoute(long agentId, LocationOnRoad currentLocation) {
        Random random = agentRnd.getOrDefault(agentId, new Random(agentId));
        agentRnd.put(agentId, random);

        Intersection sourceIntersection = currentLocation.road.to;
        int destinationIndex = random.nextInt(map.intersections().size());
        Intersection[] intersectionArray =
                map.intersections().values().toArray(new Intersection[map.intersections().size()]);
        Intersection destinationIntersection = intersectionArray[destinationIndex];
        if (destinationIntersection == sourceIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom =
                    sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
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
