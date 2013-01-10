package jadex.micro.testcases.autoterminate;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.Service;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.SubscriptionIntermediateFuture;
import jadex.commons.future.TerminationCommand;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.testcases.TestAgent;

import java.util.ArrayList;
import java.util.List;

/**
 *  Test automatic termination of subscriptions, when subscriber dies.
 */
@Service
@Agent
@ProvidedServices(@ProvidedService(type=IAutoTerminateService.class, implementation=@Implementation(expression="$pojoagent")))
public class AutoTerminateAgent	extends	TestAgent	implements IAutoTerminateService
{
	//-------- attributes --------
	
	/** The test reports. */
	protected List<TestReport>	reports	= new ArrayList<TestReport>();
	
	/** The agent. */
	@Agent
	protected IInternalAccess	agent;
	
	//-------- methods --------
	
	/**
	 *  Execute the tests.
	 */
	protected IFuture<Void> performTests(Testcase tc)
	{
		final Future<Void>	ret	= new Future<Void>();
		
		setupLocalTest(SubscriberAgent.class.getName()+".class", null)
			.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, Void>(ret)
		{
			public void customResultAvailable(IComponentIdentifier result)
			{
				setupRemoteTest(SubscriberAgent.class.getName()+".class", "self", null)
					.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, Void>(ret)
				{
					public void customResultAvailable(IComponentIdentifier result)
					{
						setupRemoteTest(SubscriberAgent.class.getName()+".class", "platform", null);
					}
				});
			}
		});
		
		return ret;
		
	}
	
	/**
	 *  Test subscription.
	 */
	public ISubscriptionIntermediateFuture<String>	subscribe()
	{
		final TestReport	report	= new TestReport("#"+reports.size()+1,
			reports.size()==0 ? "Test local automatic subscription termination."
			: reports.size()==1 ? "Test remote automatic subscription termination."
			: "Test remote offline automatic subscription termination.");
		reports.add(report);
		
		System.out.println("test: "+report.getDescription());
		
		waitForRealtimeDelay(10000,
			new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				if(!report.isSucceeded())
				{
					report.setFailed("Termination did not happen.");
					checkFinished();
				}
				return IFuture.DONE;
			}
		});
		
		final SubscriptionIntermediateFuture<String>	ret	= new SubscriptionIntermediateFuture<String>(new TerminationCommand()
		{
			public void terminated(Exception reason)
			{
				if(report.getReason()==null)
				{
					report.setSucceeded(true);
					checkFinished();
				}
			}
		});
		
		agent.waitForDelay(1000, new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				ret.addIntermediateResultIfUndone("ping");
				agent.waitForDelay(1000, this);
				
				return IFuture.DONE;
			}
		});
		
		return ret;
	}
	
	protected void	checkFinished()
	{
		boolean	finished	= reports.size()==3
			&& reports.get(0).isFinished()
			&& reports.get(1).isFinished()
			&& reports.get(2).isFinished();
		
		if(finished)
		{
			agent.setResultValue("testresults", new Testcase(reports.size(), reports.toArray(new TestReport[reports.size()])));
			agent.killComponent();
		}
	}
}
