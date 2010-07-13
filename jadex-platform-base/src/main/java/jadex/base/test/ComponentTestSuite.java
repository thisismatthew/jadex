package jadex.base.test;

import jadex.base.SComponentFactory;
import jadex.base.Starter;
import jadex.bridge.IArgument;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IExternalAccess;
import jadex.bridge.ILoadableComponentModel;
import jadex.commons.concurrent.DefaultResultListener;
import jadex.service.SServiceProvider;
import jadex.service.library.ILibraryService;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestSuite;


/**
 * Execute multiple component tests in a test suite.
 */
public class ComponentTestSuite extends TestSuite
{
	//-------- attributes --------
	
	/** The platform. */
	protected IExternalAccess rootcomp;
	
	//-------- constructors --------

	/**
	 * Create a component test suite for the given components.
	 */
	public ComponentTestSuite(String name, final String[] components) throws Exception
	{
		super(name);
	
		Starter.createPlatform(new String[]{"-configname", "all_kernels", "-simulation", "true"}).addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
				rootcomp	= (IExternalAccess)result;
				SServiceProvider.getService(rootcomp.getServiceProvider(), IComponentManagementService.class).addResultListener(new DefaultResultListener()
				{
					public void resultAvailable(Object source, Object result)
					{
						IComponentManagementService cms = (IComponentManagementService)result;
						for(int i=0; i<components.length; i++)
						{
							addTest(new ComponentTest(cms, components[i]));
						}
					}
				});
			}
		});
	}
	
	/**
	 * Create a component test suite for components contained in a given path.
	 * @param path	The path to look for test cases in.
	 * @param root	The classpath root corresponding to the path.
	 */
	public ComponentTestSuite(File path, File root) throws Exception
	{
		this(path, root, null);
	}
	
	/**
	 * Create a component test suite for components contained in a given path.
	 * @param path	The path to look for test cases in.
	 * @param root	The classpath root corresponding to the path.
	 */
	public ComponentTestSuite(final File path, final File root, final String[] excludes) throws Exception
	{
		this(path.getName(), new String[0]);
		
		SServiceProvider.getServiceUpwards(rootcomp.getServiceProvider(), IComponentManagementService.class).addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
				final IComponentManagementService cms = (IComponentManagementService)result;
				SServiceProvider.getService(rootcomp.getServiceProvider(), ILibraryService.class).addResultListener(new DefaultResultListener()
				{
					public void resultAvailable(Object source, Object result)
					{
						final ILibraryService libsrv	= (ILibraryService)result;
						try
						{
							URL url = root.toURI().toURL();
							libsrv.addURL(url);
						}
						catch(Exception e)
						{
							throw new RuntimeException(e);
						}
						
						
						// Scan for test cases.
						List	todo	= new LinkedList();
						todo.add(path);
						while(!todo.isEmpty())
						{
							File	file	= (File)todo.remove(0);
							final String	abspath	= file.getAbsolutePath();
							boolean	exclude	= false;
							for(int i=0; !exclude && excludes!=null && i<excludes.length; i++)
							{
								exclude	= abspath.indexOf(excludes[i])!=-1;
							}
							
							if(!exclude)
							{
								if(file.isDirectory())
								{
									File[]	subs	= file.listFiles();
									todo.addAll(Arrays.asList(subs));
								}
								else
								{
									final String fabspath = abspath;
									SComponentFactory.isLoadable(rootcomp.getServiceProvider(), abspath).addResultListener(new DefaultResultListener()
									{
										public void resultAvailable(Object source, Object result)
										{
											if(((Boolean)result).booleanValue())
											{
												SComponentFactory.loadModel(rootcomp.getServiceProvider(), fabspath).addResultListener(new DefaultResultListener()
												{
													public void resultAvailable(Object source, Object result)
													{
														boolean istest = false;
														ILoadableComponentModel model = (ILoadableComponentModel)result;
														if(model!=null && model.getReport().isEmpty())
														{
															IArgument[]	results	= model.getResults();
															for(int i=0; !istest && i<results.length; i++)
															{
																if(results[i].getName().equals("testresults") && results[i].getTypename().equals("Testcase"))
																	istest	= true;
															}
														}
														if(istest)
														{
															addTest(new ComponentTest(cms, abspath));
														}
													}
												});
											}
										}
									});
								}
							}
						}
					}
				});
			}
		});
	}
}
