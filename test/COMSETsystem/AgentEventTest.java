package COMSETsystem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.stream.Location;

import java.util.TreeSet;

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

    @Before
    public void BeforeEachTest() {

    }

    /**
     * Tests that the initial state of an Agent is INTERSECTION_REACHED and navigate is called.
     *
     * @throws Exception from spyEvent.Trigger if any
     */
    @Test
    public void testTrigger_initialState() throws Exception {
        AgentEvent spyEvent = spy(new AgentEvent(mockLocationOnRoad, 100, mockSimulator, mockFleetManager));
        doReturn(mockEvent).when(spyEvent).navigate();
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, spyEvent.state);
        assertEquals(mockEvent, spyEvent.trigger());
    }

    @Test
    public void testNavigate_withPickUp() throws Exception {
        SimpleMap map = new SimpleMap();

        LocationOnRoad locationOnRoad = spy(new LocationOnRoad(map.road1, 10L));
        doReturn("123,456").when(locationOnRoad).toString();

        ResourceEvent resource = new ResourceEvent(
                new LocationOnRoad(map.road2, 20L),
                new LocationOnRoad(map.road2, 10L),
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
        SimpleMap map = new SimpleMap();
        assertEquals(map.intersection1, map.road1.from);
        assertEquals(map.intersection2, map.road1.to);
        assertEquals(map.road1.to, map.road2.from);
        assertEquals(map.intersection3, map.road2.to);
    }

    private static class SimpleMap {

        private final Intersection intersection1;
        private final Intersection intersection2;
        private final Intersection intersection3;
        private final Road road1;
        private final Road road2;

        private static Intersection makeIntersection(final double longitude, final double latitude, final int id) {
            Vertex vertex1 = new Vertex(longitude, latitude, id, id, id);
            return new Intersection(vertex1);
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
            intersection1 = makeIntersection(100.0, 100.0, 1);
            intersection2 = makeIntersection(100.0, 101.0, 2);
            intersection3 = makeIntersection(100.0, 102.0, 3);
            road1 = makeRoad(intersection1, intersection2, 10L);
            road2 = makeRoad(intersection2, intersection3, 20L);
        }
    }
}
