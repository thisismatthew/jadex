/**
 * 
 */
package deco4mas.coordinate.interpreter.coordination_information;

import jadex.application.space.envsupport.environment.EnvironmentEvent;
import jadex.application.space.envsupport.environment.IPerceptGenerator;



/**
 * @author Ante Vilenica
 * 
 * Percept generator for agents that have to be coordinated.
 * Responds to the "Coordination Event Perception" within the deco4mas-architecture.
 */

public interface ICoordinationEventGenerator extends IPerceptGenerator {

	public void dispatchEnvironmentEvent(EnvironmentEvent event);
}
