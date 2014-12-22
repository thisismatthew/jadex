package jadex.micro.features.impl;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IArgumentsFeature;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.impl.AbstractComponentFeature;
import jadex.bridge.component.impl.ComponentFeatureFactory;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IProvidedServicesFeature;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceNotFoundException;
import jadex.commons.FieldInfo;
import jadex.commons.MethodInfo;
import jadex.commons.SReflect;
import jadex.commons.Tuple2;
import jadex.commons.Tuple3;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.micro.MicroModel;
import jadex.micro.annotation.AgentService;
import jadex.micro.features.IMicroInjectionFeature;
import jadex.micro.features.IMicroLifecycleFeature;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *  Inject agent arguments into annotated field values.
 */
public class MicroInjectionComponentFeature extends	AbstractComponentFeature
{
	//-------- constants ---------
	
	/** The factory. */
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(
		IMicroInjectionFeature.class, MicroInjectionComponentFeature.class,
		new Class<?>[]{IArgumentsFeature.class, IRequiredServicesFeature.class}, new Class<?>[]{IProvidedServicesFeature.class});

	//-------- constructors --------
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public MicroInjectionComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}

	/**
	 *  Initialize the feature.
	 *  Empty implementation that can be overridden.
	 */
	public IFuture<Void> init()
	{
		final Future<Void> ret = new Future<Void>();
		
		Map<String, Object>	args	= getComponent().getComponentFeature(IArgumentsFeature.class).getArguments();
		Map<String, Object>	results	= getComponent().getComponentFeature(IArgumentsFeature.class).getResults();
		MicroModel	model	= (MicroModel)getComponent().getModel().getRawModel();

		// Inject agent fields.
		FieldInfo[] fields = model.getAgentInjections();
		Object	agent	= getComponent().getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent();
		for(int i=0; i<fields.length; i++)
		{
			try
			{
				Field f = fields[i].getField(getComponent().getClassLoader());
				f.setAccessible(true);
				f.set(agent, getComponent());
			}
			catch(Exception e)
			{
				getComponent().getLogger().warning("Agent injection failed: "+e);
			}
		}

		// Inject argument values
		if(args!=null)
		{
			String[] names = model.getArgumentInjectionNames();
			if(names.length>0)
			{
				for(int i=0; i<names.length; i++)
				{
					Object val = args.get(names[i]);
					
//					if(val!=null || getModel().getArgument(names[i]).getDefaultValue()!=null)
					final Tuple2<FieldInfo, String>[] infos = model.getArgumentInjections(names[i]);
					
					try
					{
						for(int j=0; j<infos.length; j++)
						{
							Field field = infos[j].getFirstEntity().getField(getComponent().getClassLoader());
							String convert = infos[j].getSecondEntity();
//							System.out.println("seting arg: "+names[i]+" "+val);
							setFieldValue(val, field, convert);
						}
					}
					catch(Exception e)
					{
						getComponent().getLogger().warning("Field injection failed: "+e);
						if(!ret.isDone())
							ret.setException(e);
					}
				}
			}
		}
		
		// Inject default result values
		if(results!=null)
		{
			String[] names = model.getResultInjectionNames();
			if(names.length>0)
			{
				for(int i=0; i<names.length; i++)
				{
					if(results.containsKey(names[i]))
					{
						Object val = results.get(names[i]);
						final Tuple3<FieldInfo, String, String> info = model.getResultInjection(names[i]);
						
						try
						{
							Field field = info.getFirstEntity().getField(getComponent().getClassLoader());
							String convert = info.getSecondEntity();
//							System.out.println("seting res: "+names[i]+" "+val);
							setFieldValue(val, field, convert);
						}
						catch(Exception e)
						{
							getComponent().getLogger().warning("Field injection failed: "+e);
							if(!ret.isDone())
								ret.setException(e);
						}
					}
				}
			}
		}
		
		// Inject feature fields.
		fields = model.getFeatureInjections();
		for(int i=0; i<fields.length; i++)
		{
			try
			{
				Class<?> iface = getComponent().getClassLoader().loadClass(fields[i].getTypeName());
				Object feat = getComponent().getComponentFeature(iface);
				Field f = fields[i].getField(getComponent().getClassLoader());
				f.setAccessible(true);
				f.set(agent, feat);
			}
			catch(Exception e)
			{
				getComponent().getLogger().warning("Feature injection failed: "+e);
				if(!ret.isDone())
					ret.setException(e);
			}
		}
		
		// Inject required services
		injectServices(agent, model, getComponent()).addResultListener(new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				if(!ret.isDone())
					ret.setResult(null);
			}

			public void exceptionOccurred(Exception exception)
			{
				if(!ret.isDone())
					ret.setException(exception);
			}
		});
		
		return ret;
	}
	
	/**
	 *  Inject the services to the annotated fields.
	 */
	public static IFuture<Void> injectServices(final Object agent, final MicroModel model, final IInternalAccess component)
	{
		Future<Void> ret = new Future<Void>();

		if(component.getComponentFeature(IRequiredServicesFeature.class)==null)
		{
			ret.setResult(null);
		}
		else
		{
			String[] sernames = model.getServiceInjectionNames();
			
			if(sernames.length>0)
			{
				CounterResultListener<Void> lis = new CounterResultListener<Void>(sernames.length, 
					new DelegationResultListener<Void>(ret));
		
				for(int i=0; i<sernames.length; i++)
				{
					final Object[] infos = model.getServiceInjections(sernames[i]);
					final CounterResultListener<Void> lis2 = new CounterResultListener<Void>(infos.length, lis);
	
					RequiredServiceInfo	info	= model.getModelInfo().getRequiredService(sernames[i]);				
					final IFuture<Object>	sfut;
					if(info!=null && info.isMultiple())
					{
						IFuture	ifut	= component.getComponentFeature(IRequiredServicesFeature.class).getRequiredServices(sernames[i]);
						sfut	= ifut;
					}
					else
					{
						sfut	= component.getComponentFeature(IRequiredServicesFeature.class).getRequiredService(sernames[i]);					
					}
					
					for(int j=0; j<infos.length; j++)
					{
						if(infos[j] instanceof FieldInfo)
						{
							final Field	f	= ((FieldInfo)infos[j]).getField(component.getClassLoader());
							if(SReflect.isSupertype(IFuture.class, f.getType()))
							{
								try
								{
									f.setAccessible(true);
									f.set(agent, sfut);
									lis2.resultAvailable(null);
								}
								catch(Exception e)
								{
									component.getLogger().warning("Field injection failed: "+e);
									lis2.exceptionOccurred(e);
								}	
							}
							else
							{
								sfut.addResultListener(new IResultListener<Object>()
								{
									public void resultAvailable(Object result)
									{
										try
										{
											f.setAccessible(true);
											f.set(agent, result);
											lis2.resultAvailable(null);
										}
										catch(Exception e)
										{
											component.getLogger().warning("Field injection failed: "+e);
											lis2.exceptionOccurred(e);
										}	
									}
									
									public void exceptionOccurred(Exception e)
									{
										if(!(e instanceof ServiceNotFoundException)
											|| f.getAnnotation(AgentService.class).required())
										{
											component.getLogger().warning("Field injection failed: "+e);
											lis2.exceptionOccurred(e);
										}
										else
										{
											if(SReflect.isSupertype(f.getType(), List.class))
											{
												// Call self with empty list as result.
												resultAvailable(Collections.EMPTY_LIST);
											}
											else
											{
												// Don't set any value.
												lis2.resultAvailable(null);
											}
										}
									}
								});
							}
						}
						else if(infos[j] instanceof MethodInfo)
						{
							final Method	m	= SReflect.getMethod(agent.getClass(), ((MethodInfo)infos[j]).getName(), ((MethodInfo)infos[j]).getParameterTypes(component.getClassLoader()));
							if(info.isMultiple())
							{
								lis2.resultAvailable(null);
								IFuture	tfut	= sfut;
								final IIntermediateFuture<Object>	ifut	= (IIntermediateFuture<Object>)tfut;
								
								ifut.addResultListener(new IIntermediateResultListener<Object>()
								{
									public void intermediateResultAvailable(final Object result)
									{
										if(SReflect.isSupertype(m.getParameterTypes()[0], result.getClass()))
										{
											component.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
											{
												public IFuture<Void> execute(IInternalAccess ia)
												{
													try
													{
														m.setAccessible(true);
														m.invoke(agent, new Object[]{result});
													}
													catch(Throwable t)
													{
														t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
														throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
													}
													return IFuture.DONE;
												}
											});
										}
									}
									
									public void resultAvailable(Collection<Object> result)
									{
										finished();
									}
									
									public void finished()
									{
										if(SReflect.isSupertype(m.getParameterTypes()[0], Collection.class))
										{
											component.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
											{
												public IFuture<Void> execute(IInternalAccess ia)
												{
													try
													{
														m.setAccessible(true);
														m.invoke(agent, new Object[]{ifut.getIntermediateResults()});
													}
													catch(Throwable t)
													{
														t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
														throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
													}
													return IFuture.DONE;
												}
											});
										}
									}
									
									public void exceptionOccurred(Exception e)
									{
										if(!(e instanceof ServiceNotFoundException)
											|| m.getAnnotation(AgentService.class).required())
										{
											component.getLogger().warning("Method injection failed: "+e);
										}
										else
										{
											// Call self with empty list as result.
											finished();
										}
									}
								});
	
							}
							else
							{
								lis2.resultAvailable(null);
								sfut.addResultListener(new IResultListener<Object>()
								{
									public void resultAvailable(final Object result)
									{
										component.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
										{
											public IFuture<Void> execute(IInternalAccess ia)
											{
												try
												{
													m.setAccessible(true);
													m.invoke(agent, new Object[]{result});
													lis2.resultAvailable(null);
												}
												catch(Throwable t)
												{
													t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
													throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
												}
												return IFuture.DONE;
											}
										});
									}
									
									public void exceptionOccurred(Exception e)
									{
										if(!(e instanceof ServiceNotFoundException)
											|| m.getAnnotation(AgentService.class).required())
										{
											component.getLogger().warning("Method service injection failed: "+e);
										}
									}
								});
							}
						}
					}
				}
			}
			else
			{
				ret.setResult(null);
			}
		}
		
		return ret;
	}
	
//	/**
//	 *  Inject the parent to the annotated fields.
//	 */
//	protected IFuture<Void> injectParent(final Object agent, final MicroModel model)
//	{
//		Future<Void> ret = new Future<Void>();
//		FieldInfo[]	pis	= model.getParentInjections();
//		
//		if(pis.length>0)
//		{
//			CounterResultListener<Void> lis = new CounterResultListener<Void>(pis.length, 
//				new DelegationResultListener<Void>(ret));
//	
//			for(int i=0; i<pis.length; i++)
//			{
//				final Future<Void>	fut	= new Future<Void>();
//				fut.addResultListener(lis);
//				
//				final Field	f	= pis[i].getField(getComponent().getClassLoader());
//				IComponentManagementService cms = SServiceProvider.getLocalService(getComponent(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
//				cms.getExternalAccess(getComponent().getComponentIdentifier().getParent())
//					.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(fut)
//				{
//					public void customResultAvailable(IExternalAccess exta)
//					{
//						if(IExternalAccess.class.equals(f.getType()))
//						{
//							try
//							{
//								f.setAccessible(true);
//								f.set(agent, exta);
//								fut.setResult(null);
//							}
//							catch(Exception e)
//							{
//								exceptionOccurred(e);
//							}
//						}
//						else if(getComponent().getComponentDescription().isSynchronous())
//						{
//							exta.scheduleStep(new IComponentStep<Void>()
//							{
//								public IFuture<Void> execute(IInternalAccess ia)
//								{
//									if(SReflect.isSupertype(f.getType(), ia.getClass()))
//									{
//										try
//										{
//											f.setAccessible(true);
//											f.set(agent, ia);
//										}
//										catch(Exception e)
//										{
//											throw new RuntimeException(e);
//										}
//									}
//									else if(ia instanceof IPojoMicroAgent)
//									{
//										Object	pagent	= ((IPojoMicroAgent)ia).getPojoAgent();
//										if(SReflect.isSupertype(f.getType(), pagent.getClass()))
//										{
//											try
//											{
//												f.setAccessible(true);
//												f.set(agent, pagent);
//											}
//											catch(Exception e)
//											{
//												exceptionOccurred(e);
//											}
//										}
//										else
//										{
//											throw new RuntimeException("Incompatible types for parent injection: "+pagent+", "+f);													
//										}
//									}
//									else
//									{
//										throw new RuntimeException("Incompatible types for parent injection: "+ia+", "+f);													
//									}
//									return IFuture.DONE;
//								}
//							}).addResultListener(new DelegationResultListener<Void>(fut));
//						}
//						else
//						{
//							exceptionOccurred(new RuntimeException("Non-external parent injection for non-synchronous subcomponent not allowed: "+f));
//						}
//					}
//				});
//			}
//		}
//		else
//		{
//			ret.setResult(null);
//		}
//
//		return	ret;
//	}
	
	//-------- helper methods --------
	
	/**
	 *  Set an injected field value.
	 */
	protected void setFieldValue(Object val, Field field, String convert)
	{
		if(val!=null || !SReflect.isBasicType(field.getType()))
		{
			try
			{
				Object agent = getComponent().getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent();
				if(convert!=null)
				{
					SimpleValueFetcher fetcher = new SimpleValueFetcher(getComponent().getFetcher());
					fetcher.setValue("$value", val);
					val = SJavaParser.evaluateExpression(convert, getComponent().getModel().getAllImports(), fetcher, getComponent().getClassLoader());
				}
				field.setAccessible(true);
				field.set(agent, val);
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
