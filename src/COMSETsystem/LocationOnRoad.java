package COMSETsystem;

public class LocationOnRoad {
    public Road road;
    public double distanceFromStartIntersection;

    public LocationOnRoad(Road road, double distanceFromStartIntersection) {
        this.road = road;
        this.distanceFromStartIntersection = distanceFromStartIntersection;
    }

    public boolean upstreamTo(LocationOnRoad aLoc) {
        assert this.road.id == aLoc.road.id : "two links must be on the same road";
        return this.distanceFromStartIntersection <= aLoc.distanceFromStartIntersection;
    }

    public boolean atEndIntersection() {
        return this.distanceFromStartIntersection == this.road.length;
    }

    // create location at the end of a road
    public static LocationOnRoad createFromRoadEnd(Road road) {
        return new LocationOnRoad(road, road.length);
    }

    // create location at the start of a road
    public static LocationOnRoad createFromRoadStart(Road road) {
        return new LocationOnRoad(road, 0);
    }

    public String toString() {
        return "(road: " + road.id + ", distanceFromStartIntersection: " + distanceFromStartIntersection;
    }
}
