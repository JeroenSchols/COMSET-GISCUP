package COMSETsystem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
    @Mock
    AssignmentManager mockAssignmentManager;
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
        // Construction agent reaching intersection2
        LocationOnRoad locAtReachedIntersection = new LocationOnRoad(testMap.roadFrom1to2,
                testMap.roadFrom1to2.travelTime);

        AgentEvent agentEvent = new AgentEvent(locAtReachedIntersection,
                TRIGGER_TIME, mockSimulator, mockFleetManager);
        // return intersection3 as next intersection
        when(mockFleetManager.onReachIntersection(eq(agentEvent.id), anyLong(), anyObject()))
                .thenReturn(testMap.intersection3);

        // Verify initial state
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, agentEvent.state);

        AgentEvent nextEvent = (AgentEvent) agentEvent.trigger();

        // Verify that next AgentEvent will trigger when reaching the end of road2
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, nextEvent.state);
        assertEquals(testMap.roadFrom2to3, nextEvent.loc.road);
        assertEquals(TRIGGER_TIME + testMap.roadFrom2to3.travelTime, nextEvent.time);
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
                new PositionOnRoad(testMap.roadFrom2to3, 0.5),
                new PositionOnRoad(testMap.roadFrom4to5, 0.5));

        // Create an agentEvent that has a pickup upon reaching intersection2 the beginning of
        // roadFrom2to3
        LocationOnRoad locAtReachedIntersection = new LocationOnRoad(testMap.roadFrom1to2,
                testMap.roadFrom1to2.travelTime);
        AgentEvent agentEvent = new AgentEvent(locAtReachedIntersection, TRIGGER_TIME,
                mockSimulator, mockFleetManager);
        agentEvent.assignTo(customer);

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

    private static class PositionOnRoad {
        final Road road;

        /**
         * A floating point number from 0 to 1 that defines position on road.
         * 0 = beginning
         * 1 = end
         */
        final double roadFraction;

        PositionOnRoad(Road road, double roadFraction) {
            this.road = road;
            this.roadFraction = roadFraction;
        }
    }

    private static LocationOnRoad makeLocationFromPosition(PositionOnRoad position) {
        return new LocationOnRoad(position.road,
                (long) (position.road.travelTime * position.roadFraction));
    }

    private ResourceEvent makeCustomer(PositionOnRoad pickupPositionOnRoad,
                                       PositionOnRoad dropoffPositionOnRoad) {
        LocationOnRoad pickUpLocation = makeLocationFromPosition(pickupPositionOnRoad);
        LocationOnRoad dropOffLocation = makeLocationFromPosition(dropoffPositionOnRoad);
        return new ResourceEvent(pickUpLocation, dropOffLocation,
                AVAILABLE_TIME, 0, mockSimulator, mockFleetManager, mockAssignmentManager);
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
                mockFleetManager,
                mockAssignmentManager
        );

        AgentEvent spyEvent = spy(new AgentEvent(locationOnRoad, 100, mockSimulator, mockFleetManager));
        spyEvent.assignTo(resource);
        AgentEvent newEvent = (AgentEvent) spyEvent.trigger();

        assertEquals(AgentEvent.State.PICKING_UP, newEvent.state);

    }

    @Test
    public void testTwoRoadsIntesecting() {
        assertEquals(testMap.intersection1, testMap.roadFrom1to2.from);
        assertEquals(testMap.intersection2, testMap.roadFrom1to2.to);
        assertEquals(testMap.roadFrom1to2.to, testMap.roadFrom2to3.from);
        assertEquals(testMap.intersection3, testMap.roadFrom2to3.to);
    }

    private static class SimpleMap {

        private final Vertex vertex1;
        private final Vertex vertex2;
        private final Vertex vertex3;
        private final Vertex vertex4;
        private final Vertex vertex5;
        private final Link link1to2;
        private final Link link2to3;
        private final Link link3to4;
        private final Link link4to5;
        private final Intersection intersection1;
        private final Intersection intersection2;
        private final Intersection intersection3;
        private final Intersection intersection4;
        private final Intersection intersection5;
        private final Road roadFrom1to2;
        private final Road roadFrom2to3;
        private final Road roadFrom3to4;
        private final Road roadFrom4to5;

        private static Vertex makeVertex(final double longitude, final double latitude, final int id) {
            return new Vertex(longitude, latitude, id, id, id);
        }

        private static Intersection makeIntersection(Vertex vertex) {
            Intersection intersection = new Intersection(vertex);
            vertex.intersection = intersection;
            return intersection;
        }

        private static Road makeRoad(Intersection intersection1, Intersection intersection2) {
            Road r = new Road();
            r.from = intersection1;
            r.to = intersection2;
            r.to.roadsMapTo.put(r.from, r);
            r.from.roadsMapFrom.put(r.to, r);
            return r;
        }


        public SimpleMap(){
            vertex1 = makeVertex(100.0, 100.0, 1);
            vertex2 = makeVertex(100.0, 101.0, 2);
            vertex3 = makeVertex(100.0, 102.0, 3);
            vertex4 = makeVertex(100.0, 103.0, 4);
            vertex5 = makeVertex(100.0, 104.0, 5);
            link1to2 = new Link(vertex1, vertex2, 1000, 50);
            link2to3 = new Link(vertex2, vertex3, 1200, 60);
            link3to4 = new Link(vertex3, vertex4, 800, 20);
            link4to5 = new Link(vertex4, vertex5, 900, 10);
            intersection1 = makeIntersection(vertex1);
            intersection2 = makeIntersection(vertex2);
            intersection3 = makeIntersection(vertex3);
            intersection4 = makeIntersection(vertex4);
            intersection5 = makeIntersection(vertex5);
            roadFrom1to2 = makeRoad(intersection1, intersection2);
            roadFrom2to3 = makeRoad(intersection2, intersection3);
            roadFrom3to4 = makeRoad(intersection3, intersection4);
            roadFrom4to5 = makeRoad(intersection4, intersection5);
            roadFrom1to2.addLink(link1to2);
            roadFrom2to3.addLink(link2to3);
            roadFrom3to4.addLink(link3to4);
            roadFrom4to5.addLink(link4to5);
        }
    }
}
