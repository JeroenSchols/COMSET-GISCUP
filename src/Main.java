import COMSETsystem.BaseAgent;
import COMSETsystem.Simulator;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.Random;
import java.util.Properties;
import java.io.FileInputStream;

/**
 * The Main class parses the configuration file and starts the simulator. 
 */

public class Main {

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		String configFile = "etc/config.properties";
		try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(configFile));

            //get the property values
            
            String mapJSONFile = prop.getProperty("comset.map_JSON_file").trim();
            if (mapJSONFile == null) {
            	System.out.println("The map JSON file must be specified in the configuration file.");
            	System.exit(1);
            } 

            String datasetFile = prop.getProperty("comset.dataset_file").trim();
            if (datasetFile == null) {
            	System.out.println("The resource dataset file must be specified in the configuration file.");
            	System.exit(1);
            }
            
            String numberOfAgentsArg = prop.getProperty("comset.number_of_agents").trim();
            long numberOfAgents = -1;
            if (numberOfAgentsArg != null) {
            	numberOfAgents = Long.parseLong(numberOfAgentsArg);
            } else {
            	System.out.println("The number of agents must be specified in the configuration file.");
            	System.exit(1);
            }
            
            String boundingPolygonKMLFile = prop.getProperty("comset.bounding_polygon_KML_file").trim();
            if (boundingPolygonKMLFile == null) {
            	System.out.println("The bounding polygon KML file must be specified in the configuration file.");
            	System.exit(1);
            }            	
            
            String agentClassName = prop.getProperty("comset.agent_class").trim();
            if (agentClassName == null) {
            	System.out.println("The agent class must be specified the configuration file.");
            	System.exit(1);
            }            
            
            long resourceMaximumLifeTime = -1;
            String resourceMaximumLifeTimeArg = prop.getProperty("comset.resource_maximum_life_time").trim();
            if (resourceMaximumLifeTimeArg != null) {
            	resourceMaximumLifeTime = Long.parseLong(resourceMaximumLifeTimeArg);
            } else {
            	System.out.println("The resource maximum life time must be specified the configuration file.");
            	System.exit(1);
            }
            
            double speedReduction = 1.0;
            String speedReductionArg = prop.getProperty("comset.speed_reduction").trim();
            if (speedReductionArg != null) {
            	speedReduction = Double.parseDouble(speedReductionArg);
            } else {
            	System.out.println("The speed reduction must be specified the configuration file.");
            	System.exit(1);
            }
            
            boolean displayLogging = false;
            String displayLoggingArg = prop.getProperty("comset.logging").trim();
            if (displayLoggingArg != null) {
            	displayLogging = Boolean.parseBoolean(displayLoggingArg);
            } 
            
            long agentPlacementSeed = -1;
            String agentPlacementSeedArg = prop.getProperty("comset.agent_placement_seed").trim();
            if (agentPlacementSeedArg != null) {
            	agentPlacementSeed = Long.parseLong(agentPlacementSeedArg);
            } 
            if (agentPlacementSeed < 0) {
    			Random random = new Random();
    			agentPlacementSeed = random.nextLong();
            }
            
            Class<?> agentClass = Class.forName(agentClassName);
			Simulator simulator = new Simulator((Class<? extends BaseAgent>)agentClass);

			if (!displayLogging) {
				LogManager.getLogManager().reset();
			}
			
			simulator.configure(mapJSONFile, datasetFile, numberOfAgents, boundingPolygonKMLFile, resourceMaximumLifeTime, agentPlacementSeed, speedReduction);

			simulator.run();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
