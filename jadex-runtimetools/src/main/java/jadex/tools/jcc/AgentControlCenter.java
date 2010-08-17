package jadex.tools.jcc;

import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IAgentListener;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.service.IServiceProvider;

import javax.swing.SwingUtilities;

/**
 * The Jadex control center.
 */
public class AgentControlCenter extends ControlCenter
{
	//-------- attributes --------
	
	/** The external access. */
	protected IBDIExternalAccess agent;

	//-------- constructors --------

	/**
	 * Create a control center.
	 */
	public AgentControlCenter(IServiceProvider provider, String plugins_prop, final IBDIExternalAccess agent)
	{
		super(provider, agent.getComponentIdentifier(), plugins_prop);
		
		this.agent = agent;

		agent.addAgentListener(new IAgentListener()
		{
			public void agentTerminating(AgentEvent ae)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						if(!killed)
						{
							saveProject();
//							closeProject();
							closePlugins();
							killed = true;
						}
						window.setVisible(false);
						window.dispose();
					}
				});
			}

			public void agentTerminated(AgentEvent ae)
			{
			}
		});
	}

	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public IBDIExternalAccess getAgent()
	{
		return this.agent;
	}
}
