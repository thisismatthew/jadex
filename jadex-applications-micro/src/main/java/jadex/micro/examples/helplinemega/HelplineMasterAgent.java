package jadex.micro.examples.helplinemega;

import javax.swing.SwingUtilities;

import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.GuiClass;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.Description;

/**
 *  Helpline master agent for GUI and starting/stopping subcomponents for specific persons. 
 */
@Description("This agent provides an interface to the single helpline services.")
@GuiClass(HelplineViewerPanel.class)
@Agent
public class HelplineMasterAgent
{
	//-------- attributes --------
	
	/** The agent. */
	@Agent
	protected IInternalAccess agent;
	
	//-------- methods --------
	
	/**
	 *  Called once after agent creation.
	 */
	@AgentCreated
	public IFuture<Void>	agentCreated()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				HelplinePanel.createHelplineGui((IExternalAccess)agent.getExternalAccess());
			}
		});
		return IFuture.DONE;
	}
}