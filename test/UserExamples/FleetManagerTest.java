package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.LocationOnRoad;
import COMSETsystem.SimpleMap;
import UserExamples.RandomDestinationFleetManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * This test FleetManager Class basic functionality
 * It will initially only test the RandomDestinationFleetManager and we'll generalize later to cover
 * all Fleet Manager Implementations
 */
public class FleetManagerTest {
    @Mock
    CityMap mockMap;
    @Mock
    private SimpleMap testMap = new SimpleMap();

    /**
     * Test onReachIntersection creates a route wheren there is no route and returns the first
     * intersection in the route
     */
    @Test
    public void test_onReachIntersection_emptyRoute() {
        // Instantiate a FleetManager
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        LocationOnRoad currentLoc = new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime);
        Intersection[] intersections  = {testMap.intersection3, testMap.intersection4};
        doReturn(new LinkedList<>(Arrays.asList(intersections)))
                .when(spyFleetManager).getRandomRoute(1, currentLoc);

        Intersection intersection = spyFleetManager.onReachIntersection(1, 1000, currentLoc);
        verify(spyFleetManager, times(1)).getRandomRoute(1, currentLoc);
        assertEquals(testMap.intersection3, intersection);

        // Check the remaining part of the route has the left over part of the route
        // Argument to containsKey and get must have long type
        assertTrue(spyFleetManager.agentRoutes.containsKey(1L));
        assertEquals(testMap.intersection4, spyFleetManager.agentRoutes.get(1L).getFirst());
    }

    @Test
    public void test_onReachIntersection_nonEmptyRoute() {
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        LocationOnRoad currentLoc = new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime);
        Intersection[] intersections  = {testMap.intersection3, testMap.intersection4, testMap.intersection5};
        spyFleetManager.agentRoutes.put(1L, new LinkedList<>(Arrays.asList(intersections)));

        Intersection intersection = spyFleetManager.onReachIntersection(1L, 1000, currentLoc);

        assertEquals(testMap.intersection3, intersection);
        verify(spyFleetManager, times(0)).planRoute(anyLong(), anyObject());
        assertTrue(spyFleetManager.agentRoutes.containsKey(1L));
        assertEquals(testMap.intersection4, spyFleetManager.agentRoutes.get(1L).getFirst());
    }

}
