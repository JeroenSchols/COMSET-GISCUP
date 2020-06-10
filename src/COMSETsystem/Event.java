package COMSETsystem;

/**
 *
 * @author TijanaKlimovic
 *
 * The Event class represents an event which is something that is happening in
 * the simulation. In this case we have agent events and resource events which
 * will be described in their respective class. An event most importantly has
 * time. This is when this event will happen, and thus triggered.
 */
public abstract class Event implements Comparable<Event> {

	private static int maxId = 0;

	// The time at which the event is to be triggered
	long time;

	// A reference to the Simulator
	Simulator simulator;

	FleetManager fleetManager;

	/* An id that is unique among all events regardless of whether agent or resource.
	 * To facilitate solving ties of trigger time.
	 */
	long id;  

	/**
	 * Constructor for class Event
	 *
	 * @param time core to this class, indicates when this event will trigger.
	 * @param simulator a reference to simulator
	 */
	Event(long time, Simulator simulator, FleetManager fleetManager) {
		this.id = maxId++;
		this.time = time;
		this.simulator = simulator;
		this.fleetManager = fleetManager;
	}

	/**
	 * Constructor for class Event.  Allow subclasses to set simulator.
	 *
	 * @param time core to this class, indicates when this event will trigger.
	 */
	Event(long time) {
		this.id = maxId++;
		this.time = time;
	}

	/**
	 * Function called when the Event needs to be executed.
	 *
	 * @return new Event if needed
	 */
	abstract Event trigger() throws Exception;

	public long getId() {
		return id;
	}

	/**
	 * To be used by the PriorityQueue to order the Events
	 *
	 * @param o the event being compared to this one
	 * @return -1, 0, or 1 according to whether the value of expression is
	 * negative, zero or positive.
	 */
	@Override
	public int compareTo(Event o) {
		if (this.time < o.time)
			return -1;
		else if (this.time > o.time)
			return 1;
		else if (this.id < o.id) // tie on time; compare id
			return -1;
		else if (this.id > o.id)
			return 1;
		else {
			System.out.println("Duplicate event exception");
			System.exit(1);
			return 0;
		}
	}
}


