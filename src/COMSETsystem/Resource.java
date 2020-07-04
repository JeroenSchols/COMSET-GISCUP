package COMSETsystem;

public class Resource {
    public final long id;
    public final long expirationTime;
    public final long assignedAgentId;
    public LocationOnRoad pickupLoc;
    public LocationOnRoad dropOffLoc;

    public Resource(long id, long expirationTime, long assignedAgentId, LocationOnRoad pickupLoc,
                    LocationOnRoad dropOffLoc) {
        this.id = id;
        this.expirationTime = expirationTime;
        this.assignedAgentId = assignedAgentId;
        this.pickupLoc = pickupLoc;
        this.dropOffLoc = dropOffLoc;
    }
}
