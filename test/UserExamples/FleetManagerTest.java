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

    @Test
    public void test_onReachIntersection() {
        // Instantiate a FleetManager
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        LocationOnRoad currentLoc = new LocationOnRoad(testMap.roadFrom1to2,
                testMap.roadFrom1to2.travelTime / 2);
        Intersection[] intersections  = {testMap.intersection2, testMap.intersection3};
        doReturn(new LinkedList<>(Arrays.asList(intersections)))
                .when(spyFleetManager).getRandomRoute(1, currentLoc);

        Intersection intersection = spyFleetManager.onReachIntersection(1, 1000, currentLoc);
        assertEquals(testMap.intersection2, intersection);
    }
}
