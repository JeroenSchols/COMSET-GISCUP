package COMSETsystem;

import java.util.Set;

public abstract class FleetManager {

    protected CityMap map;
    //TODO: find a way to hide traffic pattern from fleet manager
    protected TrafficPattern trafficPattern;

    public enum MapState {
        ROAD_TRAVEL_TIME_CHANGED,
    }

    public enum ResourceState {
        AVAILABLE,
        PICKED_UP,
        DROPPED_OFF,
        EXPIRED
    }

    public abstract void onAgentIntroduced(long agentId, DistanceLocationOnLink currentLoc, long time);

    public abstract void onMapStateChanged(Road road, MapState state);

    public abstract AgentAction onResourceAvailabilityChange(Resource resource, ResourceState state,
                                                             DistanceLocationOnLink currentLoc, long time);

    public abstract Intersection onReachIntersection(long agentId, long time, DistanceLocationOnLink currentLoc);

    public abstract Intersection onReachIntersectionWithResource(long agentId, long time, DistanceLocationOnLink currentLoc,
                                                                 Resource resource);

    public FleetManager(CityMap map) {
        this.map = map;
    }

    public void setTrafficPattern(TrafficPattern trafficPattern) {
        this.trafficPattern = trafficPattern;
    }
}
