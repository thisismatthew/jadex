package jadex.bpmn.testcases;

import jadex.bridge.nonfunctional.annotation.NFProperties;
import jadex.bridge.nonfunctional.annotation.NFProperty;
import jadex.bridge.sensor.service.ExecutionTimeProperty;
import jadex.commons.future.IFuture;

/**
 * 
 */
public interface IAService 
{
	/**
	 *  Test method.
	 */
	public IFuture<String> test();
}
