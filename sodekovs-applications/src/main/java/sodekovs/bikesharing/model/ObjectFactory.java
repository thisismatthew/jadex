//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.08.10 at 11:42:51 AM CEST 
//


package sodekovs.bikesharing.model;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the sodekovs.bikesharing.model package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: sodekovs.bikesharing.model
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ProbabilitiesForStation }
     * 
     */
    public ProbabilitiesForStation createProbabilitiesForStation() {
        return new ProbabilitiesForStation();
    }

    /**
     * Create an instance of {@link TimeSlice }
     * 
     */
    public TimeSlice createTimeSlice() {
        return new TimeSlice();
    }

    /**
     * Create an instance of {@link SimulationDescription }
     * 
     */
    public SimulationDescription createSimulationDescription() {
        return new SimulationDescription();
    }

    /**
     * Create an instance of {@link Station }
     * 
     */
    public Station createStation() {
        return new Station();
    }

    /**
     * Create an instance of {@link ProbabilitiesForStation.DestinationProbabilities }
     * 
     */
    public ProbabilitiesForStation.DestinationProbabilities createProbabilitiesForStationDestinationProbabilities() {
        return new ProbabilitiesForStation.DestinationProbabilities();
    }

    /**
     * Create an instance of {@link TimeSlice.ProbabilitiesForStations }
     * 
     */
    public TimeSlice.ProbabilitiesForStations createTimeSliceProbabilitiesForStations() {
        return new TimeSlice.ProbabilitiesForStations();
    }

    /**
     * Create an instance of {@link TimeSlice.Stations }
     * 
     */
    public TimeSlice.Stations createTimeSliceStations() {
        return new TimeSlice.Stations();
    }

    /**
     * Create an instance of {@link DestinationProbability }
     * 
     */
    public DestinationProbability createDestinationProbability() {
        return new DestinationProbability();
    }

    /**
     * Create an instance of {@link SimulationDescription.TimeSlices }
     * 
     */
    public SimulationDescription.TimeSlices createSimulationDescriptionTimeSlices() {
        return new SimulationDescription.TimeSlices();
    }

    /**
     * Create an instance of {@link SimulationDescription.Stations }
     * 
     */
    public SimulationDescription.Stations createSimulationDescriptionStations() {
        return new SimulationDescription.Stations();
    }

}
