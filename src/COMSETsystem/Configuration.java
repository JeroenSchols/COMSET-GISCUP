package COMSETsystem;

/**
 * Singleton class to hold the configuration parameters of the simulation.
 */
public class Configuration {
    // Full path to an OSM JSON map file
    protected String mapJSONFile;
    protected String datasetFile;
    protected long numberOfAgents;
    protected String boundingPolygonKMLFile;
    protected long resourceMaximumLifetime;
    protected long agentPlacementRandomSeed;
    protected boolean dynamicTraffic;
    protected long trafficPatternEpoch;
    protected long trafficPatternStep;

    private static final Configuration singletonConfiguration = new Configuration();

    private Configuration() {}

    public static void make(String mapJSONFile,
                            String datasetFile,
                            long numberOfAgents,
                            String boundingPolygonKMLFile,
                            long resourceMaximumLifetime,
                            long agentPlacementRandomSeed,
                            boolean dynamicTraffic,
                            long trafficPatternEpoch,
                            long trafficPatternStep) {
        singletonConfiguration.mapJSONFile = mapJSONFile;
        singletonConfiguration.datasetFile = datasetFile;
        singletonConfiguration.numberOfAgents = numberOfAgents;
        singletonConfiguration.boundingPolygonKMLFile = boundingPolygonKMLFile;
        singletonConfiguration.resourceMaximumLifetime = resourceMaximumLifetime;
        singletonConfiguration.agentPlacementRandomSeed = agentPlacementRandomSeed;
        singletonConfiguration.dynamicTraffic = dynamicTraffic;
        singletonConfiguration.trafficPatternEpoch = trafficPatternEpoch;
        singletonConfiguration.trafficPatternStep = trafficPatternStep;
    }

    public static Configuration get() {
        return singletonConfiguration;
    }
}
