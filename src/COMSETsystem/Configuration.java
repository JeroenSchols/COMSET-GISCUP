package COMSETsystem;

/**
 * Class to hold the configuration parameters of the simulation. Call static method Configuration.make() first to
 * create a singleton configuration object, then call Configuration.get() to retrieve the singleton.
 */
public class Configuration {
    // Full path to an OSM JSON map file
    public final String mapJSONFile;

    // Full path to a KML defining the bounding polygon to crop the map
    public final String boundingPolygonKMLFile;

    // Full path to a TLC New York Yellow trip record file
    public final String resourceFile;

    public final long timeResolution = 1000000;

    // FIXME: The field numberOfAgents should be protected or private. Most code, beside agent creation and placement
    //   code don't need to know its value. Also the field dynamicTraffic should also be hidden.
    // The number of agents that are deployed (at the beginning of the simulation).
    public final long numberOfAgents;
    public final boolean dynamicTrafficEnabled;

    // Members accessible from COMSETsystem, hidden from others, i.e. they have no business knowing them.
    public final long resourceMaximumLifeTime, resourceMaximumLifeTimeInSeconds;
    // Traffic pattern epoch in seconds
    public final long trafficPatternEpoch, trafficPatternEpochInSeconds;
    // Traffic pattern step in seconds
    public final long trafficPatternStep, trafficPatternStepInSeconds;
    public final long agentPlacementRandomSeed;

    protected static Configuration singletonConfiguration;

    // A class that extends BaseAgent and implements a search routing strategy
    protected final Class<? extends FleetManager> fleetManagerClass;

    private Configuration(Class<? extends FleetManager> fleetManagerClass,
                          String mapJSONFile,
                          String resourceFile,
                          long numberOfAgents,
                          String boundingPolygonKMLFile,
                          long resourceMaximumLifeTime,
                          long agentPlacementRandomSeed,
                          boolean dynamicTrafficEnabled,
                          long trafficPatternEpoch,
                          long trafficPatternStep) {
        this.fleetManagerClass = fleetManagerClass;
        this.mapJSONFile = mapJSONFile;
        this.resourceFile = resourceFile;
        this.numberOfAgents = numberOfAgents;
        this.boundingPolygonKMLFile = boundingPolygonKMLFile;
        resourceMaximumLifeTimeInSeconds = resourceMaximumLifeTime;
        this.resourceMaximumLifeTime = resourceMaximumLifeTimeInSeconds * timeResolution;
        this.agentPlacementRandomSeed = agentPlacementRandomSeed;
        this.dynamicTrafficEnabled = dynamicTrafficEnabled;

        trafficPatternEpochInSeconds = trafficPatternEpoch;
        this.trafficPatternEpoch = trafficPatternEpochInSeconds * timeResolution;
        trafficPatternStepInSeconds = trafficPatternStep;
        this.trafficPatternStep = trafficPatternStepInSeconds * timeResolution;
    }

    public static void make(Class<? extends FleetManager> fleetManagerClass,
                            String mapJSONFile,
                            String resourceFile,
                            long numberOfAgents,
                            String boundingPolygonKMLFile,
                            long resourceMaximumLifetime,
                            long agentPlacementRandomSeed,
                            boolean dynamicTraffic,
                            long trafficPatternEpoch,
                            long trafficPatternStep) {
        if (singletonConfiguration == null) {
            singletonConfiguration = new Configuration(
                    fleetManagerClass,
                    mapJSONFile,
                    resourceFile,
                    numberOfAgents,
                    boundingPolygonKMLFile,
                    resourceMaximumLifetime,
                    agentPlacementRandomSeed,
                    dynamicTraffic,
                    trafficPatternEpoch,
                    trafficPatternStep);
        }
    }

    public static Configuration get() {
        assert singletonConfiguration != null;
        return singletonConfiguration;
    }

    public static long toSeconds(long scaledSimulationTime) {
        return scaledSimulationTime / singletonConfiguration.timeResolution;
    }

    public static double toSimulatedSpeed(double distancePerSecond) {
        return distancePerSecond / singletonConfiguration.timeResolution;
    }
}
