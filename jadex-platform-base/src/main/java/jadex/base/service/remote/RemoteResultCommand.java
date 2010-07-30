package jadex.base.service.remote;

import java.util.Map;

import jadex.bridge.IExternalAccess;
import jadex.commons.Future;
import jadex.commons.IFuture;

/**
 *  Command that represents the result(s) of a remote command.
 *  Notifies the caller about the result.
 */
public class RemoteResultCommand implements IRemoteCommand
{
	//-------- attributes --------
	
	/** The result. */
	protected Object result;

	/** The exception. */
	protected Exception exception;
	
	/** The callid. */
	protected String callid;
	
	//-------- constructors --------
	
	/**
	 * 
	 */
	public RemoteResultCommand()
	{
	}

	/**
	 *  Create a new remote result command.
	 */
	public RemoteResultCommand(Object result, Exception exception, String callid)
	{
		this.result = result;
		this.exception = exception;
		this.callid = callid;
	}
	
	//-------- methods --------
	
	/**
	 *  Execute the command.
	 *  @param lrms The local remote management service.
	 *  @return An optional result command that will be 
	 *  sent back to the command origin. 
	 */
	public IFuture execute(IExternalAccess component, Map waitingcalls)
	{
		Future future = (Future)waitingcalls.get(callid);
		if(exception!=null)
		{
			future.setException(exception);
		}
		else
		{
			future.setResult(result);
		}
		return new Future(null);
	}
	
	//-------- getter/setter methods --------
	
	/**
	 *  Get the result.
	 *  @return the result.
	 */
	public Object getResult()
	{
		return result;
	}

	/**
	 *  Set the result.
	 *  @param result The result to set.
	 */
	public void setResult(Object result)
	{
		this.result = result;
	}
	
	/**
	 *  Get the exception.
	 *  @return the exception.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 *  Set the exception.
	 *  @param exception The exception to set.
	 */
	public void setException(Exception exception)
	{
		this.exception = exception;
	}
	
	/**
	 *  Get the callid.
	 *  @return the callid.
	 */
	public String getCallId()
	{
		return callid;
	}

	/**
	 *  Set the callid.
	 *  @param callid The callid to set.
	 */
	public void setCallId(String callid)
	{
		this.callid = callid;
	}
}
