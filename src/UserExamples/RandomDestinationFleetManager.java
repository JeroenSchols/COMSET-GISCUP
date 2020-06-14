package UserExamples;

import COMSETsystem.*;
import org.apache.log4j.jmx.Agent;

import java.util.*;

public class RandomDestinationFleetManager extends FleetManager {
    Map<Long, LinkedList<Intersection>> agentRoutes = new HashMap<>();
    Map<Long, LocationOnRoad> agentLocation = new HashMap<>();
    Map<Long, LocationOnRoad> resourceLocation = new HashMap<>();
    Map<Long, Long> agentAssignment = new HashMap<>();
    Set<Long> availableAgent;
    Set<Long> waitingRes = new TreeSet<>();
    Set<Long> pickedUpRes = new TreeSet<>();
    Set<Long> expiredRes = new TreeSet<>();

    @Override
    public void agentsCreated(Set<Long> agentIds) {
        availableAgent = agentIds;
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
            Long assignedAgent = getNearestAvailableAgent(currentLoc, time);
            if (assignedAgent != null) {
                agentAssignment.put(assignedAgent, resource.id);
                agentRoutes.put(assignedAgent, new LinkedList<>());
                availableAgent.remove(assignedAgent);
                action = AgentAction.assignTo(assignedAgent, resource.id);
            } else {
                waitingRes.add(resource.id);
            }
        } else if (state == ResourceState.DROPPED_OFF) {
            if (waitingRes.isEmpty()) {
                availableAgent.add(resource.assignedAgentId);
            } else {
                Long assignedRes = null;
                for (Long resId : waitingRes) {
                    assignedRes = resId;

                    break;
                }

                if (assignedRes != null) {
                    waitingRes.remove(assignedRes);
                    agentAssignment.put(resource.assignedAgentId, assignedRes);
                    agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
                    action = AgentAction.assignTo(resource.assignedAgentId, assignedRes);
                }
            }
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
        agentLocation.put(agentId, currentLoc);
        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRoute(agentId, currentLoc);
            agentRoutes.put(agentId, route);
        }

        return route.poll();
    }

    @Override
    public Intersection onReachIntersectionWithResource(long agentId, long time, LocationOnRoad currentLoc,
                                                        Resource resource) {
        agentLocation.put(agentId, currentLoc);

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRouteToTarget(resource.pickupLoc, resource.dropOffLoc);
            agentRoutes.put(agentId, route);
        }

        return route.poll();
    }

    Long getNearestAvailableAgent(LocationOnRoad resourceLocation, long time) {
        long earliest = Long.MAX_VALUE;
        Long bestAgent = null;
        for (Long id : availableAgent) {
            if (!agentLocation.containsKey(id)) continue;

            long travelTime = map.travelTimeBetween(agentLocation.get(id), resourceLocation);
            long arriveTime = travelTime + time;
            if (arriveTime < earliest) {
                bestAgent = id;
                earliest = arriveTime;
            }
        }
        return bestAgent;
    }

    LinkedList<Intersection> planRoute(long agentId, LocationOnRoad currentLocation) {
        Long assignedRes = agentAssignment.get(agentId);
        if (assignedRes != null && !pickedUpRes.contains(assignedRes) && !expiredRes.contains(assignedRes)) {
            LocationOnRoad resLocation = resourceLocation.get(assignedRes);
            Intersection sourceIntersection = currentLocation.road.to;
            Intersection destinationIntersection = resLocation.road.from;
            LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                    destinationIntersection);
            if (shortestTravelTimePath.size() == 1) {
                return getRandomRoute(agentId, currentLocation);
            } else {
                shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
                return shortestTravelTimePath;
            }
        } else {
            agentAssignment.remove(agentId);
            return getRandomRoute(agentId, currentLocation);
        }
    }

    LinkedList<Intersection> planRouteToTarget(LocationOnRoad source, LocationOnRoad destination) {
        Intersection sourceIntersection = source.road.to;
        Intersection destinationIntersection = destination.road.from;
        LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                destinationIntersection);
        shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
        return shortestTravelTimePath;
    }

    LinkedList<Intersection> getRandomRoute(long agentId, LocationOnRoad currentLocation) {
        Random random = new Random(agentId);

        Intersection sourceIntersection = currentLocation.road.to;
        int destinationIndex = random.nextInt(map.intersections().size());
        Intersection[] intersectionArray =
                map.intersections().values().toArray(new Intersection[0]);
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
