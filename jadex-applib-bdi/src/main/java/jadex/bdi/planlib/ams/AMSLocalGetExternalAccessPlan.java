package jadex.bdi.planlib.ams;

import jadex.adapter.base.fipa.IAMS;
import jadex.adapter.base.fipa.SFipa;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IPlatform;

/**
 *  Plan for terminating a Jadex agent on the platform.
 */
public class AMSLocalGetExternalAccessPlan extends Plan
{
	/**
	 *  Execute a plan.
	 */
	public void body()
	{	
		IComponentIdentifier aid = (IComponentIdentifier)getParameter("agentidentifier").getValue();

		final IPlatform plat = getScope().getServiceContainer();
		try
		{
			SyncResultListener lis = new SyncResultListener();
			((IAMS)plat.getService(IAMS.class, SFipa.AMS_SERVICE)).getExternalAccess(aid, lis);
			Object ret = lis.waitForResult();
			getParameter("result").setValue(ret);
		}
		catch(Exception e)
		{
			//e.printStackTrace();
			fail(e); // Do not show exception on console. 
		}
	}
}
