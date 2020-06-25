package DataParsing;

import COMSETsystem.DistanceLocationOnLink;

/**
 *
 * @author TijanaKlimovic
 */
public class Resource extends TimestampAbstract {

	private double dropffLat; // drop-off latitude of resource
	private double dropffLon; // drop-off longitude of resource

	private DistanceLocationOnLink pickupLocation; // pickup location on road
	private DistanceLocationOnLink dropffLocation; // dropff location on road
	private long dropffTime; // drop-off time of resource from raw data

	//constructor initiating each field of the Resource record type
	public Resource(double pickupLat, double pickupLon, double dropffLat, double dropffLon, long time, long dropffTime) {
		super(pickupLat, pickupLon, time);
		this.dropffLat = dropffLat;
		this.dropffLon = dropffLon;
		this.dropffTime = dropffTime;

	}

	/**
	 * returns the dropff latitude of the resource
	 * 
	 * @return {@code this.dropffLat}
	 */
	public double getDropoffLat() {
		return dropffLat;
	}

	/**
	 * returns the dropff longitude of the resource
	 * 
	 * @return {@code this.dropffon}
	 */
	public double getDropoffLon() {
		return dropffLon;
	}

	public void setPickupLocation(DistanceLocationOnLink pickupLocation) { this.pickupLocation = pickupLocation; }

	public void setDropoffLocation(DistanceLocationOnLink dropffLocation) { this.dropffLocation = dropffLocation; }

	public DistanceLocationOnLink getPickupLocation() { return pickupLocation; }

	public DistanceLocationOnLink getDropoffLocation() { return dropffLocation; }

	public long getPickupTime() { return this.getTime(); }

	public long getDropoffTime() { return dropffTime; }
}
