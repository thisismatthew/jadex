package jadex.bridge.component;

import jadex.bridge.IInternalAccess;

import java.util.Set;

/**
 *  A component feature is a pluggable part of the state and behavior
 *  of a component.
 *  This interface follows the prototype-instance-pattern and represents
 *  the type level (i.e. factory) as well as the instance level.
 *  
 *  The feature can inject parameters for expression evaluation
 *  using the value fetcher super interface. The fetch order is the reverse
 *  init order, i.e., later features can override values from earlier features.
 */
public interface IComponentFeatureFactory
{
	//-------- type level methods --------
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 */
	public Set<Class<?>>	getPredecessors();
	
	/**
	 *  Get the successors, i.e. features that should be inited after this feature.
	 */
	public Set<Class<?>>	getSuccessors();
	
	/**
	 *  Get the user interface type of the feature.
	 */
	public Class<?>	getType();
	
	/**
	 *  Create an instance of the feature.
	 *  @param access	The access of the component.
	 *  @param info	The creation info.
	 */
	public IComponentFeature	createInstance(IInternalAccess access, ComponentCreationInfo info);
}