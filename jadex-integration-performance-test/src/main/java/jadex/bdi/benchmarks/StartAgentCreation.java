package jadex.bdi.benchmarks;

import jadex.base.Starter;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;

/**
 *  Starter class for agent creation benchmark.
 */
public class StartAgentCreation 
{
	/**
	 *  Main method starts creation benchmark.
	 */
	public static void main(String[] args) throws Exception
	{
		IExternalAccess ea = Starter.createPlatform(new String[]
		{
//			"-logging", "true",
			"-gui", "false",
			"-extensions", "null",
			"-cli", "false",
//			"-awareness", "false"
		}).get();
		IComponentManagementService cms = SServiceProvider.getService(ea, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get();
		cms.createComponent("jadex.bdi.benchmarks.AgentCreation.agent.xml", null).get();
	}
}
