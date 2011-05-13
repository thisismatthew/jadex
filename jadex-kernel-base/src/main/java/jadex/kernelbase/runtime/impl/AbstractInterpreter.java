package jadex.kernelbase.runtime.impl;

import jadex.bridge.ComponentResultListener;
import jadex.bridge.CreationInfo;
import jadex.bridge.IComponentAdapter;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentInstance;
import jadex.bridge.IComponentListener;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IMessageAdapter;
import jadex.bridge.IntermediateComponentResultListener;
import jadex.bridge.RemoteComponentListener;
import jadex.bridge.modelinfo.ComponentInstanceInfo;
import jadex.bridge.modelinfo.ConfigurationInfo;
import jadex.bridge.modelinfo.IArgument;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.modelinfo.SubcomponentTypeInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IInternalService;
import jadex.bridge.service.IServiceContainer;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.ProvidedServiceImplementation;
import jadex.bridge.service.ProvidedServiceInfo;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.commons.SReflect;
import jadex.commons.future.CollectionResultListener;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.javaparser.IValueFetcher;
import jadex.javaparser.SJavaParser;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public abstract class AbstractInterpreter implements IComponentInstance
{
	//-------- interface methods --------
	
	/**
	 *  Can be called concurrently (also during executeAction()).
	 *  
	 *  Inform the agent that a message has arrived.
	 *  Can be called concurrently (also during executeAction()).
	 *  @param message The message that arrived.
	 */
	public abstract void messageArrived(final IMessageAdapter message);

	/**
	 *  Create the service container.
	 *  @return The service container.
	 */
	public abstract IServiceContainer getServiceContainer();
	
	/**
	 *  Test if the component's execution is currently at one of the
	 *  given breakpoints. If yes, the component will be suspended by
	 *  the platform.
	 *  @param breakpoints	An array of breakpoints.
	 *  @return True, when some breakpoint is triggered.
	 */
	public abstract boolean isAtBreakpoint(String[] breakpoints);

	/**
	 *  Can be called concurrently (also during executeAction()).
	 * 
	 *  Get the external access for this component.
	 *  External access objects must implement the IExternalAccess interface. 
	 *  The specific external access interface is kernel specific
	 *  and has to be casted to its corresponding incarnation.
	 *  @return External access is delivered via future.
	 */
	public abstract IExternalAccess getExternalAccess();

	/**
	 *  Get the results of the component (considering it as a functionality).
	 *  Note: The method cannot make use of the asynchrnonous result listener
	 *  mechanism, because the it is called when the component is already
	 *  terminated (i.e. no invokerLater can be used).
	 *  @return The results map (name -> value). 
	 */
	public abstract Map getResults();

	/**
	 *  Can be called on the component thread only.
	 * 
	 *  Main method to perform component execution.
	 *  Whenever this method is called, the component performs
	 *  one of its scheduled actions.
	 *  The platform can provide different execution models for components
	 *  (e.g. thread based, or synchronous).
	 *  To avoid idle waiting, the return value can be checked.
	 *  The platform guarantees that executeAction() will not be called in parallel. 
	 *  @return True, when there are more actions waiting to be executed. 
	 */
	public abstract boolean executeStep();

	/**
	 *  Can be called concurrently (also during executeAction()).
	 *   
	 *  Request component to cleanup itself after kill.
	 *  The component might perform arbitrary cleanup activities during which executeAction()
	 *  will still be called as usual.
	 *  Can be called concurrently (also during executeAction()).
	 *  @return When cleanup of the component is finished, the future is notified.
	 */
	public abstract IFuture cleanupComponent();
	
	/**
	 *  Called when a component has been created as a subcomponent of this component.
	 *  This event may be ignored, if no special reaction to new or destroyed components is required.
	 *  The current subcomponents can be accessed by IComponentAdapter.getSubcomponents().
	 *  @param comp	The newly created component.
	 */
	public IFuture	componentCreated(final IComponentDescription desc, final IModelInfo model)
	{
		return IFuture.DONE;
	}
	
	/**
	 *  Called when a subcomponent of this component has been destroyed.
	 *  This event may be ignored, if no special reaction  to new or destroyed components is required.
	 *  The current subcomponents can be accessed by IComponentAdapter.getSubcomponents().
	 *  @param comp	The destroyed component.
	 */
	public IFuture	componentDestroyed(final IComponentDescription desc)
	{
		return IFuture.DONE;
	}

	//-------- internally used methods --------
	
	/**
	 *  Get the component adapter.
	 *  @return The component adapter.
	 */
	public abstract IComponentAdapter getComponentAdapter();

	/**
	 *  Get the model info.
	 *  @return The model info.
	 */
	public abstract IModelInfo getModel();
	
	/**
	 *  Get the imports.
	 *  @return The imports.
	 */
	public abstract String[] getAllImports();
	
	/**
	 *  Get the service bindings.
	 *  @return The service bindings.
	 */
	public abstract RequiredServiceBinding[] getServiceBindings();
	
	/**
	 *  Get the value fetcher.
	 *  @return The value fetcher.
	 */
	public abstract IValueFetcher getFetcher();

	/**
	 *  Get the internal access.
	 *  @return The internal access.
	 */
	public abstract IInternalAccess getInternalAccess();

	/**
	 *  Main init method consists of the following steps:
	 *  - init arguments and results
	 *  - init future properties
	 *  - init required and provided services
	 *  - init subcomponents
	 */
	public IFuture init(final IModelInfo model, final String config, final Map props,
		Map arguments, Map results, final Map properties)
	{
		final Future ret = new Future();
		
		initArguments(model, config, arguments, results).addResultListener(
			createResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				initFutureProperties(props, properties).addResultListener(
					createResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						initServices(model, config).addResultListener(
							createResultListener(new DelegationResultListener(ret)
						{
							public void customResultAvailable(Object result)
							{
								initComponents(model, config).addResultListener(
									createResultListener(new DelegationResultListener(ret)
								{
									public void customResultAvailable(Object result)
									{
										super.customResultAvailable(new Object[]{AbstractInterpreter.this, getComponentAdapter()});
									}		
								}));
							}
						}));
					}
				}));
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Init the arguments and results.
	 */
	public IFuture initArguments(IModelInfo model, final String config, Map arguments, Map results)
	{
		// Init the arguments with default values.
		IArgument[] args = model.getArguments();
		for(int i=0; i<args.length; i++)
		{
			if(args[i].getDefaultValue(config)!=null)
			{
//				if(this.arguments==null)
//					this.arguments = new HashMap();
			
				if(!arguments.containsKey(args[i].getName()))
				{
					arguments.put(args[i].getName(), args[i].getDefaultValue(config));
				}
			}
		}
		
		// Init the results with default values.
		IArgument[] res = model.getResults();
		for(int i=0; i<res.length; i++)
		{
			if(res[i].getDefaultValue(config)!=null)
			{
//				if(this.results==null)
//					this.results = new HashMap();
			
				results.put(res[i].getName(), res[i].getDefaultValue(config));
			}
		}

		return IFuture.DONE;
	}
	
	/**
	 *  Init the services.
	 */
	public IFuture initServices(IModelInfo model, String config)
	{
		ProvidedServiceInfo[] services = model.getProvidedServices();
//		System.out.println("init sers: "+services);
		if(services!=null)
		{
			for(int i=0; i<services.length; i++)
			{
				Object ser = null;
				
				ProvidedServiceImplementation impl = services[i].getImplementation(); 
				if(impl.getExpression()!=null || impl.getImplementation()!=null)
				{
					try
					{
						if(impl.getExpression()!=null)
						{
							// todo: other Class imports, how can be found out?
							ser = SJavaParser.evaluateExpression(impl.getExpression(), getAllImports(), getFetcher(), model.getClassLoader());
							
	//						System.out.println("added: "+service+" "+getAgentAdapter().getComponentIdentifier());
						}
						else if(services[i].getImplementation()!=null)
						{
							ser = impl.getImplementation().newInstance();
						}
						
						if(ser!=null)
						{
							addService(ser, impl.getProxytype());
						}
						else
						{
							getLogger().warning("Service creation error: "+impl.getExpression());
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
						getLogger().warning("Service creation error: "+impl.getExpression());
					}
				}
				else 
				{
					RequiredServiceInfo info = new RequiredServiceInfo("virtual", services[i].getType());
					IServiceIdentifier sid = BasicService.createServiceIdentifier(getExternalAccess().getServiceProvider().getId(), 
						info.getType(), BasicServiceInvocationHandler.class);
					IInternalService service = BasicServiceInvocationHandler.createDelegationProvidedServiceProxy(getExternalAccess(), getComponentAdapter(), sid, info, impl.getBinding());
					getServiceContainer().addService(service);
				}
			}
		}
		return getServiceContainer().start();
	}
	
	/**
	 *  Init the future properties.
	 */
	public IFuture initFutureProperties(final Map props, final Map properties)
	{
		Future ret = new Future();
		
		// Evaluate (future) properties.
		final List futures = new ArrayList();
		if(props!=null)
		{
			for(Iterator it=props.keySet().iterator(); it.hasNext(); )
			{
				final String name = (String)it.next();
				final Object value = props.get(name);
				if(value instanceof UnparsedExpression)
				{
					final UnparsedExpression unexp = (UnparsedExpression)value;
					final Object val = SJavaParser.evaluateExpression(unexp.getValue(), getAllImports(), getFetcher(), getClassLoader());
					if(unexp.getClazz()!=null && SReflect.isSupertype(IFuture.class, unexp.getClazz()))
					{
//						System.out.println("Future property: "+mexp.getName()+", "+val);
						if(val instanceof IFuture)
						{
							// Use second future to start component only when value has already been set.
							final Future retu = new Future();
							((IFuture)val).addResultListener(createResultListener(new DefaultResultListener()
							{
								public void resultAvailable(Object result)
								{
									synchronized(properties)
									{
//										System.out.println("Setting future property: "+mexp.getName()+" "+result);
										properties.put(unexp.getName(), result);
									}
									retu.setResult(result);
								}
							}));
							futures.add(retu);
						}
						else if(val!=null)
						{
							throw new RuntimeException("Future property must be instance of jadex.commons.IFuture: "+name+", "+unexp.getValue());
						}
					}
					else
					{
						// Todo: handle specific properties (logging etc.)
						properties.put(name, val);
					}
				}
			}
			
			IResultListener	crl	= new CounterResultListener(futures.size(), new DelegationResultListener(ret));
			for(int i=0; i<futures.size(); i++)
			{
				((IFuture)futures.get(i)).addResultListener(crl);
			}
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 *  Init the subcomponents.
	 */
	public IFuture initComponents(IModelInfo model, String config)
	{
		final Future ret = new Future();
		
		if(config!=null)
		{
			ConfigurationInfo conf = model.getConfiguration(config);
			if(conf==null)
			{
				System.out.println("repair me!");
				ret.setResult(null);
				return ret;
			}
			final ComponentInstanceInfo[] components = conf.getComponentInstances();
			SServiceProvider.getServiceUpwards(getServiceContainer(), IComponentManagementService.class)
				.addResultListener(createResultListener(new DefaultResultListener()
			{
				public void resultAvailable(Object result)
				{
					// NOTE: in current implementation application waits for subcomponents
					// to be finished and cms implements a hack to get the external
					// access of an uninited parent.
					
					// (NOTE1: parent cannot wait for subcomponents to be all created
					// before setting itself inited=true, because subcomponents need
					// the parent external access.)
					
					// (NOTE2: subcomponents must be created one by one as they
					// might depend on each other (e.g. bdi factory must be there for jcc)).
					
					final IComponentManagementService ces = (IComponentManagementService)result;
					createComponent(components, ces, 0, ret);
				}
			}));
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
		
	/**
	 *  Get the logger.
	 *  @return The logger.
	 */
	public Logger getLogger()
	{
		return getComponentAdapter().getLogger();
	}
	
	//-------- methods --------

	/**
	 *  Get the name.
	 */
	// todo: remove.
	public String	getName()
	{
		return getComponentAdapter().getComponentIdentifier().getLocalName();		
	}
	
	/**
	 *  Get a string representation of the context.
	 */
	public String	toString()
	{
		StringBuffer	ret	= new StringBuffer();
		ret.append(SReflect.getInnerClassName(getClass()));
		ret.append("(name=");
		ret.append(getComponentAdapter().getComponentIdentifier().getLocalName());
//		ret.append(", parent=");
//		ret.append(getParentContext());
//		IComponentIdentifier[]	aids	= getComponents(); 
//		if(aids!=null)
//		{
//			ret.append(", components=");
//			ret.append(SUtil.arrayToString(aids));
//		}
//		IContext[]	subcs	= getSubContexts(); 
//		if(subcs!=null)
//		{
//			ret.append(", subcontexts=");
//			ret.append(SUtil.arrayToString(subcs));
//		}
		ret.append(")");
		return ret.toString();
	}
		
	//-------- methods --------
	
	/**
	 *  Get the component identifier.
	 */
	public IComponentIdentifier getComponentIdentifier()
	{
		return getComponentAdapter().getComponentIdentifier();
	}
	
	//-------- methods to be called by kernel --------
	
	/**
	 *  Add a service to the component. 
	 *  @param service The service.
	 *  @param proxytype	The proxy type (@see{BasicServiceInvocationHandler}).
	 */
	public void addService(Object service, String proxytype)
	{
		IInternalService proxy = BasicServiceInvocationHandler.createProvidedServiceProxy(getInternalAccess(), getComponentAdapter(), service, proxytype);
		getServiceContainer().addService(proxy);
	}
	
	//-------- methods to be called by adapter --------
	
	/**
	 *  Kill the component.
	 */
	public IFuture killComponent()
	{
		final Future ret = new Future();
		
		SServiceProvider.getService(getServiceContainer(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object result)
			{
				((IComponentManagementService)result).destroyComponent(getComponentAdapter().getComponentIdentifier())
					.addResultListener(new DelegationResultListener(ret));
			}
		});
		
		return ret;
	}
	
	/**
	 *  Get the class loader of the component.
	 *  The component class loader is required to avoid incompatible class issues,
	 *  when changing the platform class loader while components are running. 
	 *  This may occur e.g. when decoding messages and instantiating parameter values.
	 *  @return	The component class loader. 
	 */
	public ClassLoader getClassLoader()
	{
		return getModel().getClassLoader();
	}

	/**
	 *  Get the file name for a logical type name of a subcomponent of this application.
	 */
	public String getComponentFilename(String type)
	{
		String ret = null;
		SubcomponentTypeInfo[] subcomps = getModel().getSubcomponentTypes();
		for(int i=0; ret==null && i<subcomps.length; i++)
		{
			SubcomponentTypeInfo subct = (SubcomponentTypeInfo)subcomps[i];
			if(subct.getName().equals(type))
				ret = subct.getFilename();
		}
		return ret;
	}
		
	/**
	 *  Create a result listener which is executed as an component step.
	 *  @param The original listener to be called.
	 *  @return The listener.
	 */
	public IResultListener createResultListener(IResultListener listener)
	{
		return new ComponentResultListener(listener, getComponentAdapter());
	}

	/**
	 *  Create a result listener which is executed as an component step.
	 *  @param The original listener to be called.
	 *  @return The listener.
	 */
	public IIntermediateResultListener createResultListener(IIntermediateResultListener listener)
	{
		return new IntermediateComponentResultListener(listener, getComponentAdapter());
	}

	/**
	 *  Create subcomponents.
	 *  NOTE: parent cannot declare itself initing while subcomponents are created
	 *  because they need the external access of the parent, which is available only
	 *  after init is finished (otherwise there is a cyclic init dependency between parent and subcomps). 
	 */
	public void createComponent(final ComponentInstanceInfo[] components, final IComponentManagementService cms, final int i, final Future inited)
	{
		if(i<components.length)
		{
//			System.out.println("Create: "+component.getName()+" "+component.getTypeName()+" "+component.getConfiguration()+" "+Thread.currentThread());
			int num = getNumber(components[i]);
			final IResultListener crl = new CollectionResultListener(num, false, createResultListener(new IResultListener()
			{
				public void resultAvailable(Object result)
				{
//					System.out.println("Create finished: "+component.getName()+" "+component.getTypeName()+" "+component.getConfiguration()+" "+Thread.currentThread());
//					if(getParent()==null)
//					{
//						addStep(new Runnable()
//						{
//							public void run()
//							{
//								createComponent(components, cl, ces, i+1, inited);
//							}
//						});
//					}
//					else
//					{
						createComponent(components, cms, i+1, inited);
//						scheduleStep(new IComponentStep()
//						{
//							@XMLClassname("createChild")
//							public Object execute(IInternalAccess ia)
//							{
//								createComponent(components, cms, i+1, inited);
//								return null;
//							}
//						});
//					}
				}
				
				public void exceptionOccurred(Exception exception)
				{
					inited.setException(exception);
				}
			}));
			for(int j=0; j<num; j++)
			{
				SubcomponentTypeInfo type = components[i].getType(getModel());
				if(type!=null)
				{
					final Boolean suspend	= components[i].getSuspend()!=null ? components[i].getSuspend() : type.getSuspend();
					Boolean	master = components[i].getMaster()!=null ? components[i].getMaster() : type.getMaster();
					Boolean	daemon = components[i].getDaemon()!=null ? components[i].getDaemon() : type.getDaemon();
					Boolean	autoshutdown = components[i].getAutoShutdown()!=null ? components[i].getAutoShutdown() : type.getAutoShutdown();
					RequiredServiceBinding[] bindings = components[i].getBindings();
					IFuture ret = cms.createComponent(components[i].getName(), type.getFilename(),
						new CreationInfo(components[i].getConfiguration(), getArguments(components[i]), getComponentAdapter().getComponentIdentifier(),
						suspend, master, daemon, autoshutdown, getAllImports(), bindings), null);
					ret.addResultListener(crl);
				}
				else
				{
					crl.exceptionOccurred(new RuntimeException("No such component type: "+components[i].getTypeName()));
				}
			}
		}
		else
		{
			// Init is now finished. Notify cms.
//			System.out.println("Application init finished: "+ApplicationInterpreter.this);

			// master, daemon, autoshutdown
//			Boolean[] bools = new Boolean[3];
//			bools[2] = model.getAutoShutdown();
			
//			for(int j=0; j<tostart.size(); j++)
//			{
//				IComponentIdentifier cid = (IComponentIdentifier)tostart.get(j);
//				cms.resumeComponent(cid);
//			}
			
			inited.setResult(new Object[]{AbstractInterpreter.this, getComponentAdapter()});
		}
	}
	
	/**
	 *  Get the arguments.
	 *  @return The arguments as a map of name-value pairs.
	 */
	public Map getArguments(ComponentInstanceInfo component)
	{
		Map ret = null;		
		UnparsedExpression[] arguments = component.getArguments();

		if(arguments.length>0)
		{
			ret = new HashMap();

			for(int i=0; i<arguments.length; i++)
			{
				// todo: language
				Object val = SJavaParser.evaluateExpression(arguments[i].getValue(), getAllImports(), getFetcher(), getClassLoader());
				ret.put(arguments[i].getName(), val);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get the number of components to start.
	 *  @return The number.
	 */
	public int getNumber(ComponentInstanceInfo component)
	{
		Object val = component.getNumber()!=null? SJavaParser.evaluateExpression(component.getNumber(), getAllImports(), getFetcher(), getClassLoader()): null;
		return val instanceof Integer? ((Integer)val).intValue(): 1;
	}

	/**
	 *  Get the service provider.
	 */
	public IServiceProvider getServiceProvider()
	{
		return getServiceContainer();
	}
	
//	/**
//	 *  Create the service container.
//	 *  @return The service container.
//	 */
//	public IServiceContainer createServiceContainer()
//	{
//		IServiceContainer ret = null;
//		// Init service container.
//		UnparsedExpression mex = getComponentType().getContainer();
//		if(mex!=null)
//		{
//			ret = (IServiceContainer)mex.getParsedValue().getValue(getFetcher());
//		}
//		else
//		{
////				container = new CacheServiceContainer(new ComponentServiceContainer(getComponentAdapter()), 25, 1*30*1000); // 30 secs cache expire
//			ret = new ComponentServiceContainer(getComponentAdapter(), 
//				getComponentTypeName(), getModel().getRequiredServices(), getServiceBindings());
//		}			
//		return ret;
//	}
	
	/**
	 *  Add an component listener.
	 *  @param listener The listener.
	 */
	public IFuture addComponentListener(List componentlisteners, IComponentListener listener)
	{
		if(componentlisteners==null)
			componentlisteners = new ArrayList();
		
		// Hack! How to find out if remote listener?
		if(Proxy.isProxyClass(listener.getClass()))
			listener = new RemoteComponentListener(getExternalAccess(), listener);
		
		componentlisteners.add(listener);
		return IFuture.DONE;
	}
	
	/**
	 *  Remove a component listener.
	 *  @param listener The listener.
	 */
	public IFuture removeComponentListener(List componentlisteners, IComponentListener listener)
	{
		// Hack! How to find out if remote listener?
		if(Proxy.isProxyClass(listener.getClass()))
			listener = new RemoteComponentListener(getExternalAccess(), listener);
		
		if(componentlisteners!=null)
			componentlisteners.remove(listener);
		
//		System.out.println("cl: "+componentlisteners);
		return IFuture.DONE;
	}
}
