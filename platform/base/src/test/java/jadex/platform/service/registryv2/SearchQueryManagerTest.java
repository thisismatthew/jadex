package jadex.platform.service.registryv2;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jadex.base.IPlatformConfiguration;
import jadex.base.PlatformConfigurationHandler;
import jadex.base.Starter;
import jadex.base.test.util.STest;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.registryv2.ISuperpeerStatusService;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;

/**
 *  Test search and query managing functionality.
 */
//@Ignore
public class SearchQueryManagerTest
{
	//-------- constants --------
	
	/** The delay time as factor of the default remote timeout. */
	public static final double	WAITFACTOR	= 0.01;	// 30 * 0.01 secs  -> 300 millis.
	
	/** Client configuration for platform used for searching. */
	public static final IPlatformConfiguration	CLIENTCONF;

	/** Plain provider configuration. */
	public static final IPlatformConfiguration	PROCONF;

	/** Superpeer platform configuration. */
	public static final IPlatformConfiguration	SPCONF;

	static
	{
		IPlatformConfiguration	baseconf	= STest.getDefaultTestConfig();
		baseconf.setValue("superpeerclient.awaonly", false);
		baseconf.setValue("superpeerclient.pollingrate", WAITFACTOR/2); 	// 30 * 0.005 secs  -> 150 millis.
		
		CLIENTCONF	= baseconf.clone();
		
		PROCONF	= baseconf.clone();
		PROCONF.addComponent(ProviderAgent.class);
		PROCONF.addComponent(LocalProviderAgent.class);
		
		SPCONF	= baseconf.clone();
		SPCONF.addComponent(SuperpeerRegistryAgent.class);
	}
	
	//-------- life cycle helper --------
	
	protected Collection<IExternalAccess>	platforms;
	
	@Before
	public void setup()
	{
		platforms	= new ArrayList<>();
	}
	
	@After
	public void tearDown()
	{
		for(IExternalAccess platform: platforms)
		{
			try
			{
				platform.killComponent().get();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	protected IExternalAccess	createPlatform(IPlatformConfiguration config)
	{
		IExternalAccess	ret	= Starter.createPlatform(config).get();
		platforms.add(ret);
		return ret;
	}
	
	protected void	removePlatform(IExternalAccess platform)
	{
		platform.killComponent().get();
		platforms.remove(platform);
	}
	
	protected void wait(IExternalAccess platform)
	{
		platform.waitForDelay(Starter.getScaledRemoteDefaultTimeout(platform.getId(), WAITFACTOR), new IComponentStep<Void>()
		{
			@Override
			public IFuture<Void> execute(IInternalAccess ia)
			{
				return IFuture.DONE;
			}
		}, true).get();
	}
	
	//-------- test cases --------
	
	/**
	 *  cases for testing: services
	 */
	@Test
	public void	testServices()
	{
		// 1) search for service -> not found (test if works with no superpeers and no other platforms)
		System.out.println("1) search for service");
		IExternalAccess	client	= createPlatform(CLIENTCONF);
		Collection<ITestService>	result	= client.searchServices(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL)).get();
		Assert.assertTrue(""+result, result.isEmpty());
		
		// 2) start remote platform, search for service -> test if awa fallback works with one platform 
		System.out.println("2) start remote platform, search for service");
		IExternalAccess	pro1	= createPlatform(PROCONF);
		result	= client.searchServices(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL)).get();
		Assert.assertEquals(""+result, 1, result.size());
		
		// 3) start remote platform, search for service -> test if awa fallback works with two platforms 
		System.out.println("3) start remote platform, search for service");
		/*IExternalAccess	pro2	=*/ createPlatform(PROCONF);
		result	= client.searchServices(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL)).get();
		Assert.assertEquals(""+result, 2, result.size());
		
		// 4) start SP, wait for connection from remote platforms and local platform, search for service -> test if SP connection works
		System.out.println("4) start SP, wait for connection from remote platforms and local platform, search for service");
		IExternalAccess	sp	= createPlatform(SPCONF);
		ISuperpeerStatusService	status	= sp.searchService(new ServiceQuery<>(ISuperpeerStatusService.class, RequiredServiceInfo.SCOPE_PLATFORM)).get();
		ISubscriptionIntermediateFuture<IComponentIdentifier>	connected	= status.getRegisteredClients();
		connected.getNextIntermediateResult();	// Self-connection + client + 2x provider
		connected.getNextIntermediateResult();
		connected.getNextIntermediateResult();
		connected.getNextIntermediateResult();
		wait(client);
		result	= client.searchServices(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL)).get();
		Assert.assertEquals(""+result, 2, result.size());
		
		// 5) kill one remote platform, search for service -> test if remote disconnection and service removal works
		System.out.println("5) kill one remote platform, search for service");
		removePlatform(pro1);
		result	= client.searchServices(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL)).get();
		Assert.assertEquals(""+result, 1, result.size());

		// 6) kill SP, search for service -> test if re-fallback to awa works
		System.out.println("6) kill SP, search for service");
		removePlatform(sp);
		result	= client.searchServices(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL)).get();
	}
	
	/**
	 *  cases for testing: queries
	 */
	@Test
	public void	testQueries()
	{
		// 1) add query -> not found (test if works with no superpeers and no other platforms)
		System.out.println("1) add query");
		IExternalAccess	client	= createPlatform(CLIENTCONF);
		ISubscriptionIntermediateFuture<ITestService>	results	= client.addQuery(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL));
		wait(client);
		Assert.assertEquals("1) ", Collections.emptySet(), new LinkedHashSet<>(results.getIntermediateResults()));
		
		// 2) start remote platform, wait for service -> test if awa fallback works with one platform, also checks local duplicate removal over time
		System.out.println("2) start remote platform, wait for service");
		IExternalAccess	pro1	= createPlatform(PROCONF);
		ITestService	svc	= results.getNextIntermediateResult();
		Assert.assertEquals("2) "+svc, pro1.getId(), ((IService)svc).getId().getProviderId().getRoot());
		
		// 3) start remote platform, wait for service -> test if awa fallback works with two platforms 
		System.out.println("3) start remote platform, wait for service");
		IExternalAccess	pro2	= createPlatform(PROCONF);
		svc	= results.getNextIntermediateResult();
		Assert.assertEquals("3) "+svc, pro2.getId(), ((IService)svc).getId().getProviderId().getRoot());
		
		// 4) start SP, wait for connection from remote platforms and local platform -> should get no service; test if duplicate removal works with SP
		System.out.println("4) start SP, wait for connection from remote platforms and local platform");
		IExternalAccess	sp	= createPlatform(SPCONF);
		ISuperpeerStatusService	status	= sp.searchService(new ServiceQuery<>(ISuperpeerStatusService.class, RequiredServiceInfo.SCOPE_PLATFORM)).get();
		ISubscriptionIntermediateFuture<IComponentIdentifier>	connected	= status.getRegisteredClients();
		connected.getNextIntermediateResult();	// Self-connection + client + 2x provider
		connected.getNextIntermediateResult();
		connected.getNextIntermediateResult();
		connected.getNextIntermediateResult();
		wait(client);
		Assert.assertEquals("4) ", Collections.emptySet(), new LinkedHashSet<>(results.getIntermediateResults()));
		
		// 5) add second query -> wait for two services (test if works when already SP)
		System.out.println("5) add second query");
		ISubscriptionIntermediateFuture<ITestService>	results2	= client.addQuery(new ServiceQuery<>(ITestService.class, RequiredServiceInfo.SCOPE_GLOBAL));
		Set<IComponentIdentifier>	providers1	= new LinkedHashSet<>();
		svc	= results2.getNextIntermediateResult();
		providers1.add(((IService)svc).getId().getProviderId().getRoot());
		svc	= results2.getNextIntermediateResult();
		providers1.add(((IService)svc).getId().getProviderId().getRoot());
		Set<IComponentIdentifier>	providers2	= new LinkedHashSet<>();
		providers2.add(pro1.getId());
		providers2.add(pro2.getId());
		Assert.assertEquals("5) ", Collections.emptySet(), new LinkedHashSet<>(results2.getIntermediateResults()));
		Assert.assertEquals(providers1, providers2);
		
		// 6) start remote platform, wait for service in both queries -> test if works for existing queries (before and after SP)
		System.out.println("6) start remote platform, wait for service in both queries");
		IExternalAccess	pro3	= createPlatform(PROCONF);
		svc	= results.getNextIntermediateResult();
		Assert.assertEquals(""+svc, pro3.getId(), ((IService)svc).getId().getProviderId().getRoot());
		svc	= results2.getNextIntermediateResult();
		Assert.assertEquals(""+svc, pro3.getId(), ((IService)svc).getId().getProviderId().getRoot());

		// TODO
//		// 7) kill SP, start remote platform, wait for service on both queries -> test if re-fallback to awa works for queries
//		System.out.println("7) kill SP, start remote platform, wait for service on both queries");
//		removePlatform(sp);
//		IExternalAccess	pro4	= createPlatform(PROCONF);
//		svc	= results.getNextIntermediateResult();
//		Assert.assertEquals(""+svc, pro4.getId(), ((IService)svc).getId().getProviderId().getRoot());
//		svc	= results2.getNextIntermediateResult();
//		Assert.assertEquals(""+svc, pro4.getId(), ((IService)svc).getId().getProviderId().getRoot());
	}

	//-------- main for testing --------
	
	/**
	 *  Main for testing.
	 */
	public static void	main(String[] args)
	{
		// Common base configuration
		IPlatformConfiguration	baseconfig	= PlatformConfigurationHandler.getMinimalComm();
		baseconfig.addComponent("jadex.platform.service.pawareness.PassiveAwarenessIntraVMAgent.class");
//		baseconfig.setGui(true);
//		baseconfig.setLogging(true);
		
		// Super peer base configuration
		IPlatformConfiguration	spbaseconfig	= baseconfig.clone();
		spbaseconfig.addComponent(SuperpeerRegistryAgent.class);
		
		IPlatformConfiguration	config;
		
		// Super peer AB
		config	= spbaseconfig.clone();
		config.setPlatformName("SPAB_*");
		config.setNetworkNames("network-a", "network-b");
		config.setNetworkSecrets("secret-a1234", "secret-b1234");
		Starter.createPlatform(config, args).get();
		
		// Super peer BC
		config	= spbaseconfig.clone();
		config.setPlatformName("SPBC_*");
		config.setNetworkNames("network-c", "network-b");
		config.setNetworkSecrets("secret-c1234", "secret-b1234");
		Starter.createPlatform(config, args).get();

		// Client ABC
		config	= baseconfig.clone();
		config.addComponent(SuperpeerClientAgent.class);
		config.setPlatformName("ClientABC_*");
		config.setNetworkNames("network-a", "network-b", "network-c");
		config.setNetworkSecrets("secret-a1234", "secret-b1234", "secret-c1234");
		Starter.createPlatform(config, args).get();
	}
}
