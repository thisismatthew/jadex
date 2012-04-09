package jadex.bridge.service.types.remote;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInputConnection;
import jadex.bridge.IOutputConnection;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

/**
 * 
 */
public class ServiceOutputConnectionProxy implements IOutputConnection
{
	/** The original connection. */
	protected ServiceInputConnection con;
	
	/** The connection id. */
	protected int conid;
	
	/**
	 * 
	 */
	public ServiceOutputConnectionProxy()
	{
		// Bean constructor.
	}
	
	/**
	 * 
	 */
	public ServiceOutputConnectionProxy(ServiceInputConnection con)
	{
		this.con = con;
	}
	
	/**
	 * 
	 */
	public void setInputConnection(IInputConnection icon)
	{
		con.setInputConnection(icon);
	}
	
	/**
	 *  Get the connectionid.
	 *  @return The connectionid.
	 */
	public int getConnectionId()
	{
		return conid;
	}

	/**
	 *  Set the connectionid.
	 *  @param connectionid The connectionid to set.
	 */
	public void setConnectionId(int conid)
	{
		this.conid = conid;
	}
	
	
	
	/**
	 *  Write the content to the stream.
	 *  @param data The data.
	 */
	public IFuture<Void> write(byte[] data)
	{
		return new Future<Void>(new UnsupportedOperationException());
	}
	
	/**
	 *  Flush the data.
	 */
	public void flush()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Close the connection.
	 */
	// todo: make IFuture<Void> ?
	public void close()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 */
	public IComponentIdentifier getInitiator()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 */
	public IComponentIdentifier getParticipant()
	{
		throw new UnsupportedOperationException();
	}
}
