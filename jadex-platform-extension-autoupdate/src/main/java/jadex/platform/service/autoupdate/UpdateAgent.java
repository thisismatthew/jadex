package jadex.platform.service.autoupdate;

import jadex.base.Starter;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.VersionInfo;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.types.chat.IChatGuiService;
import jadex.bridge.service.types.chat.IChatService;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.daemon.IDaemonService;
import jadex.bridge.service.types.daemon.StartOptions;
import jadex.bridge.service.types.email.Email;
import jadex.bridge.service.types.email.EmailAccount;
import jadex.bridge.service.types.email.IEmailService;
import jadex.bridge.service.types.library.IDependencyService;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentService;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.ComponentType;
import jadex.micro.annotation.ComponentTypes;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.xml.bean.JavaWriter;
import jadex.xml.writer.AWriter;
import jadex.xml.writer.XMLWriterFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *  The update agent can be used to restart the platform with a newer version.
 */
@Agent
@RequiredServices(
{	
	@RequiredService(name="cms", type=IComponentManagementService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="chatser", type=IChatGuiService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="emailser", type=IEmailService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM, create=true, creationtype="emailagent")),
	@RequiredService(name="depser", type=IDependencyService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="daeser", type=IDaemonService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM, create=true, creationtype="daemon"))
})
@Arguments(
{
	@Argument(name="interval", clazz=long.class, defaultvalue="10000"),
	@Argument(name="separatevm", clazz=boolean.class, defaultvalue="true"),
	@Argument(name="forbiddenvmargs", clazz=String[].class, defaultvalue="new String[]{\"-agentlib:jdwp=transport\"}"),
	@Argument(name="account", clazz=EmailAccount.class, description="The email account to send the emails."),
	@Argument(name="receivers", clazz=String[].class, description="The email receivers."),
	@Argument(name="outputfile", clazz=String.class, description="Redirect output stream of new platform to file", defaultvalue="\"./platform_out.txt\""),
	@Argument(name="errorfile", clazz=String.class, description="Redirect error stream of new platform to file", defaultvalue="\"./platform_err.txt\"")
})
@ComponentTypes(
{
	@ComponentType(name="emailagent", filename="jadex/platform/service/email/EmailAgent.class"),
	@ComponentType(name="daemon", filename="jadex/platform/service/daemon/DaemonAgent.class")
})
public class UpdateAgent implements IUpdateService
{
	//-------- attributes --------
	
	/** The check for update interval. */
	@AgentArgument
	protected long interval;

	/** Flag if new vm should be used. */
	@AgentArgument
	protected boolean separatevm;
	
	/** The agent. */
	@Agent
	protected MicroAgent agent;
	
	/** The cms. */
	@AgentService
	protected IComponentManagementService cms;
	
//	/** The new cid (need to be acknowledge by create and via call ack). */
//	protected IComponentIdentifier newcomp;
	
//	/** The future to be set when updatign was successful. */
//	protected Future<Void> updated;
	
	/** The vmargs that should not be used. */
	@AgentArgument
	protected String[] forbiddenvmargs;
	
	/** The email account. */
	@AgentArgument
	protected EmailAccount account;

	/** The email receivers. */
	@AgentArgument
	protected String[] receivers;
	
	/** The output file (if any). */
	@AgentArgument
	protected String outputfile;
	
	/** The error file (if any). */
	@AgentArgument
	protected String errorfile;
	
	//-------- methods --------
	
	/**
	 *  The agent body.
	 */
	@AgentBody
	public void body()
	{
		agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				final IComponentStep<Void> self = this;
				checkForUpdate().addResultListener(new IResultListener<UpdateInfo>()
				{
					public void resultAvailable(UpdateInfo ui)
					{
						if(ui!=null)
						{
							performUpdate(ui).addResultListener(new IResultListener<IComponentIdentifier>()
							{
								public void resultAvailable(IComponentIdentifier result)
								{
									notifyUpdatePerformed("Shutting down old platform "+agent.getComponentIdentifier().getRoot()
										+" (Jadex "+VersionInfo.getInstance().getVersion()+", "+VersionInfo.getInstance().getNumberDateString()+") for new platform "+result)
										.addResultListener(new IResultListener<Void>()
									{
										public void resultAvailable(Void result) 
										{
											// Kill platform.
											cms.destroyComponent(agent.getComponentIdentifier().getRoot());	
										}
										
										public void exceptionOccurred(Exception exception) 
										{
											resultAvailable(null);
										}
									});
								}
								
								public void exceptionOccurred(Exception exception)
								{
									agent.waitFor(interval, self);
								}
							});
						}
						else
						{
							agent.waitFor(interval, self);
						}
					}
					
					public void exceptionOccurred(Exception exception)
					{
						agent.waitFor(interval, self);
					}
				});
				return IFuture.DONE;
			}
		});
	}
	
	//-------- interface methods --------
	
//	/**
//	 *  Called when message arrived.
//	 */
//	// hack?: done with message as awareness must not be used so there
//	// is no gauarantee that a proxy to the other platform exists (or would have to be created).
//	@AgentMessageArrived
//	public void messageArrived(Map<String, Object> msg, MessageType mt)
//	{
////		System.out.println("rec: "+msg);
//		if(mt.getName().equals(SFipa.MESSAGE_TYPE_NAME_FIPA))
//		{
//			IComponentIdentifier sender = (IComponentIdentifier)msg.get(SFipa.SENDER);
//			acknowledgeUpdate(sender.getRoot());
//		}
//	}
	
//	/**
//	 * 
//	 */
//	public IFuture<Void> acknowledgeUpdate()
//	{
//		IComponentIdentifier caller = ServiceCall.getInstance().getCaller();
//		acknowledgeUpdate(caller);
//		return IFuture.DONE;
//	}
	
//	/**
//	 *  Called by new platform after correct startup.
//	 *  
//	 *  Acknowledgement is complete when called twice:
//	 *  a) after creation with cid of new platform
//	 *  b) after new update agent has send ack to this agent (handshake)
//	 *  
//	 *  After ack is complete platform shutdown will be initiated.
//	 */
//	public IFuture<Void> acknowledgeUpdate(IComponentIdentifier caller)
//	{
////		System.out.println("ack: "+caller);
//		
//		if(caller.equals(newcomp))
//		{
////			System.out.println("Update acknowledged, shutting down old platform: "+agent.getComponentIdentifier());
//			notifyUpdatePerformed("Shutting down old platform "+agent.getComponentIdentifier().getRoot()
//					+" (Jadex "+VersionInfo.getInstance().getVersion()+", "+VersionInfo.getInstance().getNumberDateString()+") for new platform "+caller.getRoot())
//				.addResultListener(new DelegationResultListener<Void>(updated)
//			{
//				public void exceptionOccurred(Exception exception)
//				{
//					// Update is successful even if notification fails.
//					resultAvailable(null);
//				}
//			});
//		}
//		else if(newcomp==null)
//		{
//			newcomp = caller;
//			updated	= new Future<Void>();
//		}
//		
//		return updated;
//	}
	
	/**
	 *  Notify administrators that platform update has
	 *  been successfully performed.
	 */
	protected IFuture<Void> notifyUpdatePerformed(final String text)
	{
		final Future<Void> ret = new Future<Void>();
		
		CounterResultListener<Void> lis = new CounterResultListener<Void>(2, new DelegationResultListener<Void>(ret));
		
		// notify via chat
		final Future<Void> firstret = new Future<Void>(); 
		firstret.addResultListener(lis);
		IFuture<IChatGuiService> fut = agent.getRequiredService("chatser");
		fut.addResultListener(new ExceptionDelegationResultListener<IChatGuiService, Void>(firstret)
		{
			public void customResultAvailable(IChatGuiService chatser)
			{
				chatser.message(agent.getComponentIdentifier().getName()+": "+text, null, true)
					.addResultListener(new ExceptionDelegationResultListener<Collection<IChatService>, Void>(firstret)
				{
					public void customResultAvailable(Collection<IChatService> result)
					{
						firstret.setResult(null);
					}
				});
			}
		});
		
		// notify via email
		final Future<Void> secret = new Future<Void>(); 
		secret.addResultListener(lis);
		if(receivers!=null && receivers.length>0)
		{
			IFuture<IEmailService> efut = agent.getRequiredService("emailser");
			efut.addResultListener(new IResultListener<IEmailService>()
			{
				public void resultAvailable(IEmailService emailser)
				{
					Email eml = new Email(null, text, "platform update notification", null);
					eml.setReceivers(receivers);
					emailser.sendEmail(eml, account).addResultListener(new DelegationResultListener<Void>(secret));
				}
				public void exceptionOccurred(Exception exception)
				{
					agent.getLogger().warning("Failed to send email: "+exception);
					secret.setResult(null);
				}
			});
		}
		else
		{
			secret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 *  Perform the update.
	 */
	public IFuture<IComponentIdentifier> performUpdate(UpdateInfo ui)
	{
//		System.out.println("perform update: "+ui);
		
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();
		
		if(separatevm)
		{
			generateStartOptions(ui).addResultListener(new ExceptionDelegationResultListener<StartOptions, IComponentIdentifier>(ret)
			{
				public void customResultAvailable(StartOptions so)
				{
					startPlatformInSeparateVM(so).addResultListener(new DelegationResultListener<IComponentIdentifier>(ret));
				}
			});
		}
		else
		{
			// todo: generate start options for same vm
			
			startPlatformInSameVM().addResultListener(new DelegationResultListener<IComponentIdentifier>(ret));
		}
		
		return ret;
	}
	
	/**
	 *  Start a new platform in the same vm.
	 */
	public IFuture<IComponentIdentifier> startPlatformInSameVM()
	{
//		System.out.println("Starting platform in same vm");
		
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();
		
		// todo: create new classpath for new version 
		
		Map<String, Object> args = agent.getArguments();//new HashMap<String, Object>();
		args.put("creator", agent.getComponentIdentifier());
		String argsstr = AWriter.objectToXML(XMLWriterFactory.getInstance().createWriter(true, false, false), args, null, JavaWriter.getObjectHandler());
//		String argsstr = JavaWriter.objectToXML(args, null);
		argsstr = argsstr.replaceAll("\"", "\\\\\"");
		String deser = "jadex.xml.bean.JavaReader.objectFromXML(\""+argsstr+"\""+",null)";
//		
		// todo: find out original configuration and parameters to replay on new
		// todo for major release: make checkpoint and let new use checkpoint
		
//		String comstr = "-component jadex.platform.service.autoupdate.UpdateAgent.class(:"+deser+")";
//		String comstr = "-maven_dependencies true -component jadex.platform.service.autoupdate.UpdateAgent.class(:"+deser+")";
//		System.out.println("generated: "+comstr);
		
//		StartOptions so = new StartOptions();
//		so.setMain("jadex.base.Starter");
//		so.setProgramArguments(comstr);
		
//		Starter.createPlatform(new String[]{"-component", "jadex.platform.service.autoupdate.UpdateAgent.class(:"+deser+")"})
//		Starter.createPlatform(new String[]{"-deftimeout", "-1", "-component", "jadex.platform.service.autoupdate.UpdateAgent.class(:"+deser+")"})
		Starter.createPlatform(new String[]{"-maven_dependencies", "false", "-component", "jadex.platform.service.autoupdate.UpdateAgent.class(:"+deser+")"})
			.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, IComponentIdentifier>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				IComponentIdentifier cid = result.getComponentIdentifier();
				ret.setResult(cid);
//				acknowledgeUpdate(cid.getRoot());
			}
		});
		
		return ret;
	}
	
	/**
	 *  Start a new platform in a separate vm.
	 */
	public IFuture<IComponentIdentifier> startPlatformInSeparateVM(final StartOptions so)
	{
//		System.out.println("Starting platform in separate vm");
		
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();
		
		IFuture<IDaemonService> fut = agent.getRequiredService("daeser");
		fut.addResultListener(new ExceptionDelegationResultListener<IDaemonService, IComponentIdentifier>(ret)
		{
			public void customResultAvailable(IDaemonService daeser)
			{
				daeser.startPlatformAndWait(so).addResultListener(new DelegationResultListener<IComponentIdentifier>(ret)); 
			}
		});
		
		return ret;
	}

	//-------- helper methods --------
	
	/**
	 *  Generate the vm start options.
	 *  
	 *  - main class: jadex.base.Starter
	 *  - vmargs: old vmargs (without debug option)
	 *  - program args: old program args (with updated argument for update agent to allow handshake)
	 */
	protected IFuture<StartOptions> generateStartOptions(UpdateInfo ui)
	{
		final Future<StartOptions> ret = new Future<StartOptions>();
		final StartOptions so = new StartOptions();
		
		so.setMain("jadex.base.Starter");
		so.setOutputFile(outputfile);
		so.setErrorFile(errorfile);
		
		RuntimeMXBean rbean = ManagementFactory.getRuntimeMXBean();
		List<String> vmargs = new ArrayList<String>(rbean.getInputArguments());

		if(vmargs!=null && vmargs.size()>0)
		{
			if(forbiddenvmargs!=null && forbiddenvmargs.length>0)
			{
				for(Iterator<String> it=vmargs.iterator(); it.hasNext(); )
				{
					String tst = it.next();
					for(String forbid: forbiddenvmargs)
					{
						if(tst.indexOf(forbid)!=-1)
						{
							it.remove();
							break;
						}
					}
				}
			}
			so.setVMArguments(flattenStrings((Iterator)SReflect.getIterator(vmargs), " "));
		}
		
//		String cmd = System.getProperty("sun.java.command");
		
		IFuture<IComponentManagementService> fut = agent.getRequiredService("cms");
		fut.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, StartOptions>(ret)
		{
			public void customResultAvailable(IComponentManagementService cms)
			{
				cms.getExternalAccess(agent.getComponentIdentifier().getRoot())
					.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, StartOptions>(ret)
				{
					public void customResultAvailable(IExternalAccess plat)
					{
						plat.getArguments().addResultListener(new ExceptionDelegationResultListener<Map<String,Object>, StartOptions>(ret)
						{
							public void customResultAvailable(Map<String, Object> args)
							{
								// Set program args according to the original ones
								// Will change the argument with the update agent to have the creator cid
								
								String[] oldargs = (String[])args.get(Starter.PROGRAM_ARGUMENTS);
								List<String> newargs = new ArrayList<String>();
								
								if(oldargs!=null)
								{
									for(int i=0; i<oldargs.length; i++)
									{
//										if("-component".equals(oldargs[i]) && oldargs[i+1].indexOf("jadex.platform.service.autoupdate.FileUpdateAgent")!=-1)
										if("-component".equals(oldargs[i]) && (oldargs[i+1].indexOf("UpdateAgent")!=-1
											// Hack!!! Shouldn't know about daemon responder!?
											|| oldargs[i+1].indexOf("DaemonResponderAgent")!=-1))
										{
											i++;
										}
										else
										{
											newargs.add("\""+SUtil.escapeString(oldargs[i])+"\"");
										}
									}
								}
								
								// Add -component jadex.platform.service.autoupdate.FileUpdateAgent.class with fresh argument
								Map<String, Object> uaargs = getUpdateArguments();
								String argsstr = AWriter.objectToXML(XMLWriterFactory.getInstance().createWriter(true, false, false), uaargs, null, JavaWriter.getObjectHandler());
//								System.out.println("pre: "+argsstr);
								argsstr	= SUtil.escapeString(argsstr);	// First: escape string
								argsstr = argsstr.replace("\"", "\\\\\""); // then escape quotes again for argument parser
//								System.out.println("post: "+argsstr);
								String deser = "jadex.xml.bean.JavaReader.objectFromXML(\\\""+argsstr+"\\\",null)";
								newargs.add("-component");
								newargs.add("\""+agent.getModel().getFullName().replace(".", "/")+"Agent.class(:"+deser+")\"");

								so.setProgramArguments(flattenStrings((Iterator)SReflect.getIterator(newargs), " "));
								
								ret.setResult(so);
							}
						});
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  Flatten strings to one string.
	 *  @param it The iterator of strings.
	 *  @param delim The delimiter used to connect the entries.
	 */
	public String flattenStrings(Iterator<String> it, String delim)
	{
		StringBuffer buf = new StringBuffer();
		while(it.hasNext())
		{
			buf.append(it.next());
			if(it.hasNext())
				buf.append(delim);
		}
		return buf.toString();
	}
	
	/**
	 *  Check if an update is available.
	 */
	protected IFuture<UpdateInfo> checkForUpdate()
	{
		return new Future<UpdateInfo>((UpdateInfo)null);
	}
	
	/**
	 *  Get the arguments to use for the update agent. 
	 */
	protected Map<String, Object>	getUpdateArguments()
	{
		Map<String, Object>	ret	= new HashMap<String, Object>();
		ret.putAll(agent.getArguments());
		return ret;
	}
}
