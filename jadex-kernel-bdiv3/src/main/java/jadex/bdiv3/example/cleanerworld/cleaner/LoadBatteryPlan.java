package jadex.bdiv3.example.cleanerworld.cleaner;

import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanCapability;
import jadex.bdiv3.annotation.PlanPlan;
import jadex.bdiv3.annotation.PlanReason;
import jadex.bdiv3.example.cleanerworld.cleaner.CleanerBDI.AchieveMoveTo;
import jadex.bdiv3.example.cleanerworld.cleaner.CleanerBDI.MaintainBatteryLoaded;
import jadex.bdiv3.example.cleanerworld.cleaner.CleanerBDI.QueryChargingStation;
import jadex.bdiv3.example.cleanerworld.world.Chargingstation;
import jadex.bdiv3.runtime.RPlan;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;


/**
 *  Go to the charging station and load the battery.
 */
public class LoadBatteryPlan
{
	@PlanCapability
	protected CleanerBDI capa;
	
	@PlanPlan
	protected RPlan rplan;
	
	@PlanReason
	protected MaintainBatteryLoaded goal;
	
	//-------- constructors --------

	/**
	 *  Create a new plan.
	 */
	public LoadBatteryPlan()
	{
//		getLogger().info("Created: "+this);
	}

	//-------- methods --------

	/**
	 *  The plan body.
	 */
	@PlanBody
	public IFuture<Void> body()
	{
		final Future<Void> ret = new Future<Void>();
		
		// Move to station.
		rplan.dispatchSubgoal(capa.new QueryChargingStation()).addResultListener(new ExceptionDelegationResultListener<CleanerBDI.QueryChargingStation, Void>(ret)
		{
			public void customResultAvailable(CleanerBDI.QueryChargingStation qcs)
			{
				final Chargingstation station = qcs.getStation();
				rplan.dispatchSubgoal(capa.new AchieveMoveTo(station.getLocation()))
					.addResultListener(new ExceptionDelegationResultListener<CleanerBDI.AchieveMoveTo, Void>(ret)
				{
					public void customResultAvailable(AchieveMoveTo amt)
					{
						IComponentStep<Void> loadstep = new IComponentStep<Void>()
						{
							public IFuture<Void> execute(IInternalAccess ia) 
							{
								final IComponentStep<Void> self = this;
								
								double charge = capa.getMyChargestate();
								if(capa.getMyLocation().getDistance(station.getLocation())<0.01 && charge<1.0)
								{
									charge	= Math.min(charge + 0.01, 1.0);
									capa.setMyChargestate(charge);
								}
								if(charge>=1.0)
								{
									ret.setResult(null);
								}
								else
								{
									rplan.waitFor(100).addResultListener(new DefaultResultListener<Void>()
									{
										public void resultAvailable(Void result)
										{
											capa.getAgent().scheduleStep(self);
										}
									});
								}
								return IFuture.DONE;
							};
						};
						capa.getAgent().scheduleStep(loadstep);
					}
				});
			}
		});
		
		return ret;
	}

}
