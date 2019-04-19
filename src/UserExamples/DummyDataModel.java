package UserExamples;

import COMSETsystem.CityMap;

/**
 * A dummy data model that provides nothing.
 * The participants may implement a data model that represents for example the 
 * resource availability pattern per time and per location.
 * 
 * To ensure that the agents do not know each other, the data model object cannot be used for 
 * communication between agents, nor can it be used to inform one agent about another.
 */
public class DummyDataModel {

	// A reference to the map.
	CityMap map;

	public DummyDataModel(CityMap map) {
		this.map = map;
	}

	// A dummy method that returns a greeting.
	public String foo() {
		return "Hello, this is a dummy data model!";
	}

}