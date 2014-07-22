package jadex.bridge.service.types.factory;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeature;
import jadex.commons.future.IFuture;

import java.util.Collection;

/**
 *  Interface for operations on the component to be called from the platform.
 */
public interface IPlatformComponentAccess
{
	//-------- methods --------
	
	/**
	 *  Perform the initialization of the component.
	 *  
	 *  @param info The component creation info.
	 *  @param templates The component feature templates to be instantiated for this component.
	 *  @return A future to indicate when the initialization is done.
	 */
	public IFuture<Void>	init(ComponentCreationInfo info, Collection<IComponentFeature> templates);
	
	/**
	 *  Get the user view of this platform component.
	 *  
	 *  @return An internal access exposing user operations of the component.
	 */
	public IInternalAccess	getInternalAccess();
}