package jadex.bdiv3.examples.disastermanagement.commander;

import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanPrecondition;
import jadex.bdiv3.examples.disastermanagement.ITreatVictimsService;
import jadex.bdiv3.examples.disastermanagement.commander.CommanderBDI.SendRescueForce;
import jadex.commons.future.ITerminableFuture;
import jadex.extension.envsupport.environment.ISpaceObject;

/**
 * 
 */
@Plan
public class TreatVictimsPlan
{
	//-------- attributes --------
	
	/** The service future. */
	protected ITerminableFuture<Void>	tv;
	
	//-------- plan methods --------
	
	/**
	 *  The body method is called on the
	 *  instantiated plan instance from the scheduler.
	 */
	@PlanBody
	public void	body(SendRescueForce goal)
	{
		ISpaceObject disaster = (ISpaceObject)goal.getDisaster();
		ITreatVictimsService force = (ITreatVictimsService)goal.getRescueForce();
		try
		{
			tv	= force.treatVictims(disaster);
			tv.get();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 *  Called when the plan is aborted.
	 */
	public void aborted()
	{
		if(tv!=null)
		{
			try
			{
				tv.terminate();
			}
			catch(Exception e)
			{
				// Wait until service is finished before superordinated goal is dropped.
				tv.get();
			}
		}
	}
	
	/**
	 * 
	 */
	@PlanPrecondition
	public boolean checkPrecondition(SendRescueForce goal)
	{
		return goal.getRescueForce() instanceof ITreatVictimsService;
	}
}
