package jadex.adapter.standalone;

import jadex.adapter.base.DefaultResultListener;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentIdentifier;
import jadex.commons.concurrent.IResultListener;
import jadex.commons.concurrent.IThreadPool;
import jadex.service.PropertyServiceContainer;
import jadex.service.clock.IClockService;
import jadex.service.clock.ITimedObject;
import jadex.service.clock.ITimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 *  Abstract base class for standalone platform.
 */
public abstract class AbstractPlatform extends PropertyServiceContainer
{
	//-------- constants --------

	/** The configuration file. */
	public static final String CONFIGURATION = "conf";

	/** The maximum shutdown time. */
	public static final long MAX_SHUTDOWM_TIME = 3000;

	//-------- attributes --------

	/** The optional system components (ams, df). */
	protected Set daemoncomponents;

	/** The logger. */
	protected Logger logger;

	/** The shutdown flag. */
	protected boolean shuttingdown;

	/** The shutdown time. */
	protected long shutdowntime;
	
	/** The threadpool. */
	protected IThreadPool threadpool;

	//-------- methods --------
		
	/**
	 *  Check if the platform is currently shutting down.
	 */
	public boolean isShuttingDown() // todo: make protected?
	{
		return shuttingdown;
	}

	/**
	 *  Get the platform logger.
	 *  @return The platform logger.
	 */
	public Logger getLogger()
	{
		return logger;
	}

	/**
	 *  Shutdown the platform.
	 */
	public void shutdown(IResultListener rl)
	{
		if(rl==null)
			rl	= DefaultResultListener.getInstance();
		final IResultListener	listener	= rl;
		
		//System.out.println("Shutting down the platform: "+getName());
		// Hack !!! Should be synchronized with CES.
		synchronized(this)
		{
			if(shuttingdown)
				return;

			this.shuttingdown = true;
		}
		
		// Step 1: Find existing components.
		final IComponentManagementService	ces	= (IComponentManagementService)getService(IComponentManagementService.class);
		ces.getComponentIdentifiers(new IResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
				// Step 2: Kill existing components excepts daemons.
				final List comps = new ArrayList(Arrays.asList((IComponentIdentifier[])result));
				for(Iterator it=daemoncomponents.iterator(); it.hasNext(); )
				{
					comps.remove(it.next());
				}
				killComponents(comps, shutdowntime!=0 ? shutdowntime : MAX_SHUTDOWM_TIME, new IResultListener()
				{
					public void resultAvailable(Object source, Object result)
					{
						// Step 3: Find remaining components.
						ces.getComponentIdentifiers(new IResultListener()
						{
							public void resultAvailable(Object source, Object result)
							{
								// Step 4: Kill remaining components.
								killComponents(Arrays.asList((IComponentIdentifier[])result), shutdowntime!=0 ? shutdowntime : MAX_SHUTDOWM_TIME, new IResultListener()
								{
									public void resultAvailable(Object source, Object result)
									{
										// Step 5: Stop the services.
										AbstractPlatform.super.shutdown(listener);
									}
									public void exceptionOccurred(Object source, Exception exception)
									{
										listener.exceptionOccurred(source, exception);
									}
								});
							}

							public void exceptionOccurred(Object source, Exception exception)
							{
								listener.exceptionOccurred(source, exception);
							}
						});		
					}
					
					public void exceptionOccurred(Object source, Exception exception)
					{
						listener.exceptionOccurred(source, exception);
					}
				});
			}

			public void exceptionOccurred(Object source, Exception exception)
			{
				listener.exceptionOccurred(source, exception);
			}
		});		
	}
	
	/**
	 *  Create a component.
	 */
	protected void createComponent(String name, String model, String config, Map args, final boolean daemon)
	{
		IComponentManagementService	ces	= (IComponentManagementService)getService(IComponentManagementService.class);
		ces.createComponent(name, model, config, args, false, null, null, null, false);
	}

	/**
	 *  Kill the given components within the specified timeout.
	 *  @param comps	The component ids.
	 *  @param timeout	The time after which to inform the listener anyways.
	 *  @param listener	The result listener.
	 */
	protected void killComponents(final List comps, long timeout, final IResultListener listener)
	{
		if(comps.isEmpty())
			listener.resultAvailable(this, null);
		
		// Timer entry to notify lister after timeout.
		final	boolean	notified[]	= new boolean[1];
		IClockService clock	= (IClockService)getService(IClockService.class);
		final ITimer	killtimer	= clock.createTimer(timeout, new ITimedObject()
		{
			public void timeEventOccurred(long currenttime)
			{
				boolean	notify	= false;
				synchronized(notified)
				{
					if(!notified[0])
					{
						notify	= true;
						notified[0]	= true;
					}
				}
				if(notify)
				{
					listener.resultAvailable(this, null);
				}
			}
		});
		
		// Kill the given components.
		IResultListener	rl	= new IResultListener()
		{
			int cnt	= 0;
			public void resultAvailable(Object source, Object result)
			{
				testFinished();
			}
			public void exceptionOccurred(Object source, Exception exception)
			{
				testFinished();
			}
			protected synchronized void testFinished()
			{
				cnt++;
				if(cnt==comps.size())
				{
					killtimer.cancel();
					boolean	notify	= false;
					synchronized(notified)
					{
						if(!notified[0])
						{
							notify	= true;
							notified[0]	= true;
						}
					}
					if(notify)
					{
						listener.resultAvailable(this, null);
					}
				}
			}
		};
		IComponentManagementService	ces	= (IComponentManagementService)getService(IComponentManagementService.class);
		for(int i=0; i < comps.size(); i++)
		{
			//System.out.println("Killing component: "+comps.get(i));
			ces.destroyComponent((IComponentIdentifier)comps.get(i), rl);
		}
	}

}
