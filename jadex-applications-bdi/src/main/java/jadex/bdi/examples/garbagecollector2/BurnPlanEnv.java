package jadex.bdi.examples.garbagecollector2;

import jadex.adapter.base.envsupport.environment.IEnvironmentSpace;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.Plan;
import jadex.bdi.runtime.Plan.SyncResultListener;
import jadex.bridge.IAgentIdentifier;

/**
 *  Burn a piece of garbage.
 */
public class BurnPlanEnv extends Plan
{
	/**
	 *  The plan body.
	 */
	public void body()
	{
//		System.out.println("Burn plan activated!");
		
		IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("env").getFact();

		// Pickup the garbarge.
		IGoal pickup = createGoal("pick");
		dispatchSubgoalAndWait(pickup);

		// Burn the waste.
		waitFor(100);
		
		SyncResultListener	srl	= new SyncResultListener();
		env.performAction("burn", null, srl); // todo: garbage as parameter?
		srl.waitForResult();
		
//		env.burn(getAgentName());
	}
}
