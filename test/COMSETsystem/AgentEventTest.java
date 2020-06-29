package COMSETsystem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.TreeSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentEventTest {

    public static final int CUSTOMER_TRIP_TIME = 500;
    public static final long TIME_TO_PICKUP_CUSTOMER = 300L;
    public static final int TRIGGER_TIME = 100;
    public static final int AVAILABLE_TIME = 50;
    @Mock
    Simulator mockSimulator;
    @Mock
    Event mockEvent;
    @Mock
    ResourceEvent mockResourceEvent;
    @Mock
    LocationOnRoad mockLocationOnRoad;
    @Mock
    Road mockRoad;
    @Mock
    Simulator.PickUp mockNoPickUp;
    @Mock
    FleetManager mockFleetManager;

    private SimpleMap testMap = new SimpleMap();

    @Before
    public void BeforeEachTest() {
    }

    /**
     * Tests cruising this path intersection 1 -> intersection 2 -> intersection 3.
     *
     * @throws Exception from spyEvent.Trigger if any
     */
    @Test
    public void testTrigger_cruising() throws Exception {
        long initTimeFromStartIntersection = testMap.roadFrom1to2.travelTime / 2;
        // Construction agent reaching intersection2
        LocationOnRoad locAtMiddleOfRoad = new LocationOnRoad(testMap.roadFrom1to2, initTimeFromStartIntersection);

        AgentEvent agentEvent = new AgentEvent(locAtMiddleOfRoad, TRIGGER_TIME, mockSimulator, mockFleetManager);

        // Verify initial state
        assertEquals(AgentEvent.State.INITIAL, agentEvent.state);
        
        AgentEvent nextEvent = (AgentEvent) agentEvent.trigger();
        // return intersection3 as next intersection
        when(mockFleetManager.onReachIntersection(eq(nextEvent.id), anyLong(), anyObject()))
                .thenReturn(testMap.intersection3);

        // Verify that next AgentEvent will trigger when reaching the end of road1
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, nextEvent.state);
        assertEquals(testMap.roadFrom1to2, nextEvent.loc.road);
        assertEquals(TRIGGER_TIME + initTimeFromStartIntersection, nextEvent.time);

        nextEvent = (AgentEvent) agentEvent.trigger();

        // Verify that next AgentEvent will trigger when reaching the end of road2
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, nextEvent.state);
        assertEquals(testMap.roadFrom2to3, nextEvent.loc.road);
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime + initTimeFromStartIntersection, nextEvent.time);
    }

    /**
     * Test the following sequence of events
     * 1. Travel to intersection just before pickup
     * 2. Travel to pickup
     * 3. pickup
     * 4. travel to dropoff
     * 5. dropoff
     * 6. back to cruising
     */
    @Test
    public void testTrigger_pickUpAndDropOff() throws Exception {
        // Create a Resource to be picked up half way down on road2to3 and to be dropped off
        // half way down on roadFrom4to5. Agent should traverse roadFrom2to3, roadFrom3to4,
        // and roadFrom4to5
        ResourceEvent customer = makeCustomer(
                SimpleMap.makeLocationFromRoad(testMap.roadFrom2to3, 0.5),
                SimpleMap.makeLocationFromRoad(testMap.roadFrom4to5, 0.5));

        // Create an agentEvent that has a pickup upon reaching intersection2 the beginning of
        // roadFrom2to3
        LocationOnRoad locAtReachedIntersection = new LocationOnRoad(testMap.roadFrom1to2,
                testMap.roadFrom1to2.travelTime);
        AgentEvent agentEvent = new AgentEvent(locAtReachedIntersection, TRIGGER_TIME,
                mockSimulator, mockFleetManager);
        agentEvent.assignTo(customer, 1);
        agentEvent.state = AgentEvent.State.INTERSECTION_REACHED;

        // Trigger the event
        AgentEvent pickUpEvent = (AgentEvent) agentEvent.trigger();

        // Verify that pickup was detected with correct pickuptime.
        assertEquals(AgentEvent.State.PICKING_UP, pickUpEvent.state);
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime/2, pickUpEvent.time);
        assertEquals(testMap.roadFrom2to3, pickUpEvent.loc.road);

        // Trigger pickup Event
        AgentEvent travelToDropoffEvent = (AgentEvent) pickUpEvent.trigger();

        // Verify returned travel event represents travel to the end of the roadFrom2to3
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, travelToDropoffEvent.state);
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime, travelToDropoffEvent.time);
        assertEquals(testMap.roadFrom2to3, travelToDropoffEvent.loc.road);
        assertTrue(travelToDropoffEvent.isPickup);

        // Verify customer was picked up at half-way point of roadFrom2to3
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime/2, customer.pickupTime);

        // Validate simulation statistics
        // * agent search time is the time it took to travel half o roadFrom2to3
        // * and that customer waited from the time that agent was assigned to it (i.e. TRIGGER_TIME)
        assertEquals(testMap.roadFrom2to3.travelTime/2, mockSimulator.totalAgentSearchTime);
        assertEquals(TRIGGER_TIME - AVAILABLE_TIME + testMap.roadFrom2to3.travelTime/2,
                mockSimulator.totalResourceWaitTime);

        // Setup expectations, we expect a call to FleetManager upon reaching end of road2to3 to
        // get next intersection which is intersection 4, i.e. the end of roadFrom3to4.
        when(mockFleetManager.onReachIntersectionWithResource(eq(travelToDropoffEvent.id),
                eq(TRIGGER_TIME + testMap.roadFrom2to3.travelTime), anyObject(), anyObject()))
                .thenReturn(testMap.intersection4);

        // Trigger travel to DropOff's second segment
        AgentEvent travelToDropOffEvent2 = (AgentEvent) travelToDropoffEvent.trigger();

        assertEquals(AgentEvent.State.INTERSECTION_REACHED, travelToDropOffEvent2.state);
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime + testMap.roadFrom3to4.travelTime,
                travelToDropOffEvent2.time);
        assertEquals(testMap.roadFrom3to4, travelToDropOffEvent2.loc.road);
        assertTrue(travelToDropOffEvent2.isPickup);

        // Now trigger to create drop-off event
        AgentEvent dropoffEvent = (AgentEvent) travelToDropOffEvent2.trigger();

        assertEquals(AgentEvent.State.DROPPING_OFF, dropoffEvent.state);
        assertEquals(
                TRIGGER_TIME + testMap.roadFrom2to3.travelTime + testMap.roadFrom3to4.travelTime
                        + testMap.roadFrom4to5.travelTime/2,
                dropoffEvent.time);
        assertEquals(testMap.roadFrom4to5, dropoffEvent.loc.road);

        // trigger drop-off
        AgentEvent backToCruisingEvent = (AgentEvent) dropoffEvent.trigger();

        verify(mockSimulator, times(1)).removeEvent(customer);
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, backToCruisingEvent.state);
        assertEquals(testMap.roadFrom4to5, backToCruisingEvent.loc.road);
        assertFalse(backToCruisingEvent.isPickup);
        assertNull(backToCruisingEvent.assignedResource);
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime + testMap.roadFrom3to4.travelTime
                + testMap.roadFrom4to5.travelTime, backToCruisingEvent.time);
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime + testMap.roadFrom3to4.travelTime
                + testMap.roadFrom4to5.travelTime/2, backToCruisingEvent.startSearchTime);

        // Validate simulation statistics
        // * Trip time was time to go halfwady down roadFrom2to3, all the way on roadFrom3to4 and
        //   halfway on roadFrom4to5
        assertEquals(testMap.roadFrom2to3.travelTime/2 + testMap.roadFrom3to4.travelTime
                + testMap.roadFrom4to5.travelTime/2, mockSimulator.totalResourceTripTime);
    }

    private ResourceEvent makeCustomer(LocationOnRoad pickupLocation,
                                       LocationOnRoad dropoffLocation) {
        return new ResourceEvent(pickupLocation, dropoffLocation,
                AVAILABLE_TIME, 0, mockSimulator, mockFleetManager);
    }

    @Test
    public void testNavigate_withPickUp() throws Exception {
        LocationOnRoad locationOnRoad = spy(new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime));
        when(locationOnRoad.toString()).thenReturn("123,45t");

        ResourceEvent resource = new ResourceEvent(
                new LocationOnRoad(testMap.roadFrom2to3, 20L),
                new LocationOnRoad(testMap.roadFrom2to3, 10L),
                100L,
                1000L,
                mockSimulator,
                mockFleetManager
        );

        AgentEvent spyEvent = spy(new AgentEvent(locationOnRoad, 100, mockSimulator, mockFleetManager));
        spyEvent.assignTo(resource, 1);
        spyEvent.state = AgentEvent.State.INTERSECTION_REACHED;

        AgentEvent newEvent = (AgentEvent) spyEvent.trigger();

        assertEquals(AgentEvent.State.PICKING_UP, newEvent.state);

    }

    @Test
    public void testExpiration_withAssignedAgent() throws Exception {
        LocationOnRoad locationOnRoad = spy(new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime));
        when(locationOnRoad.toString()).thenReturn("123,45t");

        mockSimulator.waitingResources = new TreeSet<>();

        ResourceEvent resource = new ResourceEvent(
                new LocationOnRoad(testMap.roadFrom2to3, 20L),
                new LocationOnRoad(testMap.roadFrom2to3, 10L),
                100L,
                1000L,
                mockSimulator,
                mockFleetManager
        );
        resource.state = ResourceEvent.State.EXPIRED;

        AgentEvent spyEvent = spy(new AgentEvent(locationOnRoad, 101L, mockSimulator, mockFleetManager));
        spyEvent.state = AgentEvent.State.INTERSECTION_REACHED;

        spyEvent.assignTo(resource, 101L);
        resource.assignTo(spyEvent);

        spyEvent = (AgentEvent) spyEvent.trigger();

        assertEquals(AgentEvent.State.PICKING_UP, spyEvent.state);

        resource.trigger();

        assertEquals(AgentEvent.State.INTERSECTION_REACHED, spyEvent.state);
    }

    @Test
    public void testTwoRoadsIntesecting() {
        assertEquals(testMap.intersection1, testMap.roadFrom1to2.from);
        assertEquals(testMap.intersection2, testMap.roadFrom1to2.to);
        assertEquals(testMap.roadFrom1to2.to, testMap.roadFrom2to3.from);
        assertEquals(testMap.intersection3, testMap.roadFrom2to3.to);
    }

}
