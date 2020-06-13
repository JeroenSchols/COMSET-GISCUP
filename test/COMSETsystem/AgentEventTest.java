package COMSETsystem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentEventTest {

    public static final int CUSTOMER_TRIP_TIME = 500;
    public static final long TIME_TO_PICKUP_CUSTOMER = 300L;
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
     * Tests that the initial state of an Agent is INTERSECTION_REACHED and navigate is called.
     * Traveling from intersection 1 -> intersection 2 -> intersection 3
     *
     * @throws Exception from spyEvent.Trigger if any
     */
    @Test
    public void testTrigger_initialState() throws Exception {
        LocationOnRoad locAtReachedIntersection = new LocationOnRoad(testMap.road1, testMap.road1.travelTime);
        AgentEvent agentEvent = new AgentEvent(locAtReachedIntersection,
                100, mockSimulator, mockFleetManager);
        when(mockFleetManager.onReachIntersection(eq(agentEvent.id), anyLong(), anyObject()))
                .thenReturn(testMap.intersection3);
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, agentEvent.state);
        assertEquals(agentEvent, agentEvent.trigger());
    }

    @Test
    public void testNavigate_withPickUp() throws Exception {
        LocationOnRoad locationOnRoad = spy(new LocationOnRoad(testMap.road1, testMap.road1.travelTime));
        when(locationOnRoad.toString()).thenReturn("123,45t");

        ResourceEvent resource = new ResourceEvent(
                new LocationOnRoad(testMap.road2, 20L),
                new LocationOnRoad(testMap.road2, 10L),
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
        assertEquals(testMap.intersection1, testMap.road1.from);
        assertEquals(testMap.intersection2, testMap.road1.to);
        assertEquals(testMap.road1.to, testMap.road2.from);
        assertEquals(testMap.intersection3, testMap.road2.to);
    }

    private static class SimpleMap {

        private final Vertex vertex1;
        private final Vertex vertex2;
        private final Vertex vertex3;
        private final Link link1to2;
        private final Link link2to3;
        private final Intersection intersection1;
        private final Intersection intersection2;
        private final Intersection intersection3;
        private final Road road1;
        private final Road road2;

        private static Vertex makeVertex(final double longitude, final double latitude, final int id) {
            return new Vertex(longitude, latitude, id, id, id);
        }

        private static Intersection makeIntersection(Vertex vertex) {
            Intersection intersection = new Intersection(vertex);
            vertex.intersection = intersection;
            return intersection;
        }

        private static Road makeRoad(Intersection intersection1, Intersection intersection2, long travelTime) {
            Road r = new Road();
            r.from = intersection1;
            r.to = intersection2;
            r.to.roadsMapTo.put(r.from, r);
            r.from.roadsMapFrom.put(r.to, r);
            r.travelTime = travelTime;
            return r;
        }


        public SimpleMap(){
            vertex1 = makeVertex(100.0, 100.0, 1);
            vertex2 = makeVertex(100.0, 101.0, 2);
            vertex3 = makeVertex(100.0, 102.0, 3);
            link1to2 = new Link(vertex1, vertex2, 1000, 50);
            link2to3 = new Link(vertex2, vertex3, 1200, 60);
            intersection1 = makeIntersection(vertex1);
            intersection2 = makeIntersection(vertex2);
            intersection3 = makeIntersection(vertex3);
            road1 = makeRoad(intersection1, intersection2, 10L);
            road2 = makeRoad(intersection2, intersection3, 20L);
            road1.addLink(link1to2);
            road2.addLink(link2to3);
        }
    }
}
