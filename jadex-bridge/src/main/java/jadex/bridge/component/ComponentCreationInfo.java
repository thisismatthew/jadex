package jadex.bridge.component;

import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.types.cms.IComponentDescription;

import java.util.Map;

/**
 *  Internal parameter object for data required during component initialization.
 */
public class ComponentCreationInfo
{
	//-------- attributes --------
	
	/** The model. */
	protected IModelInfo	model;
	
	/** The start configuration name. */
	protected String config;
	
	/** The arguments. */
	protected Map<String, Object> arguments;
	
	/** The component description. */
	// Hack???
	protected IComponentDescription desc;
	
	/** The real time flag. */
	protected boolean	realtime;
	
	/** The copy flag. */
	protected boolean	copy;
	
	//-------- constructors --------
	
	/**
	 *  Create an info object.
	 *  @param model	The model (required).
	 *  @param config	The configuration name or null for default (if any).
	 *  @param arguments	The arguments (if any).
	 *  @param desc	The component description (required).
	 *  @param realtime	The real time flag.
	 *  @param copy	The copy flag.
	 */
	public ComponentCreationInfo(IModelInfo model, String config, Map<String, Object> arguments, IComponentDescription desc, boolean realtime, boolean copy)
	{
		this.model	= model;
		this.config = config!=null ? config : model.getConfigurationNames().length>0 ? model.getConfigurationNames()[0] : null;
		this.arguments	= arguments;
		this.desc	= desc;
		this.realtime	= realtime;
		this.copy	= copy;
	}
	
	//-------- methods --------
	
	/**
	 *  Get the model.
	 */
	public IModelInfo	getModel()
	{
		return this.model;
	}
	
	/**
	 *  Get the configuration.
	 */
	public String	getConfiguration()
	{
		return this.config;
	}
	
	/**
	 *  Get the arguments.
	 */
	public Map<String, Object>	getArguments()
	{
		return arguments;
	}
	
	/**
	 *  Get the component description.
	 */
	public IComponentDescription	getComponentDescription()
	{
		return this.desc;
	}
	
	/**
	 *  Get the real time flag.
	 */
	public boolean isRealtime()
	{
		return realtime;
	}

	/**
	 *  Get the copy flag.
	 */
	public boolean isCopy()
	{
		return copy;
	}
}