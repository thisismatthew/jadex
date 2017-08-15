package jadex.bdiv3x.features;

import jadex.bdiv3x.runtime.RMessageEvent;

/**
 *  Allow (de-)registering messages for capabilities.
 */
public interface IInternalBDIXMessageFeature
{
	/**
	 *  Register a conversation or reply-with to be able
	 *  to send back answers to the source capability.
	 *  @param msgevent The message event.
	 */
	public <T> void registerMessageEvent(RMessageEvent<T> msgevent);
	
	/**
	 *  Deregister a conversation or reply-with.
	 *  @param msgevent The message event.
	 */
	public <T> void deregisterMessageEvent(RMessageEvent<T> msgevent);
}
