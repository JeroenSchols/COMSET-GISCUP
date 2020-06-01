package COMSETsystem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.PriorityQueue;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentEventTest {

    public static final int CUSTOMER_TRIP_TIME = 500;
    public static final long TIME_TO_PICKUP_CUSTOMER = 300L;
    @Mock BaseAgent mockAgent;
    @Mock Simulator mockSimulator;
    @Mock Event mockEvent;
    @Mock ResourceEvent mockResourceEvent;
    @Mock LocationOnRoad mockLocationOnRoad;
    @Mock Road mockRoad;
    @Mock Simulator.PickUp mockNoPickUp;

    @Before
    public void BeforeEachTest() {

    }

    /**
     * Tests that the initial state of an Agent is DROPPING_OFF
     * @throws Exception from spyEvent.Trigger if any
     */
    @Test
    public void testTrigger_initialState() throws Exception {
        when(mockSimulator.MakeAgent(anyLong())).thenReturn(mockAgent);
        AgentEvent spyEvent = spy(new AgentEvent(mockLocationOnRoad, 100, mockSimulator));
        doReturn(mockEvent).when(spyEvent).dropoffHandler();
        assertEquals(mockEvent, spyEvent.trigger());
    }

    /**
     * Tests that DropOffHandler returns an event for INTERSECTION_REACHED with the right info.
     * @throws Exception pass through from called functions.
     */
    @Test
    public void testDropOffHandler_NoWaitingResources() throws Exception {
        // expectations and mocks
        when(mockSimulator.MakeAgent(anyLong())).thenReturn(mockAgent);
        mockSimulator.emptyAgents = new TreeSet<>(new Simulator.AgentEventComparator());
        mockLocationOnRoad.travelTimeFromStartIntersection = 1000;
        mockLocationOnRoad.road = mockRoad;
        mockRoad.travelTime = 200001;
        AgentEvent spyEvent = spy(new AgentEvent(mockLocationOnRoad, 100, mockSimulator));
        doReturn(mockNoPickUp).when(mockSimulator).FindEarliestPickup(mockLocationOnRoad);

        // Run code under test
        Event nextEvent = spyEvent.trigger();

        // Check results
        assertEquals(spyEvent, nextEvent);
        assertEquals(1, mockSimulator.emptyAgents.size());
        assertTrue(mockSimulator.emptyAgents.contains(spyEvent));
        assertEquals(199101, spyEvent.time);
        assertEquals(AgentEvent.INTERSECTION_REACHED, spyEvent.eventCause);
        assertEquals(mockRoad, spyEvent.loc.road);
        assertEquals(200001, spyEvent.loc.travelTimeFromStartIntersection);
    }

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Test
    public void testDropOffHandler_WaitingResources() throws Exception {
        // expectations and mocks
        when(mockSimulator.MakeAgent(anyLong())).thenReturn(mockAgent);

        //TODO DropOffHandler knows far too much about the simulator, remove those dependencies.
        // Doing that also do away with this horrible mock setups.
        mockSimulator.emptyAgents = (TreeSet<AgentEvent>) mock(TreeSet.class);
        mockSimulator.waitingResources = (TreeSet<ResourceEvent>) mock(TreeSet.class);
        mockSimulator.events = (PriorityQueue<Event>) mock(PriorityQueue.class);

        // Create a ResourceEvent to Pickup
        LocationOnRoad mockDropOffLoc = mock(LocationOnRoad.class);
        LocationOnRoad mockPickupLoc = mock(LocationOnRoad.class);
        ResourceEvent customer = new ResourceEvent(mockPickupLoc, mockDropOffLoc, 10000,
                CUSTOMER_TRIP_TIME, mockSimulator);

        // Setup Mock Pickup
        Simulator.PickUp mockActualPickUp = mock(Simulator.PickUp.class);
        doReturn(customer).when(mockActualPickUp).getResource();
        doReturn(TIME_TO_PICKUP_CUSTOMER).when(mockActualPickUp).getTime();

        mockLocationOnRoad.travelTimeFromStartIntersection = 1000;
        mockLocationOnRoad.road = mockRoad;
        mockRoad.travelTime = 200001;
        AgentEvent spyEvent = spy(new AgentEvent(mockLocationOnRoad, 100, mockSimulator));
        doReturn(mockActualPickUp).when(mockSimulator).FindEarliestPickup(mockLocationOnRoad);

        // Run code under test
        Event nextEvent = spyEvent.trigger();

        // Check results
        assertEquals(spyEvent, nextEvent);

        //TODO Symption of DropOffHandler knowing too much about the simulator.  We sholdn't be
        // verifying the consistency of simulator data structures when we are testing AgentEvent
        verify(mockSimulator.emptyAgents).remove(spyEvent);
        verify(mockSimulator.waitingResources).remove(customer);
        verify(mockSimulator.events).remove(customer);

        assertEquals(CUSTOMER_TRIP_TIME + TIME_TO_PICKUP_CUSTOMER, spyEvent.time);
        assertEquals(AgentEvent.DROPPING_OFF, spyEvent.eventCause);
        assertEquals(mockDropOffLoc, spyEvent.loc);
    }}