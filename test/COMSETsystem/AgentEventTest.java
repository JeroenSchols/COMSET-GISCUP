package COMSETsystem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentEventTest {

    @Mock BaseAgent mockAgent;
    @Mock Simulator mockSimulator;
    @Mock Event mockEvent;
    @Mock LocationOnRoad mockLocationOnRoad;
    @Mock Road mockRoad;

    /**
     * Tests that the initial state of an Agent is in DROPPING_OFF
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
    @SuppressWarnings("unchecked")
    @Test
    public void testDropOffHandler_NoWaitingResources() throws Exception {
        when(mockSimulator.MakeAgent(anyLong())).thenReturn(mockAgent);
        mockSimulator.waitingResources = new TreeSet<>(new Simulator.ResourceEventComparator());
        mockSimulator.emptyAgents = (TreeSet<AgentEvent>) mock(TreeSet.class);
        mockLocationOnRoad.travelTimeFromStartIntersection = 1000;
        mockLocationOnRoad.road = mockRoad;
        mockRoad.travelTime = 200001;

        AgentEvent spyEvent = spy(new AgentEvent(mockLocationOnRoad, 100, mockSimulator));
        Event nextEvent = spyEvent.trigger();

        assertEquals(spyEvent, nextEvent);
        verify(mockSimulator.emptyAgents).add(spyEvent);
        assertEquals(199101, spyEvent.time);
        assertEquals(AgentEvent.INTERSECTION_REACHED, spyEvent.eventCause);
        assertEquals(mockRoad, spyEvent.loc.road);
        assertEquals(200001, spyEvent.loc.travelTimeFromStartIntersection);
    }
}