package jadex.bpmn.runtime;

import java.util.Set;

import jadex.bridge.IComponentAdapter;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.ILoadableComponentModel;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.concurrent.DelegationResultListener;
import jadex.service.IServiceProvider;

/**
 *  External access for bpmn components.
 */
public class ExternalAccess implements IExternalAccess
{
	//-------- attributes --------

	/** The agent. */
	protected BpmnInterpreter interpreter;

	/** The agent adapter. */
	protected IComponentAdapter adapter;

	/** The provider. */
	protected IServiceProvider provider;
	
	//-------- constructors --------

	/**
	 *	Create an external access.
	 */
	public ExternalAccess(BpmnInterpreter interpreter)
	{
		this.interpreter = interpreter;
		this.adapter = interpreter.getComponentAdapter();
		this.provider = interpreter.getServiceProvider();
	}

	//-------- methods --------
	
	/**
	 *  Get the model.
	 *  @return The model.
	 */
	public ILoadableComponentModel	getModel()
	{
		return interpreter.getModel();
	}
	
	/**
	 *  Get the component identifier.
	 */
	public IComponentIdentifier	getComponentIdentifier()
	{
		return adapter.getComponentIdentifier();
	}
	
	/**
	 *  Get the parent.
	 */
	public IExternalAccess getParent()
	{
		return interpreter.getParent();
	}
	
	/**
	 *  Get the children (if any).
	 *  @return The children.
	 */
	public IFuture getChildren()
	{
		return interpreter.getChildren();
	}

	/**
	 *  Get the application component.
	 */
	public IServiceProvider getServiceProvider()
	{
		return provider;
	}
	
	/**
	 *  Kill the component.
	 */
	public IFuture killComponent()
	{
		final Future ret = new Future();
		
		if(adapter.isExternalThread())
		{
			adapter.invokeLater(new Runnable() 
			{
				public void run() 
				{
					interpreter.killComponent().addResultListener(new DelegationResultListener(ret));
				}
			});
		}
		else
		{
			interpreter.killComponent().addResultListener(new DelegationResultListener(ret));
		}
		
		return ret;
	}

	/**
	 *  Get the interpreter.
	 *  @return the interpreter.
	 */
	public BpmnInterpreter getInterpreter()
	{
		return interpreter;
	}

	/**
	 *  Get the string representation.
	 * /
	public String toString()
	{
		return "ExternalAccess(comp=" + tostring + ")";
	}*/
}
