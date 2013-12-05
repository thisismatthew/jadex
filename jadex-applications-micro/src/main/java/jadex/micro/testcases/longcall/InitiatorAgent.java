package jadex.micro.testcases.longcall;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.ServiceCall;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.commons.SReflect;
import jadex.commons.Tuple2;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IFutureCommandResultListener;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.IntermediateDelegationResultListener;
import jadex.commons.future.IntermediateExceptionDelegationResultListener;
import jadex.commons.future.IntermediateFuture;
import jadex.commons.future.IForwardCommandFuture.Type;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.micro.testcases.TestAgent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  Tests if a long lasting call works even with default or small timeout.
 */
@Agent
@RequiredServices(
{
	@RequiredService(name="ts", type=ITestService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_GLOBAL))
})
@Arguments(
{
	@Argument(name="testcnt", clazz=int.class, defaultvalue="12")
})
public class InitiatorAgent extends TestAgent
{
	/**
	 *  Perform the tests.
	 */
	protected IFuture<Void> performTests(final Testcase tc)
	{
		if(SReflect.isAndroid()) 
		{
			tc.setTestCount(6);
		}
		
		final Future<Void> ret = new Future<Void>();
		
		testLocal(1).addResultListener(agent.createResultListener(new IntermediateExceptionDelegationResultListener<TestReport, Void>(ret)
		{
			public void customResultAvailable(Collection<TestReport> result)
			{
				for(TestReport tr: result)
					tc.addReport(tr);
				proceed();
			}
			
			public void finished()
			{
				proceed();
			}
			
			public void intermediateResultAvailable(TestReport result)
			{
				tc.addReport(result);
			}
			
			public void proceed()
			{
				if(SReflect.isAndroid()) 
				{
					// skip remote tests
					ret.setResult(null);
				} 
				else 
				{
					testRemote(3).addResultListener(agent.createResultListener(new IntermediateExceptionDelegationResultListener<TestReport, Void>(ret)
					{
						public void customResultAvailable(Collection<TestReport> result)
						{
							for(TestReport tr: result)
								tc.addReport(tr);
							ret.setResult(null);
						}
						
						public void finished()
						{
							ret.setResult(null);
						}
						
						public void intermediateResultAvailable(TestReport result)
						{
							tc.addReport(result);
						}
					}));
				}
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Test local.
	 */
	protected IIntermediateFuture<TestReport> testLocal(final int testno)
	{
		final IntermediateFuture<TestReport> ret = new IntermediateFuture<TestReport>();
		
		performTests(agent.getComponentIdentifier().getRoot(), testno, true)
			.addResultListener(agent.createResultListener(new IntermediateDelegationResultListener<TestReport>(ret)));
		
		return ret;
	}
	
	/**
	 *  Test remote.
	 */
	protected IIntermediateFuture<TestReport> testRemote(final int testno)
	{
		final IntermediateFuture<TestReport> ret = new IntermediateFuture<TestReport>();
		
		createPlatform(null/*new String[]{"-gui", "true", "-logging", "true"}*/).addResultListener(agent.createResultListener(
			new ExceptionDelegationResultListener<IExternalAccess, Collection<TestReport>>(ret)
		{
			public void customResultAvailable(final IExternalAccess platform)
			{
				performTests(platform.getComponentIdentifier(), testno, false)
					.addResultListener(agent.createResultListener(new IntermediateDelegationResultListener<TestReport>(ret)));
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Perform the test. Consists of the following steps:
	 *  Create provider agent
	 *  Call methods on it
	 */
	protected IIntermediateFuture<TestReport> performTests(final IComponentIdentifier root, final int testno, final boolean hassectrans)
	{
		final IntermediateFuture<TestReport> ret = new IntermediateFuture<TestReport>();

		final IntermediateFuture<TestReport> res = new IntermediateFuture<TestReport>();
		
		ret.addResultListener(new IntermediateDelegationResultListener<TestReport>(res)
		{
			public void exceptionOccurred(Exception exception)
			{
				TestReport tr = new TestReport("#"+testno, "Tests if a long running call works.");
				tr.setReason(exception);
				List<TestReport> li = new ArrayList<TestReport>();
				super.resultAvailable(li);
			}
		});
		
		final Future<Collection<Tuple2<String, Object>>> resfut = new Future<Collection<Tuple2<String, Object>>>();
		IResultListener<Collection<Tuple2<String, Object>>> reslis = new DelegationResultListener<Collection<Tuple2<String,Object>>>(resfut);
		
//		System.out.println("root: "+root+" "+SUtil.arrayToString(root.getAddresses()));
		createComponent(ProviderAgent.class.getName()+".class", root, reslis)
			.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, Collection<TestReport>>(ret)
		{
			public void customResultAvailable(final IComponentIdentifier cid) 
			{
				callServices(cid, testno, -1).addResultListener(new IntermediateDelegationResultListener<TestReport>(ret));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
				super.exceptionOccurred(exception);
			}
		});
		
		return res;
	}
	
	/**
	 *  Call the service methods.
	 */
	protected IIntermediateFuture<TestReport> callServices(IComponentIdentifier cid, int testno, final long to)
	{
		final IntermediateFuture<TestReport> ret = new IntermediateFuture<TestReport>();
		
		IFuture<ITestService> fut = agent.getServiceContainer().getService(ITestService.class, cid);
		
//		fut.addResultListener(new IResultListener()
//		{
//			public void resultAvailable(Object result)
//			{
//				System.out.println("res: "+result+" "+SUtil.arrayToString(result.getClass().getInterfaces()));
//				try
//				{
//					ITestService ts = (ITestService)result;
//				}
//				catch(Exception e)
//				{
//					e.printStackTrace();
//				}
//			}
//			public void exceptionOccurred(Exception exception)
//			{
//				exception.printStackTrace();
//			}
//		});
		
		fut.addResultListener(new ExceptionDelegationResultListener<ITestService, Collection<TestReport>>(ret)
		{
			public void customResultAvailable(final ITestService ts)
			{
				// create a service call meta object and set the timeout
				if(to!=-1)
				{
//					ServiceCall.setInvocationProperties(to, true);
					ServiceCall call = ServiceCall.getOrCreateNextInvocation();
					call.setTimeout(to);
					call.setRealtime(Boolean.TRUE);
				}				
				
//				System.out.println("calling method: "+ServiceCall.getOrCreateNextInvocation());
				
				callMethod(ts, 1, ret).addResultListener(new IResultListener<Void>()
				{
					public void resultAvailable(Void result)
					{
						ret.setFinished();
					}
					public void exceptionOccurred(Exception exception)
					{
						ret.setException(exception);
					}
				});
			}
		});
		return ret;
	}
	
	/**
	 * 
	 */
	protected IFuture<Void> callMethod(final ITestService ts, final int cnt, final IntermediateFuture<TestReport> ret) 
	{
		final Future<Void> res = new Future<Void>();
		
		try
		{
			// Garbage collect before executing method to avoid interference causing timeouts on slow computers
			System.gc();
			
			final TestReport tr = new TestReport("#"+cnt, "Test if long call works with normal timeout.");

			Method m = ITestService.class.getMethod("method"+cnt, new Class[0]);
//			System.out.println("calling method "+cnt+": "+System.currentTimeMillis());
			
			ServiceCall.getOrCreateNextInvocation().setTimeout(BasicService.getLocalDefaultTimeout()/15);
			
			final long start	= System.currentTimeMillis();
			((IFuture<Object>)m.invoke(ts, new Object[0])).addResultListener(new IFutureCommandResultListener<Object>()
			{
				public void resultAvailable(Object result)
				{
//					System.out.println("rec result "+cnt+": "+(System.currentTimeMillis()-start)+", "+System.currentTimeMillis());
					tr.setSucceeded(true);
					ret.addIntermediateResult(tr);
					proceed();
				}
				
				public void exceptionOccurred(Exception exception)
				{
//					System.out.println("rec exception "+cnt+": "+(System.currentTimeMillis()-start)+", "+System.currentTimeMillis());
					tr.setFailed("Exception: "+exception);
					ret.addIntermediateResult(tr);
					proceed();
				}
				
				public void commandAvailable(Object command)
				{
					// ignore timer updates
				}
				
				public void proceed()
				{
					if(cnt<6)
					{
						callMethod(ts, cnt+1, ret).addResultListener(new DelegationResultListener<Void>(res));
					}
					else
					{
						res.setResult(null);
					}
				}
			});
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return res;
	}
}
