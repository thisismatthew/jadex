package jadex.simulation.evaluation;

import jadex.simulation.helper.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import sodekovs.bikesharing.model.SimulationDescription;
import sodekovs.bikesharing.model.Station;
import sodekovs.bikesharing.model.TimeSlice;
import sodekovs.util.misc.XMLHandler;

/**
 * Special class for evaluating the bikesharing application. Can be used to compare the simulation results with the real data results retrieved from the live system.
 * 
 * @author Vilenica
 * 
 */
public class BikeSharingEvaluation {

	private String realDataXMLFile = "E:\\Workspaces\\Jadex\\BikeSharing\\MyProject\\sodekovs-applications\\src\\main\\java\\sodekovs\\bikesharing\\setting\\WashingtonEvaluation_Monday.xml";
	// conf. following method for understanding the data structure:
	// EvaluateRow.evaluateRowData(preparedRowData)
	private HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>> simulationData;
	// contains the final results of the evaluation
	private HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>> compareSimAndRealWorldResultsMap = new HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>>();
	//contains the evaluation of the stock level, sorted into three buckets (blue, green, red -> cf. the *.application.xml for details)	
	private HashMap<Integer, HashMap<String, Integer>> stockLevelResultsMap = new HashMap<Integer, HashMap<String, Integer>>();

	public BikeSharingEvaluation(HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>> simulationData) {
		this.simulationData = simulationData;
	}

	public void compare() {

		// 1.Compute tick size, e.g. the "length" of the observed simulation
		int tickSize = computeTickSize();

		// 2.Transform simData into new data structure --> contains the values
		// of each object instances sorted by tick
		HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>> transformedSimDataMap = transformSimData(tickSize);

		// 3.Compare Simulation Results with real data
		compareResults(transformedSimDataMap);
		
		//4. Evaluate the stock level
//		evalStockLevel();

	}

	private HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>> transformSimData(int tickSize) {
		HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>> resultMap = new HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>>();

		for (int i = 0; i < tickSize; i++) {

			// result Map for all object instances for this tick
			HashMap<String, HashMap<String, HashMap<String, String>>> result1 = new HashMap<String, HashMap<String, HashMap<String, String>>>();

			// station instance iterator
			for (Iterator<String> stationInstanceIt = simulationData.keySet().iterator(); stationInstanceIt.hasNext();) {

				// result Map for the properties of one object instance
				HashMap<String, HashMap<String, String>> result2 = new HashMap<String, HashMap<String, String>>();

				// station instance properties Map
				String stationInstanceKey = stationInstanceIt.next();
				HashMap<String, HashMap<String, ArrayList<String>>> stationInstancePropertiesMap = simulationData.get(stationInstanceKey);

				// station instance properties iterator
				for (Iterator<String> stationInstancePropertiesIt = stationInstancePropertiesMap.keySet().iterator(); stationInstancePropertiesIt.hasNext();) {

					// result Map for one property
					HashMap<String, String> result3 = new HashMap<String, String>();

					// station instance properties-> observed values Map
					String observedPropertyValuesKey = stationInstancePropertiesIt.next();
					HashMap<String, ArrayList<String>> observedPropertyValues = stationInstancePropertiesMap.get(observedPropertyValuesKey);

					// retrieve observed value for this property at this tick
					// "i"
					result3.put(Constants.MEAN_VALUE, observedPropertyValues.get(Constants.MEAN_VALUE_LIST).get(i));
					result3.put(Constants.MEDIAN_VALUE, observedPropertyValues.get(Constants.MEDIAN_VALUE_LIST).get(i));
					result3.put(Constants.SAMPLE_VARIANCE_VALUE, observedPropertyValues.get(Constants.SAMPLE_VARIANCE_VALUE_LIST).get(i));

					// put results for this single property into the map of all
					// properties of this object instance
					result2.put(observedPropertyValuesKey, result3);
				}

				// put results for this single object instance into the map of
				// all object instances at this tick
				result1.put(stationInstanceKey, result2);
			}

			// put all observed values for all object instances at this tick
			resultMap.put(i, result1);
		}

		return resultMap;

	}

	// take a arbitrary station with a arbitrary property with a arbitrary
	// observed value to get the tick size, e.g. how many steps have been
	// observed
	private int computeTickSize() {

		// arbitrary station instance iterator
		Iterator<String> stationInstanceIt = simulationData.keySet().iterator();
		String randomStationInstanceKey = stationInstanceIt.next();

		// arbitrary station instance properties iterator
		HashMap<String, HashMap<String, ArrayList<String>>> abritrayStationInstance = simulationData.get(randomStationInstanceKey);
		Iterator<String> stationInstancePropertiesIt = abritrayStationInstance.keySet().iterator();
		String randomStationPropertyInstanceKey = stationInstancePropertiesIt.next();

		// arbitrary station instance observed property values iterator
		HashMap<String, ArrayList<String>> abritrayObservedPropertyValues = abritrayStationInstance.get(randomStationPropertyInstanceKey);
		Iterator<String> abritrayObservedPropertyValuesIt = abritrayObservedPropertyValues.keySet().iterator();
		String abritrayObservedPropertyValuesKey = abritrayObservedPropertyValuesIt.next();

		// get tick size
		return abritrayObservedPropertyValues.get(abritrayObservedPropertyValuesKey).size();
	}

	
	/**
	 * Compare the sim data with real data: compute the difference with respect to the property "stock" 
	 * @param transformedSimDataMap
	 * @return
	 */
	private HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>> compareResults(HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>> transformedSimDataMap) {

		SimulationDescription scenario = (SimulationDescription) XMLHandler.parseXMLFromXMLFile(realDataXMLFile, SimulationDescription.class);

		for (TimeSlice tSlice : scenario.getTimeSlices().getTimeSlice()) {
			long startTime = tSlice.getStartTime();

			for (Station station : tSlice.getStations().getStation()) {
				// station.getNumberOfBikes();
				// String observedVal =
				// transformedSimDataMap.get((int)startTime).get(station.getStationID()).get("stock").get(Constants.MEAN_VALUE);

				// check, whether there are suimulation results for this station
				// HashMap<String, HashMap<String, String>> stationInstancesMap = transformedSimDataMap.get((int) startTime).get(station.getStationID());

				// check , whether there are results for this tick
				// check also, whether there are simulation results for this station
				if (transformedSimDataMap.get((int) startTime) != null && transformedSimDataMap.get((int) startTime).get(station.getStationID()) != null) {
					// HashMap<String, String> prop = statID.get("stock");
					// String val = prop.get(Constants.MEAN_VALUE);
					HashMap<String, HashMap<String, String>> stationInstancesMap = transformedSimDataMap.get((int) startTime).get(station.getStationID());

					// compare
					double difference = station.getNumberOfBikes() / Double.valueOf(stationInstancesMap.get("stock").get(Constants.MEAN_VALUE));

					// compute relative difference
					if (difference <= 1.0) {
						difference = 1.0 - difference;
					} else {
						difference = difference - 1.0;
					}

					// check, if hashMaps have been already initialized
					if (compareSimAndRealWorldResultsMap.get((int) startTime) == null) {
						compareSimAndRealWorldResultsMap.put((int) startTime, new HashMap<String, HashMap<String, HashMap<String, String>>>());
					}

					if (compareSimAndRealWorldResultsMap.get((int) startTime).get(station.getStationID()) == null) {
						compareSimAndRealWorldResultsMap.get((int) startTime).put(station.getStationID(), new HashMap<String, HashMap<String, String>>());
					}

					if (compareSimAndRealWorldResultsMap.get((int) startTime).get(station.getStationID()).get("stock") == null) {
						compareSimAndRealWorldResultsMap.get((int) startTime).get(station.getStationID()).put("stock", new HashMap<String, String>());
					}

					HashMap<String, String> statsForProperty = compareSimAndRealWorldResultsMap.get((int) startTime).get(station.getStationID()).get("stock");

					// put the value, that denotes the difference betwenn the
					// sim data and real data into this HashMap
					statsForProperty.put(Constants.MEAN_VALUE_DIFF_BETWEEN_SIM_AND_REAL_DATA, String.valueOf(difference));

					// add res for this station property to ResultMap
					compareSimAndRealWorldResultsMap.get((int) startTime).get(station.getStationID()).put("stock", statsForProperty);
				}
			}
		}
		return compareSimAndRealWorldResultsMap;
	}

	public String resultsToString() {
		StringBuffer result = new StringBuffer();

		// ew HashMap<Integer, HashMap<String, HashMap<String, HashMap<String, String>>>>();

		// get object instances
		for (Iterator<Integer> it1 = compareSimAndRealWorldResultsMap.keySet().iterator(); it1.hasNext();) {
			int timeSliceKey = it1.next();
			HashMap<String, HashMap<String, HashMap<String, String>>> timeSlicesMap = compareSimAndRealWorldResultsMap.get(timeSliceKey);

			// How many of the results differ between 0-10, 11-20, 21-30 etc.
			ArrayList<Integer> diffStats = new ArrayList<Integer>();
			for (int i = 0; i < 10; i++) {
				diffStats.add(0);
			}

			// Compute the stock level of the bikestations: three buckets:
			// 1.) stock < 1
			// 2.) stock > 0 && stock < capacity
			// 3.) stock >= capacity
			ArrayList<Integer> stockLevel = new ArrayList<Integer>();
			for (int i = 0; i < 3; i++) {
				stockLevel.add(0);
			}

			result.append("TIME SLICE: ");
			result.append("\t");
			result.append(timeSliceKey);
			result.append("\n");

			for (Iterator<String> it2 = timeSlicesMap.keySet().iterator(); it2.hasNext();) {
				String objectInstancesKey = it2.next();
				HashMap<String, HashMap<String, String>> objectInstancesMap = timeSlicesMap.get(objectInstancesKey);

				// for (Iterator<String> it3 = propertiesMap.keySet().iterator(); it3.hasNext();) {
				// String observedPropertyValuesKey = it3.next();
				HashMap<String, String> observedPropertyValues = objectInstancesMap.get("stock");

				// result.append("Station ");
				result.append(objectInstancesKey);
				result.append("\t: ");
				result.append(observedPropertyValues.get(Constants.MEAN_VALUE_DIFF_BETWEEN_SIM_AND_REAL_DATA));
				result.append("\n");
				computeDiff(diffStats, observedPropertyValues.get(Constants.MEAN_VALUE_DIFF_BETWEEN_SIM_AND_REAL_DATA));
				// }

			}
			// Eval similarities between the stock of the bikestations in the real data and the simulation. Compute only the relative comparison, separated into buckets.
			result.append("\n******** Similarities between real and sim data ********************");
			result.append("\nDifference between 0%-10% " + diffStats.get(0));
			result.append("\nDifference between 11%-20% " + diffStats.get(1));
			result.append("\nDifference between 21%-30% " + diffStats.get(2));
			result.append("\nDifference between 31%-40% " + diffStats.get(3));
			result.append("\nDifference between 41%-50% " + diffStats.get(4));
			result.append("\nDifference between 51%-60% " + diffStats.get(5));
			result.append("\nDifference between 61%-70% " + diffStats.get(6));
			result.append("\nDifference between 71%-80% " + diffStats.get(7));
			result.append("\nDifference between 81%-90% " + diffStats.get(8));
			result.append("\nDifference between 91%-100% " + diffStats.get(9));
			result.append("\n*********************************************************************");

			// Compute the stock level of the bikestations: three buckets:
			// 1.) stock < 1
			// 2.) stock > 0 && stock < capacity
			// 3.) stock >= capacity

			result.append("***************************************");
		}

		return result.toString();
	}

	private void computeDiff(ArrayList<Integer> diffStats, String value) {
		double val = Double.valueOf(value);
		val *= 100;

		if (val < 11) {
			int tmp = diffStats.get(0);
			diffStats.set(0, tmp + 1);
		} else if (val < 21) {
			int tmp = diffStats.get(1);
			diffStats.set(1, tmp + 1);
		} else if (val < 31) {
			int tmp = diffStats.get(2);
			diffStats.set(2, tmp + 1);
		} else if (val < 41) {
			int tmp = diffStats.get(3);
			diffStats.set(3, tmp + 1);
		} else if (val < 51) {
			int tmp = diffStats.get(4);
			diffStats.set(4, tmp + 1);
		} else if (val < 61) {
			int tmp = diffStats.get(5);
			diffStats.set(5, tmp + 1);
		} else if (val < 71) {
			int tmp = diffStats.get(6);
			diffStats.set(6, tmp + 1);
		} else if (val < 81) {
			int tmp = diffStats.get(7);
			diffStats.set(7, tmp + 1);
		} else if (val < 91) {
			int tmp = diffStats.get(8);
			diffStats.set(8, tmp + 1);
		} else {
			int tmp = diffStats.get(9);
			diffStats.set(9, tmp + 1);
		}
	}
}
