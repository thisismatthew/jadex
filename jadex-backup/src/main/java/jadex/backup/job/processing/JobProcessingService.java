package jadex.backup.job.processing;

import jadex.backup.job.Job;
import jadex.backup.job.SyncJob;
import jadex.backup.job.Task;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.bridge.service.annotation.ServiceShutdown;
import jadex.bridge.service.annotation.ServiceStart;
import jadex.commons.IFilter;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.ITerminationCommand;
import jadex.commons.future.SubscriptionIntermediateFuture;
import jadex.micro.IPojoMicroAgent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */
@Service
public class JobProcessingService implements IJobProcessingService
{
	//-------- attributes --------

	/** The agent. */
	@ServiceComponent
	protected IInternalAccess agent;
	
	/** The pojo agent. */
	protected SyncJobProcessingAgent pojoagent;
	
	/** The futures of active subscribers. */
	protected Map<SubscriptionIntermediateFuture<AJobProcessingEvent>, IFilter<AJobProcessingEvent>> subscribers;
	
	//-------- constructors --------
	
	/**
	 *  Called on startup.
	 */
	@ServiceStart
	public IFuture<Void> start()
	{
		pojoagent = (SyncJobProcessingAgent)((IPojoMicroAgent)agent).getPojoAgent();
		return IFuture.DONE;
	}
	
	/**
	 *  Called on shutdown.
	 */
	@ServiceShutdown
	public IFuture<Void> shutdown()
	{
		if(subscribers!=null)
		{
			for(SubscriptionIntermediateFuture<AJobProcessingEvent> fut: subscribers.keySet())
			{
				fut.terminate();
			}
		}
		
		return IFuture.DONE;
	}
	
	//-------- methods --------
	
	/**
	 *  Get the job id.
	 *  
	 *  The id is static, i.e. it does not change during
	 *  the lifetime of the job agent.
	 */
	public String	getJobId()
	{
		return pojoagent.getJob().getId();
	}

	/**
	 *  Get the job of this service.
	 */
	public IFuture<Job> getJob()
	{
		return new Future<Job>(pojoagent.getJob());
	}
	
	/**
	 *  Modify a job.
	 *  @param job The job.
	 */
	public IFuture<Void> modifyJob(Job job)
	{
		pojoagent.jobModified((SyncJob)job);
				
		return IFuture.DONE;
	}
	
	/**
	 *  Modify a task.
	 *  @param task The task.
	 */
	public IFuture<Void> modifyTask(Task task)
	{
		pojoagent.taskModified(task);
		
		return IFuture.DONE;
	}

	
	/**
	 *  Subscribe for job news.
	 */
	public ISubscriptionIntermediateFuture<AJobProcessingEvent> subscribe(IFilter<AJobProcessingEvent> filter)
	{
		if(subscribers==null)
		{
			subscribers	= new LinkedHashMap<SubscriptionIntermediateFuture<AJobProcessingEvent>, IFilter<AJobProcessingEvent>>();
		}
		
		final SubscriptionIntermediateFuture<AJobProcessingEvent> ret = new SubscriptionIntermediateFuture<AJobProcessingEvent>();
		ret.setTerminationCommand(new ITerminationCommand()
		{
			public boolean checkTermination(Exception reason)
			{
				return true;
			}
			
			public void	terminated(Exception reason)
			{
				subscribers.remove(ret);
			}
		});
		subscribers.put(ret, filter);
		
		// Immediately send first event to ensure subscription
		ret.addIntermediateResult(new JobProcessingEvent(JobProcessingEvent.INITIAL, pojoagent.getJob()));
		
		return ret;		
	}
	
	/**
	 *  Publish an event to all subscribers.
	 *  @param event The event.
	 */
	protected void publishEvent(AJobProcessingEvent event)
	{
		if(subscribers!=null)
		{
			for(SubscriptionIntermediateFuture<AJobProcessingEvent> sub: subscribers.keySet())
			{
				IFilter<AJobProcessingEvent> fil = subscribers.get(sub);
				if(fil==null || fil.filter(event))
				{
					sub.addIntermediateResult(event);
				}
			}
		}
	}
}
