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
    public final boolean dynamicTraffic;

    // Members accessible from COMSETsystem, hidden from others, i.e. they have no business knowing them.
    protected final long resourceMaximumLifetime;
    protected final long agentPlacementRandomSeed;
    protected final long trafficPatternEpoch;
    protected final long trafficPatternStep;

    protected static Configuration singletonConfiguration;

    private Configuration(String mapJSONFile,
                          String resourceFile,
                          long numberOfAgents,
                          String boundingPolygonKMLFile,
                          long resourceMaximumLifetime,
                          long agentPlacementRandomSeed,
                          boolean dynamicTraffic,
                          long trafficPatternEpoch,
                          long trafficPatternStep) {
        this.mapJSONFile = mapJSONFile;
        this.resourceFile = resourceFile;
        this.numberOfAgents = numberOfAgents;
        this.boundingPolygonKMLFile = boundingPolygonKMLFile;
        this.resourceMaximumLifetime = resourceMaximumLifetime;
        this.agentPlacementRandomSeed = agentPlacementRandomSeed;
        this.dynamicTraffic = dynamicTraffic;
        this.trafficPatternEpoch = trafficPatternEpoch;
        this.trafficPatternStep = trafficPatternStep;
    }

    public static void make(String mapJSONFile,
                            String resourceFile,
                            long numberOfAgents,
                            String boundingPolygonKMLFile,
                            long resourceMaximumLifetime,
                            long agentPlacementRandomSeed,
                            boolean dynamicTraffic,
                            long trafficPatternEpoch,
                            long trafficPatternStep) {
        if (singletonConfiguration == null) {
            singletonConfiguration = new Configuration(mapJSONFile,
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
}
