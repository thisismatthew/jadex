package jadex.adapter.base.clock;

import jadex.bridge.IClock;


/**
 *  An event-driven simulation clock represents a discrete 
 *  clock that is based on a event-list. 
 */
public class SimulationEventClock extends AbstractClock implements ISimulationClock
{
	//-------- constructors --------
	
	/**
	 *  Create a new clock.
	 *  @param name The name.
	 *  @param starttime The start time.
	 *  @param delta The tick size.
	 */
	public SimulationEventClock(String name, long starttime, long delta)
	{
		super(name, starttime, delta);
	}
	
	/**
	 *  Create a new clock.
	 *  @param oldclock The old clock.
	 */
	public SimulationEventClock(IClock oldclock)
	{
		this(null, 0, 1);
		copyFromClock(oldclock);
	}
	
	//-------- methods --------
	
	/**
	 *  Advance one event.
	 *  @return True, if clock could be advanced.
	 */
	public boolean advanceEvent()
	{
		boolean	advanced	= false;
		Timer t = null;
		
		Timer dorem = null;
		synchronized(this)
		{
			if(STATE_RUNNING.equals(state) && timers.size()>0)
			{
				advanced	= true;
				t = (Timer)timers.first();
				long tmptime = t.getNotificationTime();
				
				if(tmptime>currenttime)
					currenttime = tmptime;
					
				//System.out.println("time event notificaton: "+t);
				dorem = t;
//				removeTimer(t);
//				t.getTimedObject().timeEventOccurred();
				//System.out.println("timers remaining: "+timers.size());
			}
		}
		
		if(dorem!=null)
			removeTimer(dorem);
		
		// Must not be done while holding lock to avoid deadlocks.
		if(t!=null)
			t.getTimedObject().timeEventOccurred();
			
		notifyListeners();
		return advanced;
	}
	
	/**
	 *  Get the clock type.
	 *  @return The clock type.
	 */
	public String getType()
	{
		return IClock.TYPE_EVENT_DRIVEN;
	}
}