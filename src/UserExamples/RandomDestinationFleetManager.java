package UserExamples;

import COMSETsystem.*;

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
    public void onResourceAvailabilityChange(long resourceId, ResourceState state, long agentId,
                                             LocationOnRoad location, long time, long expiredTime) {
        resourceLocation.put(resourceId, location);
        if (state == ResourceState.WAITING) {
            Long assignedAgent = getNearestAvailableAgent(location, time);
            if (assignedAgent != null) {
                agentAssignment.put(assignedAgent, resourceId);
                agentRoutes.put(assignedAgent, new LinkedList<>());
                availableAgent.remove(assignedAgent);
            } else {
                waitingRes.add(resourceId);

            }
        } else if (state == ResourceState.DROPPED_OFF) {
            if (waitingRes.isEmpty()) {
                availableAgent.add(agentId);
            } else {
                Long assignedRes = null;
                for (Long resId : waitingRes) {
                    assignedRes = resId;
                    agentAssignment.put(agentId, resId);
                    agentRoutes.put(agentId, new LinkedList<>());
                    break;
                }

                if (assignedRes != null) {
                    waitingRes.remove(assignedRes);
                }
            }
        } else if (state == ResourceState.EXPIRED) {
            if (agentId != -1) {
                agentAssignment.remove(agentId);
                availableAgent.add(agentId);
                agentRoutes.put(agentId, new LinkedList<>());
            } else {
                waitingRes.remove(resourceId);
            }
            expiredRes.add(resourceId);
        } else if (state == ResourceState.PICKED_UP) {
            agentRoutes.put(agentId, new LinkedList<>());
            pickedUpRes.add(resourceId);
        }
    }

    @Override
    public Intersection onReachIntersection(long agentId, long time, LocationOnRoad road) {
        agentLocation.put(agentId, road);
        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRoute(agentId, road);
            agentRoutes.put(agentId, route);
        }

        Intersection nextIntersection = route.poll();
        return nextIntersection;
    }

    @Override
    public Intersection onReachIntersectionWithResource(long agentId, long resourceId, long time, LocationOnRoad road) {
        agentLocation.put(agentId, road);

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRouteToTarget(resourceId, road);
            agentRoutes.put(agentId, route);
        }

        Intersection nextIntersection = route.poll();
        return nextIntersection;
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

    private LinkedList<Intersection> planRouteToTarget(long resId, LocationOnRoad currentLocation) {
        LocationOnRoad resLocation = resourceLocation.get(resId);
        Intersection sourceIntersection = currentLocation.road.to;
        Intersection destinationIntersection = resLocation.road.from;
        LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                destinationIntersection);
        shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
        return shortestTravelTimePath;
    }

    private LinkedList<Intersection> getRandomRoute(long agentId, LocationOnRoad currentLocation) {
        Random random = new Random(agentId);

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
