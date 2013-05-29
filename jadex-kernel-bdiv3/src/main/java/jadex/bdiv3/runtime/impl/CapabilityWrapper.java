package jadex.bdiv3.runtime.impl;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.runtime.IBeliefListener;
import jadex.bdiv3.runtime.ICapability;

/**
 *  Wrapper providing BDI methods to the user.
 */
public class CapabilityWrapper implements ICapability
{
	//-------- attributes --------
	
	/** The agent. */
	protected BDIAgent	agent;
	
	/** The fully qualified capability name (or null for agent). */
	protected String	capa;
	
	//-------- constructors --------
	
	/**
	 *  Create a capability wrapper.
	 */
	public CapabilityWrapper(BDIAgent agent, String capa)
	{
		this.agent	= agent;
		this.capa	= capa;
	}
	
	//-------- ICapability interface --------
	
	/**
	 *  Add a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public void addBeliefListener(final String name, final IBeliefListener listener)
	{
		agent.addBeliefListener(capa!=null ? capa+"."+name : name, listener);
	}
	
	/**
	 *  Remove a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public void removeBeliefListener(String name, IBeliefListener listener)
	{
		agent.removeBeliefListener(capa!=null ? capa+"."+name : name, listener);
	}

	/**
	 *  Get the agent.
	 */
	public BDIAgent	getAgent()
	{
		return agent;
	}
}
