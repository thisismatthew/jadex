package jadex.base.service.awareness.discovery;

import jadex.base.service.awareness.AwarenessInfo;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.SUtil;
import jadex.xml.annotation.XMLClassname;

import java.util.Timer;

/**
 * 
 */
public class SendHandler
{
	/** The agent. */
	protected DiscoveryState state;

	/** The root component identifier. */
	protected IComponentIdentifier root;
	
	/** The timer. */
	protected Timer	timer;

	/** The current send id. */
	protected String sendid;
	
	/**
	 *  Create a new lease time handling object.
	 */
	public SendHandler(DiscoveryState state)
	{
		this.state = state;
		this.root = state.getExternalAccess().getComponentIdentifier().getRoot();
		startSendBehavior();
	}
	
	/**
	 *  Start sending awareness infos.
	 *  (Ends automatically when a new send behaviour is started).
	 */
	public void startSendBehavior()
	{
		if(state.isStarted())
		{
			final String sendid = SUtil.createUniqueId(state.getExternalAccess().getComponentIdentifier().getLocalName());
			this.sendid = sendid;	
			
			state.getExternalAccess().scheduleStep(new IComponentStep()
			{
				@XMLClassname("send")
				public Object execute(IInternalAccess ia)
				{
					if(!state.isKilled() && sendid.equals(getSendId()))
					{
//						System.out.println(System.currentTimeMillis()+" sending: "+getComponentIdentifier());
						send(new AwarenessInfo(root, AwarenessInfo.STATE_ONLINE, state.getDelay(), state.getIncludes(), state.getExcludes()));
						
						if(state.getDelay()>0)
							state.doWaitFor(state.getDelay(), this);
					}
					return null;
				}
			});
		}
	}
	
	/**
	 *  Get the sendid.
	 *  @return the sendid.
	 */
	public String getSendId()
	{
		return sendid;
	}

	/**
	 *  Set the sendid.
	 *  @param sendid The sendid to set.
	 */
	public void setSendId(String sendid)
	{
		this.sendid = sendid;
	}
	
	/**
	 *  Method to send messages.
	 */
	public void send(AwarenessInfo info)
	{
	}
}
