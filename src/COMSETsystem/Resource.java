package COMSETsystem;

public class Resource {
    public long id;
    public long expirationTime;
    public long assignedAgentId;
    public DistanceLocationOnLink pickupLoc;
    public DistanceLocationOnLink dropOffLoc;

    public Resource(long id, long expirationTime, long assignedAgentId, DistanceLocationOnLink pickupLoc,
                    DistanceLocationOnLink dropOffLoc) {
        this.id = id;
        this.expirationTime = expirationTime;
        this.assignedAgentId = assignedAgentId;
        this.pickupLoc = pickupLoc;
        this.dropOffLoc = dropOffLoc;
    }
}
