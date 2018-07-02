package jadex.bridge.service.types.registry;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jadex.bridge.IComponentIdentifier;

/**
 *  The abstract registry event.
 */
public abstract class ARegistryEvent
{	
	/** Constants for identifying the different kinds of registry clients. */
	public static final String CLIENTTYPE_CLIENT = "client";
	public static final String CLIENTTYPE_SUPERPEER_LEVEL0 = "superpeer_0";
	public static final String CLIENTTYPE_SUPERPEER_LEVEL1 = "superpeer_1";
	
	/** The number of events that must have occured before a remote message is sent. */
	protected int eventslimit;
	
	/** The timestamp of the first event (change). */
	protected long timestamp; 
	
	/** The time limit. */
	protected long timelimit;
	
	/** The clients. */
	protected Set<IComponentIdentifier> clients;
	
	/** The sender. */
	protected IComponentIdentifier sender;
	
	//-------- for superpeer supersuperpeer interaction --------
	
	/** The client type (client, superpeer_1, superpeer_0, ...). */
	protected String clienttype;
	
	/** The network names. */
	protected String[] networknames;

	/** Identity code. */
	protected String id = ""+ai.incrementAndGet();
	
	protected static AtomicInteger ai = new AtomicInteger();	
		
	/**
	 *  Create a new event.
	 *  @param eventslimit
	 *  @param timestamp
	 *  @param timelimit
	 */
	public ARegistryEvent()
	{
	}
	
	/**
	 *  Create a new event.
	 *  @param eventslimit
	 *  @param timestamp
	 *  @param timelimit
	 */
	public ARegistryEvent(int eventslimit, long timelimit)
	{
		this.eventslimit = eventslimit;
		this.timelimit = timelimit;
	}

	/**
	 * Returns the number of elements added to this event.
	 */
	public abstract int size();
	
	/**
	 *  Check if this event is due and should be sent.
	 *  @param True, if the event is due and should be sent.
	 */
	public boolean isDue()
	{
		int size = size();
		// Send event if more than eventlimit events have been collected
		// OR
		// timeout has been reached (AND and at least one event has been collected)
		// The last aspect is not used because lease times are used
		// so an empty event at least renews the lease
		return size>=getEventslimit() || (System.currentTimeMillis()-getTimestamp()>=getTimelimit());// && size>0);
	}

	/**
	 *  Get the time until this event is due.
	 *  @return The time until the event is due.
	 */
	public long getTimeUntilDue()
	{
		long wait = timelimit-(System.currentTimeMillis()-timestamp);
		return wait>0? wait: 0;
	}

	/**
	 *  Get the eventslimit.
	 *  @return The eventslimit
	 */
	public int getEventslimit()
	{
		return eventslimit;
	}

	/**
	 *  Get the timestamp.
	 *  @return The timestamp
	 */
	public long getTimestamp()
	{
		return timestamp;
	}

	/**
	 *  Get the timelimit.
	 *  @return The timelimit
	 */
	public long getTimelimit()
	{
		return timelimit;
	}
	
	/**
	 *  Get the clients.
	 *  @return The clients
	 */
	public Set<IComponentIdentifier> getClients()
	{
		return clients;
	}

	/**
	 *  Set the clients.
	 *  @param clients the clients to set
	 */
	public void setClients(Set<IComponentIdentifier> clients)
	{
		this.clients = clients;
	}
	
	/**
	 *  Add a client.
	 *  @param client The client.
	 */
	public void addClient(IComponentIdentifier client)
	{
		if(clients==null)
			clients = new HashSet<IComponentIdentifier>();
		clients.add(client);
	}

	/**
	 *  Get the sender.
	 *  @return The sender.
	 */
	public IComponentIdentifier getSender()
	{
		return sender;
	}

	/**
	 *  Set the sender.
	 *  @param sender The sender to set.
	 */
	public void setSender(IComponentIdentifier sender)
	{
		this.sender = sender;
	}
	
	/**
	 *  Get the client type.
	 *  @return The client type.
	 */
	public String getClientType()
	{
		return clienttype;
	}

	/**
	 *  Set the client type.
	 *  @param clienttype The client type to set.
	 */
	public void setClientType(String clienttype)
	{
		this.clienttype = clienttype;
	}
	
	/**
	 *  Get the networknames.
	 *  @return The networknames.
	 */
	public String[] getNetworkNames()
	{
		return networknames;
	}

	/**
	 *  Set the network names.
	 *  @param networknames The networknames to set.
	 */
	public void setNetworkNames(String[] networknames)
	{
		this.networknames = networknames;
	}

	/**
	 * @return the id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id)
	{
		this.id = id;
	}
}