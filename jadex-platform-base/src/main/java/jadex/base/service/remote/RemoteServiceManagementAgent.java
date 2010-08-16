package jadex.base.service.remote;

import jadex.base.fipa.SFipa;
import jadex.bridge.MessageType;
import jadex.commons.concurrent.DefaultResultListener;
import jadex.commons.concurrent.IResultListener;
import jadex.micro.MicroAgent;
import jadex.service.SServiceProvider;
import jadex.service.library.ILibraryService;
import jadex.xml.bean.JavaReader;
import jadex.xml.bean.JavaWriter;

import java.util.Map;

/**
 *  Remote service management service that hosts the corresponding
 *  service. It basically has the task to forward messages from
 *  remote service management components on other platforms to its service.
 */
public class RemoteServiceManagementAgent extends MicroAgent
{
	//-------- attributes --------
	
	/** The remote management service. */
	protected RemoteServiceManagementService rms;
	
	//-------- constructors --------
	
	/**
	 *  Called once after agent creation.
	 */
	public void agentCreated()
	{
		rms = new RemoteServiceManagementService(getExternalAccess());
		addService(rms);
	}
	
	/**
	 *  Called, whenever a message is received.
	 *  @param msg The message.
	 *  @param mt The message type.
	 */
	public void messageArrived(final Map msg, final MessageType mt)
	{
//		if(msg.toString().indexOf("Shop")!=-1)
//			System.out.println("received: "+msg);
		if(SFipa.MESSAGE_TYPE_NAME_FIPA.equals(mt.getName()))
		{
			SServiceProvider.getService(getServiceProvider(), ILibraryService.class)
				.addResultListener(createResultListener(new IResultListener()
			{
				public void resultAvailable(Object source, Object result)
				{
					// Hack!!! Manual decoding for using custom class loader.
					final ILibraryService ls = (ILibraryService)result;
					Object	content	= msg.get(SFipa.CONTENT);
					if(content instanceof String)
					{
						content = JavaReader.objectFromXML((String)content, ls.getClassLoader());
					}
					
					if(content instanceof IRemoteCommand)
					{
						final IRemoteCommand com = (IRemoteCommand)content;
						com.execute(getExternalAccess(), rms.getWaitingCalls()).addResultListener(createResultListener(new IResultListener()
						{
							public void resultAvailable(Object source, Object result)
							{
		//						System.out.println("result of command: "+com+" "+result);
								if(result!=null)
								{
									final Object repcontent = result;
									createReply(msg, mt).addResultListener(createResultListener(new DefaultResultListener()
									{
										public void resultAvailable(Object source, Object result)
										{
											Map reply = (Map)result;
											reply.put(SFipa.CONTENT, JavaWriter.objectToXML(repcontent, ls.getClassLoader()));
											sendMessage(reply, mt);
										}
									}));
								}
							}
							
							public void exceptionOccurred(Object source, Exception exception)
							{
								// todo: print or send failure reply?
								exception.printStackTrace();
							}
						}));
					}
					else
					{
						System.out.println("Unexpected message: "+msg);
					}
				}
				
				public void exceptionOccurred(Object source, Exception exception)
				{
					// todo: print or send failure reply?
					exception.printStackTrace();
				}
			}));
		}
	}
}
