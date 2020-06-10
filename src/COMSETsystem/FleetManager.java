package COMSETsystem;

import java.util.Set;

public abstract class FleetManager {

    protected CityMap map;

    public enum MapState {
        ROAD_TRAVEL_TIME_CHANGED,
    }

    public enum ResourceState {
        WAITING,
        PICKED_UP,
        DROPPED_OFF,
        EXPIRED
    }

    public abstract void agentsCreated(Set<Long> agentIds);

    public abstract void onMapStateChanged(Road road, MapState state);

    public abstract void onResourceAvailabilityChange(long resourceId, ResourceState state, long agentId, LocationOnRoad location, long time, long expiredTime);

    public abstract Intersection onReachIntersection(long agentId, long time, LocationOnRoad location);

    public abstract Intersection onReachIntersectionWithResource(long agentId, long resourceId, long time, LocationOnRoad road);

    public FleetManager(CityMap map) {
        this.map = map;
    }
}
