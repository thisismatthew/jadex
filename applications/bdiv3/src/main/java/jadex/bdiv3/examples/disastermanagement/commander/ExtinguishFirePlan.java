package jadex.bdiv3.examples.disastermanagement.commander;

import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanAborted;
import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanPrecondition;
import jadex.bdiv3.examples.disastermanagement.IExtinguishFireService;
import jadex.bdiv3.examples.disastermanagement.commander.CommanderAgent.SendRescueForce;
import jadex.commons.future.ITerminableFuture;
import jadex.extension.envsupport.environment.ISpaceObject;

/**
 * 
 */
@Plan
public class ExtinguishFirePlan 
{
	protected ITerminableFuture<Void>	ef;
	
	/**
	 *  The body method is called on the
	 *  instantiated plan instance from the scheduler.
	 */
	@PlanBody
	public void	body(SendRescueForce goal)
	{
		ISpaceObject disaster = goal.getDisaster();
		IExtinguishFireService force = (IExtinguishFireService)goal.getRescueForce();
		ef = force.extinguishFire(disaster.getId());
		ef.get();
	}
	
	/**
	 *  Called when the plan is aborted.
	 */
	@PlanAborted
	public void aborted()
	{
		if(ef!=null)
		{
			ef.terminate();
		}
	}
	
	/**
	 * 
	 */
	@PlanPrecondition
	public boolean checkPrecondition(SendRescueForce goal)
	{
		return goal.getRescueForce() instanceof IExtinguishFireService;
	}
}
