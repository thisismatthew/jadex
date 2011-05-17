package jadex.base.service.remote;

import jadex.base.fipa.SFipa;
import jadex.base.service.remote.commands.RemoteGetExternalAccessCommand;
import jadex.base.service.remote.commands.RemoteSearchCommand;
import jadex.base.service.remote.xml.RMIPostProcessor;
import jadex.base.service.remote.xml.RMIPreProcessor;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IMessageService;
import jadex.bridge.IRemoteServiceManagementService;
import jadex.bridge.service.AnyResultSelector;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IResultSelector;
import jadex.bridge.service.ISearchManager;
import jadex.bridge.service.IVisitDecider;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.bridge.service.ServiceNotFoundException;
import jadex.bridge.service.TypeResultSelector;
import jadex.bridge.service.clock.IClockService;
import jadex.bridge.service.clock.ITimer;
import jadex.bridge.service.library.ILibraryService;
import jadex.commons.IRemotable;
import jadex.commons.SUtil;
import jadex.commons.Tuple;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.IMicroExternalAccess;
import jadex.micro.MicroAgent;
import jadex.xml.ObjectInfo;
import jadex.xml.SXML;
import jadex.xml.TypeInfo;
import jadex.xml.XMLInfo;
import jadex.xml.annotation.XMLClassname;
import jadex.xml.bean.BeanObjectReaderHandler;
import jadex.xml.bean.BeanObjectWriterHandler;
import jadex.xml.bean.JavaReader;
import jadex.xml.bean.JavaWriter;
import jadex.xml.reader.ReadContext;
import jadex.xml.reader.Reader;
import jadex.xml.writer.Writer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

/**
 *  The remote service management service is responsible for 
 *  handling remote service invocations (similar to RMI).
 */
public class RemoteServiceManagementService extends BasicService implements IRemoteServiceManagementService
{
	//-------- constants --------
	
	/** Excluded remote methods (for all methods)
	 *	Excluded methods throw UnsupportedOperationException. */
	public static String REMOTE_EXCLUDED = "remote_excluded";
	
	/** Uncached remote methods (for methods with no parameters)
	 *	Uncached methods will be invoked on every call. */
	public static String REMOTE_UNCACHED = "remote_uncached";
	
	/** Synchronous remote methods (for methods with void return value). 
     *	If void methods are declared synchronous they will block the caller until
     *	the method has been executed on the remote side (exception thus can arrive). */
	public static String REMOTE_SYNCHRONOUS = "remote_synchronous";

	/** Replacement methods to be executed instead of remote method invocation. */
	public static String REMOTE_METHODREPLACEMENT = "remote_methodreplacement";

	/** Timeout for remote method invocation. */
	public static String REMOTE_TIMEOUT = "remote_timeout";

	/** The default timeout. */
	public static long DEFAULT_TIMEOUT = 10000;
		
	//-------- attributes --------
	
	/** The component. */
	protected IMicroExternalAccess component;
	
	/** The map of waiting calls (callid -> future). */
	protected Map waitingcalls;

	/** The remote reference module. */
	protected RemoteReferenceModule rrm;
	
	/** The rmi object to xml writer. */
	protected Writer writer;
	
	/** The rmi xml to object reader. */
	protected Reader reader;
		
	//-------- constructors --------
	
	/**
	 *  Create a new remote service management service.
	 */
	public RemoteServiceManagementService(IMicroExternalAccess component, IClockService clock, ILibraryService libservice)
	{
		super(component.getServiceProvider().getId(), IRemoteServiceManagementService.class, null);

		this.component = component;
		this.rrm = new RemoteReferenceModule(this, clock, libservice);
		this.waitingcalls = new HashMap();
		
		QName[] pr = new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"jadex.base.service.remote", "ProxyReference")};
		
		Set typeinfosread = JavaReader.getTypeInfos();
		TypeInfo ti_rr = new TypeInfo(new XMLInfo(pr), 
			new ObjectInfo(ProxyReference.class, new RMIPostProcessor(rrm)));
		typeinfosread.add(ti_rr);
		
		Set typeinfoswrite = JavaWriter.getTypeInfos();
		TypeInfo ti_proxyable = new TypeInfo(new XMLInfo(pr, null, false, new RMIPreProcessor(rrm)), 
			new ObjectInfo(IRemotable.class));
		typeinfoswrite.add(ti_proxyable);
		
		this.reader = new Reader(new BeanObjectReaderHandler(typeinfosread), false, false, new XMLReporter()
		{
			public void report(String message, String error, Object info, Location location)
				throws XMLStreamException
			{
				List	errors	= (List)((ReadContext)Reader.READ_CONTEXT.get()).getUserContext();
				errors.add(new Tuple(new Object[]{message, error, info, location}));
			}
		});
		this.writer = new Writer(new BeanObjectWriterHandler(typeinfoswrite, true));
	}
	
	//-------- methods --------
	
	/**
	 *  Get a service proxies from a remote platform.
	 *  (called from arbitrary components)
	 *  @param cid Component id that is used to start the search.
	 *  @param manager The search manager.
	 *  @param decider The visit decider.
	 *  @param selector The result selector.
	 *  @return Collection or single result (i.e. service proxies). 
	 */
	public IFuture getServiceProxies(final IComponentIdentifier cid, 
		final ISearchManager manager, final IVisitDecider decider, final IResultSelector selector)
	{
		Future ret = new Future();
		
		component.scheduleStep(new IComponentStep()
		{
			@XMLClassname("getServiceProxies")
			public Object execute(IInternalAccess ia)
			{
				RemoteServiceManagementAgent agent = (RemoteServiceManagementAgent)ia;
				final Future fut = new Future();
				SServiceProvider.getService(component.getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(agent.createResultListener(new IResultListener()
				{
					public void resultAvailable(Object result)
					{
						IComponentManagementService cms = (IComponentManagementService)result;
						
						// Hack! create remote rms cid with "rms" assumption.
						IComponentIdentifier rrms = cms.createComponentIdentifier("rms@"+cid.getPlatformName(), false, cid.getAddresses());
						final String callid = SUtil.createUniqueId(component.getComponentIdentifier().getLocalName());
						RemoteSearchCommand content = new RemoteSearchCommand(cid, manager, 
							decider, selector, callid);
						
						sendMessage(rrms, content, callid, -1, fut);
					}
					public void exceptionOccurred(Exception exception)
					{
						fut.setException(exception);
					}
				}));
				
				return fut;
			}
		}).addResultListener(new DelegationResultListener(ret));
		
		return ret;
	}
	
	/**
	 *  Get a service proxy from a remote component.
	 *  (called from arbitrary components)
	 *  @param cid	The remote provider id.
	 *  @param service	The service type.
	 *  @param scope	The search scope. 
	 *  @return The service proxy.
	 */
	public IFuture getServiceProxy(final IComponentIdentifier cid, final Class service, String scope)
	{
		Future	ret	= new Future();
		getServiceProxies(cid, SServiceProvider.getSearchManager(false, scope), SServiceProvider.getVisitDecider(true, scope), 
			new TypeResultSelector(service, true)).addResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				if(result!=null && !((Collection)result).isEmpty())
					super.customResultAvailable(((Collection)result).iterator().next());
				else
					super.exceptionOccurred(new ServiceNotFoundException("No proxy for service found: "+cid+", "+service.getName()));
			}
		});
		return ret;
	}
	
	/**
	 *  Get all service proxies from a remote component.
	 *  (called from arbitrary components)
	 *  @param cid	The remote provider id.
	 *  @param service	The service type.
	 *  @param scope	The search scope. 
	 *  @return The service proxy.
	 */
	public IFuture getServiceProxies(IComponentIdentifier cid, Class service, String scope)
	{
		return getServiceProxies(cid, SServiceProvider.getSearchManager(true, scope), SServiceProvider.getVisitDecider(false, scope), 
			new TypeResultSelector(service, true));
	}
	
	/**
	 *  Get all declared service proxies from a remote component.
	 *  (called from arbitrary components)
	 *  @param cid The remote provider id.
	 *  @param service The service type.
	 *  @return The service proxy.
	 */
	public IFuture getDeclaredServiceProxies(IComponentIdentifier cid)
	{
		return getServiceProxies(cid, SServiceProvider.localmanager, SServiceProvider.contdecider, 
			new AnyResultSelector(false, false));
	}
	
	/**
	 *  Get an external access proxy from a remote component.
	 *  (called from arbitrary components)
	 *  @param cid Component target id.
	 *  @return External access of remote component. 
	 */
	public IFuture getExternalAccessProxy(final IComponentIdentifier cid)
	{
		final Future ret = new Future();
		
		component.scheduleStep(new IComponentStep()
		{
			@XMLClassname("getExternalAccessProxy")
			public Object execute(IInternalAccess ia)
			{
				final Future fut = new Future();
				RemoteServiceManagementAgent agent = (RemoteServiceManagementAgent)ia;
				SServiceProvider.getService(component.getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(agent.createResultListener(new IResultListener()
				{
					public void resultAvailable(Object result)
					{
						IComponentManagementService cms = (IComponentManagementService)result;
						
						// Hack! create remote rms cid with "rms" assumption.
						IComponentIdentifier rrms = cms.createComponentIdentifier("rms@"+cid.getPlatformName(), false, cid.getAddresses());
						final String callid = SUtil.createUniqueId(component.getComponentIdentifier().getLocalName());
						RemoteGetExternalAccessCommand content = new RemoteGetExternalAccessCommand(cid, callid);
						
						sendMessage(rrms, content, callid, -1, fut);
					}
					public void exceptionOccurred(Exception exception)
					{
						fut.setException(exception);
					}
				}));
				
				return fut;
			}
		}).addResultListener(new DelegationResultListener(ret));
		
		return ret;
	}

	/**
	 *  Get the component.
	 *  @return the component.
	 */
	public IMicroExternalAccess getComponent()
	{
		return component;
	}
	
	/**
	 *  Get the rms component identifier.
	 *  @return The rms component identifier.
	 */
	public IComponentIdentifier getRMSComponentIdentifier()
	{
		return component.getComponentIdentifier();
	}
	
	/**
	 *  Get the waiting calls.
	 *  @return the waiting calls.
	 * /
	public Map getWaitingCalls()
	{
		return waitingcalls;
	}*/
	
	/**
	 *  Get the reader.
	 *  @return The reader.
	 */
	public Reader getReader()
	{
		return reader;
	}

	/**
	 *  Get the writer.
	 *  @return the writer.
	 */
	public Writer getWriter()
	{
		return writer;
	}
	
	/**
	 *  Add a new waiting call.
	 *  @param callid The callid.
	 *  @param future The future.
	 */
	public void putWaitingCall(String callid, Future future)
	{
		getRemoteReferenceModule().checkThread();
		waitingcalls.put(callid, future);
	}
	
	/**
	 *  Get a waiting call future.
	 *  @param callid The callid.
	 *  @return The future.
	 */
	public Future getWaitingCall(String callid)
	{
		getRemoteReferenceModule().checkThread();
		return (Future)waitingcalls.get(callid);
	}
	
	/**
	 *  Remove a waiting call.
	 *  @param callid The callid.
	 *  @return The future.
	 */
	public Future removeWaitingCall(String callid)
	{
		getRemoteReferenceModule().checkThread();
		return (Future)waitingcalls.remove(callid);
	}
	
	/**
	 *  Get the remote reference module.
	 *  @return the rrm.
	 */
	public RemoteReferenceModule getRemoteReferenceModule()
	{
		return rrm;
	}
	
//	protected static Map errors = Collections.synchronizedMap(new LRU(200));
//	public Map interestingcalls = new HashMap();
	/**
	 *  Send the request message of a remote method invocation.
	 *  (Can savely be called from any thread).
	 */
	public void sendMessage(final IComponentIdentifier receiver, final Object content,
		final String callid, final long to, final Future future)
	{
		component.scheduleStep(new IComponentStep()
		{
			@XMLClassname("sendMessage")
			public Object execute(final IInternalAccess ia)
			{
				final long timeout = to<=0? DEFAULT_TIMEOUT: to;
				
				putWaitingCall(callid, future);
//				System.out.println("Waitingcalls: "+waitingcalls.size());
				
				final Map msg = new HashMap();
				msg.put(SFipa.SENDER, component.getComponentIdentifier());
				msg.put(SFipa.RECEIVERS, new IComponentIdentifier[]{receiver});
				msg.put(SFipa.CONVERSATION_ID, callid);
//				msg.put(SFipa.LANGUAGE, SFipa.JADEX_XML);
				
				SServiceProvider.getService(component.getServiceProvider(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(ia.createResultListener(new IResultListener()
				{
					public void resultAvailable(Object result)
					{
						final ILibraryService ls = (ILibraryService)result;
						
						SServiceProvider.getService(component.getServiceProvider(), IMessageService.class, RequiredServiceInfo.SCOPE_PLATFORM)
							.addResultListener(ia.createResultListener(new IResultListener()
						{
							public void resultAvailable(Object result)
							{
								// Hack!!! Manual encoding for using custom class loader at receiver side.
//								msg.put(SFipa.CONTENT, JavaWriter.objectToXML(content, ls.getClassLoader()));
								
//								System.out.println("sent: "+callid);
//								System.out.println("RMS sending to: "+receiver+", "+(content!=null?SReflect.getClassName(content.getClass()):null));
								
								String cont = Writer.objectToXML(getWriter(), content, ls.getClassLoader(), receiver);
								msg.put(SFipa.CONTENT, cont);
								
//								if(cont.indexOf("getServices")!=-1)
//								{
//									interestingcalls.put(callid, cont);
//								}
								
								IMessageService ms = (IMessageService)result;
								ms.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE, component.getComponentIdentifier(), ls.getClassLoader(), null)
									.addResultListener(ia.createResultListener(new IResultListener()
								{
									public void resultAvailable(Object result)
									{
										// ok message could be sent.
										component.scheduleStep(new IComponentStep()
										{
											@XMLClassname("oksent")
											public Object execute(final IInternalAccess ia)
											{
//												System.out.println("waitfor");
												MicroAgent pa = (MicroAgent)ia;
												pa.waitFor(timeout, new IComponentStep()
												{
													public Object execute(IInternalAccess ia)
													{
//														System.out.println("timeout triggered: "+msg);
														removeWaitingCall(callid);
//														waitingcalls.remove(callid);
//														System.out.println("Waitingcalls: "+waitingcalls.size());
														future.setExceptionIfUndone(new RuntimeException("No reply received and timeout occurred: "+callid)
														{
															public void printStackTrace()
															{
																Thread.dumpStack();
																super.printStackTrace();
															}
														});
														return null;
													}
												}).addResultListener(ia.createResultListener(new DefaultResultListener()
												{
													public void resultAvailable(Object result)
													{
														// cancel timer when future is finished before. 
														final ITimer timer = (ITimer)result;
														future.addResultListener(ia.createResultListener(new IResultListener()
														{
															public void resultAvailable(Object result)
															{
																removeWaitingCall(callid);
//																waitingcalls.remove(callid);
//																System.out.println("Waitingcalls: "+waitingcalls.size());
//																System.out.println("Cancel timeout (res): "+callid+" "+future);
//																errors.put(callid, new Object[]{"Cancel timeout (res)", result});
																timer.cancel();
															}
															
															public void exceptionOccurred(Exception exception)
															{
//																exception.printStackTrace();
																removeWaitingCall(callid);
//																waitingcalls.remove(callid);
//																System.out.println("Waitingcalls: "+waitingcalls.size());
//																System.out.println("Cancel timeout (ex): "+callid+" "+future);
//																errors.put(callid, new Object[]{"Cancel timeout (ex):", exception});
																timer.cancel();
																ia.getLogger().warning("Remote request failed: "+content+"\n"+exception.getMessage());
															}
														}));
													}
												}));
												
												return null;
											}
										});
									}
									
									public void exceptionOccurred(Exception exception)
									{
										// message could not be sent -> fail immediately.
//										System.out.println("Callee could not be reached: "+exception);
//										errors.put(callid, new Object[]{"Callee could not be reached", exception});
										future.setException(exception);
										removeWaitingCall(callid);
//										waitingcalls.remove(callid);
//										System.out.println("Waitingcalls: "+waitingcalls.size());
									}
								}));
							}
							
							public void exceptionOccurred(Exception exception)
							{
//								System.out.println("Message service not found: "+exception);
//								errors.put(callid, new Object[]{"No msg service", exception});
								removeWaitingCall(callid);
//								waitingcalls.remove(callid);
//								System.out.println("Waitingcalls: "+waitingcalls.size());
								future.setException(exception);
							}
						}));
					}
					
					public void exceptionOccurred(Exception exception)
					{
//						System.out.println("Library service not found: "+exception);
//						errors.put(callid, new Object[]{"No lib service", exception});
						removeWaitingCall(callid);
//						waitingcalls.remove(callid);
//						System.out.println("Waitingcalls: "+waitingcalls.size());
						future.setException(exception);
					}
				}));
				
				return null;
			}
		});
	}
}

