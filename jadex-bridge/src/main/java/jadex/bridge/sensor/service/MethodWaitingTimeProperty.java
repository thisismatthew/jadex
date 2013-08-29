package jadex.bridge.sensor.service;

import jadex.bridge.IInternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.commons.MethodInfo;
import jadex.commons.future.IFuture;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 *  Property for the waiting time of a method.
 */
public class MethodWaitingTimeProperty extends TimedProperty
{
	/** The name of the property. */
	public static final String WAITINGTIME = "waiting time";
	
	/** The handler. */
	protected BasicServiceInvocationHandler handler;
	
	/** The listener. */
	protected IMethodInvocationListener listener;
	
	/** The method info. */
	protected MethodInfo method;
	
	/**
	 *  Create a new property.
	 */
	public  MethodWaitingTimeProperty(IInternalAccess comp, IService service, MethodInfo method)
	{
		super(WAITINGTIME, comp, -1);
		this.method = method;
		
		if(Proxy.isProxyClass(service.getClass()))
		{
			handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
			listener = new IMethodInvocationListener()
			{
				Map<Long, Long> times = new HashMap<Long, Long>();
				
				public void methodCallStarted(Object proxy, Object[] args, long callid)
				{
					times.put(new Long(callid), new Long(System.currentTimeMillis()));
				}
				
				public void methodCallFinished(Object proxy, Object[] args, long callid)
				{
					Long start = times.remove(new Long(callid));
					long dur = System.currentTimeMillis() - start.longValue();
					setValue(dur);
				}
			};
			handler.addMethodListener(method, listener);
		}
		else
		{
			System.out.println("Warning, cannot install property callback on service: "+getName());
		}
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		return null;
//		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Set the value.
	 */
	public void setValue(Long value) 
	{
		// ema calculatio: EMAt = EMAt-1 +(SF*(Ct-EMAt-1)) SF=2/(n+1)
		if(this.value!=null && value!=null)
		{
			double sf = 2d/(10d+1); // 10 periods per default
			double delta = value-this.value;
			value = new Long((long)(this.value+sf*delta));
		}
		
		if(value!=null)
		{
			System.out.println("Setting value: "+value);
			super.setValue((long)value);
		}
	}
	
	/**
	 *  Property was removed and should be disposed.
	 */
	public IFuture<Void> dispose()
	{
		if(handler!=null)
			handler.removeMethodListener(method, listener);
		return IFuture.DONE;
	}
}
