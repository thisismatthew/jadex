package jadex.platform.service.dht;

import jadex.base.Starter;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.dht.IDistributedKVStoreService;
import jadex.bridge.service.types.dht.IFinger;
import jadex.bridge.service.types.dht.IID;
import jadex.bridge.service.types.dht.IRingNodeDebugService;
import jadex.bridge.service.types.dht.IRingNodeService;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.concurrent.TimeoutException;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DefaultTuple2ResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class DistributedKVStoreTest extends TestCase
{
	@Rule 
	public TestName name = new TestName();

	private IDistributedKVStoreService	store1;
	private IDistributedKVStoreService	store2;
	private IDistributedKVStoreService	store3;
	private IDistributedKVStoreService	store4;
	
	private IRingNodeDebugService	ring1;
	private IRingNodeDebugService 	ring2;
	private IRingNodeDebugService 	ring3;
	private IRingNodeDebugService 	ring4;

	private long	timeout;

	private IExternalAccess	platform1;
	private IExternalAccess	platform2;
	private IExternalAccess	platform3;
	private IExternalAccess	platform4;



	
	@Before
	public void setUp() {
		
//		System.out.println("Default timeout is: " + timeout);
		timeout = Starter.getLocalDefaultTimeout(null);
		
		String	pid	= SUtil.createUniqueId(name.getMethodName(), 3)+"-*";
		
		platform1 = Starter.createPlatform(new String[]{"-platformname", pid,
//					"-relaytransport", "false",
//					"-deftimeout", Long.toString(timeout),
//					"-logging", "true",
			"-dht true -saveonexit", "false", "-welcome", "false", "-autoshutdown", "false", "-gui", "false", "-awareness", "false", "-printpass", "false",
//			"-component", "jadex/platform/service/dht/RingAgent.class"
			}).get(timeout);
				
		platform2 = Starter.createPlatform(new String[]{"-platformname", pid,
			"-dht true -saveonexit", "false", "-welcome", "false", "-autoshutdown", "false", "-gui", "false", "-awareness", "false", "-printpass", "false",
	//					"-relaytransport", "false",
	//					"-deftimeout", Long.toString(timeout),
	//					"-logging", "true",
		}).get(timeout);
			
		platform3 = Starter.createPlatform(new String[]{"-platformname", pid,
			"-dht true -saveonexit", "false", "-welcome", "false", "-autoshutdown", "false", "-gui", "false", "-awareness", "false", "-printpass", "false",
	//					"-relaytransport", "false",
	//					"-deftimeout", Long.toString(timeout),
	//					"-logging", "true",
		}).get(timeout);
		
		platform4 = Starter.createPlatform(new String[]{"-platformname", pid,
			"-dht true -saveonexit", "false", "-welcome", "false", "-autoshutdown", "false", "-gui", "false", "-awareness", "false", "-printpass", "false",
	//					"-relaytransport", "false",
	//					"-deftimeout", Long.toString(timeout),
	//					"-logging", "true",
		}).get(timeout);
				
		
		// rn1 should join rn2
		
//		rn1 = SServiceProvider
//			.getService(platform1, IDebugRingNode.class, RequiredServiceInfo.SCOPE_GLOBAL).get(timeout);
		
		Tuple2<IDistributedKVStoreService, IRingNodeDebugService> t1 = createRingAgent(platform1);
		store1 = t1.getFirstEntity();
		ring1 = t1.getSecondEntity();
		
		Tuple2<IDistributedKVStoreService, IRingNodeDebugService> t2 = createRingAgent(platform2);
		store2 = t2.getFirstEntity();
		ring2 = t2.getSecondEntity();
		
		Tuple2<IDistributedKVStoreService, IRingNodeDebugService> t3 = createRingAgent(platform3);
		store3 = t3.getFirstEntity();
		ring3 = t3.getSecondEntity();
		
		Tuple2<IDistributedKVStoreService, IRingNodeDebugService> t4 = createRingAgent(platform4);
		store4 = t4.getFirstEntity();
		ring4 = t4.getSecondEntity();
		
		createProxies(platform1, platform2, platform3, platform4);
	}

	private void createProxies(IExternalAccess ... platforms)
	{
		for(int i = 0; i < platforms.length; i++)
		{
			for(int j = 0; j < platforms.length; j++)
			{
				Starter.createProxy(platforms[i], platforms[j]).get(timeout);
			}
		}
	}

	private Tuple2<IDistributedKVStoreService, IRingNodeDebugService> createRingAgent(IExternalAccess platform)
	{
		IComponentManagementService cms = SServiceProvider.getService(platform, IComponentManagementService.class, RequiredServiceInfo.SCOPE_GLOBAL).get(timeout);
		
		IComponentIdentifier identifier = createComponent(cms, DistributedKVStoreAgent.class);
		
//		IDebugRingNode declaredService = SServiceProvider.getDeclaredService(platform, IDebugRingNode.class).get();
//		System.out.println(declaredService);
		
		IDistributedKVStoreService storeService = SServiceProvider.getService(platform, identifier, IDistributedKVStoreService.class).get(timeout);
		
		IRingNodeDebugService iDebugRingNode = null; 
		
//		IComponentIdentifier[] iComponentIdentifiers = cms.getChildren(identifier).get();
//		for(IComponentIdentifier cid : iComponentIdentifiers)
//		{
//			if (cid.getLocalName().equals("RingNode")) {
				iDebugRingNode = SServiceProvider.getService(platform, identifier, IRingNodeDebugService.class).get(timeout);		
//			}
//		}
		
		iDebugRingNode.disableSchedules();
		return new Tuple2<IDistributedKVStoreService, IRingNodeDebugService>(storeService, iDebugRingNode);
	}

	private IComponentIdentifier createComponent(IComponentManagementService cms, Class<?> clazz)
	{
		final Future<IComponentIdentifier> future = new Future<IComponentIdentifier>();
		cms.createComponent(clazz.getName() + ".class", null).addResultListener(new DefaultTuple2ResultListener<IComponentIdentifier, Map<String,Object>>()
		{
			@Override
			public void firstResultAvailable(IComponentIdentifier result)
			{
				future.setResult(result);
			}

			@Override
			public void secondResultAvailable(Map<String, Object> result)
			{
			}

			@Override
			public void exceptionOccurred(Exception exception)
			{
			}
		});
		
		// wait for creation
		IComponentIdentifier identifier = future.get(timeout);
		return identifier;
	}
	
	@Test
	public void testSave() {
		store1.put("test", "testValue").get();
		assertEquals("testValue", store1.lookup("test").get());
	}
	
	@Test
	public void testSaveRemote() {
		ring1.join(ring2).get();
		stabilize2(ring1, ring2, ring3).get();
		ring3.join(ring2).get();
		stabilize2(ring1, ring2, ring3).get();
		
		assertCircle(ring1,ring2,ring3);
		IID iid = ring1.getId().get();
		final String key = findKeyForStoreId(iid, ring2.getId().get(), ring3.getId().get());
		
		System.out.println("key for id : " + iid + " is: "  + key + " with hash: " + ID.get(key));
		
		// value has to be stored in store1, so store2 should pass it through.
		IID put = store2.put(key, "testValue").get();
		
		// check if store1 knows it is responsible for the key
		IID responsibleId = store1.lookupResponsibleStore(key).get();
		assertEquals(iid, responsibleId);

		// check if retrieval works by calling lookup on all stores.
		assertEquals("testValue", store1.lookup(key).get());
		assertEquals("testValue", store2.lookup(key).get());
		assertEquals("testValue", store3.lookup(key).get());
	}
	
//	@Test
//	public void testMove() {
//		store1.put("testXX", "testValue");
//		Set<StoreEntry> moveEntries = store1.moveEntries(store1.getRingService().get().getId().get()).get();
//		StoreEntry next = moveEntries.iterator().next();
//		assertEquals("testXX", next.getKey());
//		assertEquals("testValue", next.getValue());
//	}
	
	@Test
	public void testMove_afterStabilize() {
		// ring consists of ring1 only
		
		final String key = findKeyForStoreId(ring2, ring1, ring3);
		IID put = store1.put(key, "testValue").get();
		// value is stored in store1, but the key is in the range of store2.
		
		// during join, the key should be moved to ring2:
		ring2.join(ring1).get();
		stabilize2(ring1, ring2).get();
		
		// check if all stores know that store2 is responsible for the key now:
		assertEquals(ring2.getId().get(), store2.lookupResponsibleStore(key).get());
		assertEquals(ring2.getId().get(), store1.lookupResponsibleStore(key).get());
		
		// check if correct value is saved now:
		assertEquals("testValue", store2.lookup(key).get());
		assertEquals("testValue", store1.lookup(key).get());
	}
	
	@Test
	public void testMove_onSuccessorChange() {
		// ring consists of ring1 only
		
		final String key = findKeyForStoreId(ring2, ring1, ring3);
		IID put = store1.put(key, "testValue").get();
		// value is stored in store1, but the key is in the range of store2.
		
		// during join, the key should be moved to ring2:
		ring2.join(ring1).get();
		
		// check if all stores know that store2 is responsible for the key now:
		assertEquals(ring2.getId().get(), store1.lookupResponsibleStore(key).get());
		assertEquals(ring2.getId().get(), store2.lookupResponsibleStore(key).get());

		// check if correct value is saved now:
		assertEquals("testValue", store2.lookup(key).get());
		assertEquals("testValue", store1.lookup(key).get());
	}
	
	@Test
	public void testMoveCollection_onSuccessorChange() {
		// ring consists of ring1 only
		
		final String key = findKeyForStoreId(ring2, ring1, ring3);
		List<String> asList = Arrays.asList(new String[]{"testValue", "testValue2"});
		IID put = store1.put(key, asList).get();
		// value is stored in store1, but the key is in the range of store2.
		
		// during join, the key should be moved to ring2:
		ring2.join(ring1).get();
		
		// check if all stores know that store2 is responsible for the key now:
		assertEquals(ring2.getId().get(), store1.lookupResponsibleStore(key).get());
		assertEquals(ring2.getId().get(), store2.lookupResponsibleStore(key).get());

		// check if correct value is saved now:
		assertEquals(asList, store2.lookup(key).get());
		assertEquals(asList, store1.lookup(key).get());
	}
	
	@Test
	public void testMove_onPredecessorChange() {
		// ring consists of ring1 only
		
		final String key = findKeyForStoreId(ring2, ring1, ring3);
		IID put = store1.put(key, "testValue").get();
		// value is stored in store1, but the key is in the range of store2.
		
		// during join, the key should be moved to ring2:
		ring1.join(ring2).get();
		
//		stabilize2(ring1, ring2).get();
		
		// check if all stores know that store2 is responsible for the key now:
		assertEquals(ring2.getId().get(), store2.lookupResponsibleStore(key).get());
		assertEquals(ring2.getId().get(), store1.lookupResponsibleStore(key).get());
		
		// check if correct value is saved now:
		assertEquals("testValue", store2.lookup(key).get());
		assertEquals("testValue", store1.lookup(key).get());
	}
	
	@Test
	public void testMoveEntries_withThreeNodes() {
		// This tests if already existing data is passed to the right node eventually.
		// Example: Data with hash = 18 exists in node 10.
		// Nodes 20 - 30 - 40 are in a ring.
		// Now 40 joins 10 -> data has to be copied to 20.
		
		// ring consists of ring1 only
		ring1.init(createId2(10));
		ring2.init(createId2(20));
		ring3.init(createId2(30));
		ring4.init(createId2(40));
		
		// propagate id change to kvstores:
		store1.setRingService(ring1);
		store2.setRingService(ring2);
		store3.setRingService(ring3);
		store4.setRingService(ring4);
		
		final String key = findKeyForStoreId(ring2, ring1, ring3, ring4);

		// ring 3 is responsible for the key.
		// but the others don't know that.
		IID put = store1.put(key, "testValue").get();
		
		ring2.join(ring3).get();
		ring3.join(ring4).get();
		// ring is now: ring2 - ring3 - ring4
		ring4.join(ring1).get();
		// ring is now ring1 - ring 2 - ring3
		
		// stabilize for responsibilities:
		// this is an artificial example where stabilizing is done in the correct order and
		// one-after another.
		for(int i = 0; i < 3; i++)
		{
			ring4.stabilize().get();
			ring1.stabilize().get();
			ring3.stabilize().get();
			ring2.stabilize().get();
//			ring2.stabilize().get();
		}
		
		// after stabilize, store 1 knows it is not responsible anymore
		assertEquals(ring2.getId().get(), store1.lookupResponsibleStore(key).get());
		assertEquals(ring2.getId().get(), store2.lookupResponsibleStore(key).get());
		assertEquals(ring2.getId().get(), store3.lookupResponsibleStore(key).get());
		assertEquals(ring2.getId().get(), store4.lookupResponsibleStore(key).get());

		// and the corresponding value should be moved, too.
		assertEquals("testValue", store1.lookup(key).get());
		assertEquals("testValue", store2.lookup(key).get());
		assertEquals("testValue", store3.lookup(key).get());
		assertEquals("testValue", store4.lookup(key).get());
	}
	
	@Test
	public void testResponsibility_afterJoin_askJoining() {
		// ring consists of ring1 only
		final String key = findKeyForStoreId(ring2, ring1, ring3);
		
		assertEquals(ring2.getId().get(), store2.lookupResponsibleStore(key).get());
		
		ring1.join(ring2).get();
		
		assertEquals(ring2.getId().get(), store1.lookupResponsibleStore(key).get());
	}
	
	@Test
	public void testResponsibility_afterJoin_askJoined() {
		// ring consists of ring1 only
		final String key = findKeyForStoreId(ring2, ring1, ring3);
		
		assertEquals(ring2.getId().get(), store2.lookupResponsibleStore(key).get());
		
		ring2.join(ring1).get();
		// responsibility is only correct after stabilize...
		stabilize2(ring1, ring2).get();
		
		assertEquals(ring2.getId().get(), store1.lookupResponsibleStore(key).get());
	}
	
//	@Test
//	public void testJoin() {
//		store2.join(store1).get();
//		stabilize2(new IRingNodeDebugService[]{store1, store2}).get();
//	}
	
//	@Test
//	public void testKillPlatform() {
//		store2.join(store1).get();
//		stabilize2(new IRingNodeDebugService[]{store1, store2, store3}).get();
//		store3.join(store1).get();
//		stabilize2(new IRingNodeDebugService[]{store1, store2, store3}).get();
//		stabilize2(new IRingNodeDebugService[]{store1, store2, store3}).get();
//		
//		IID iid = store3.getId().get();
//		System.out.println("killing platform of node: " + iid);
//		platform3.killComponent().get();
//		
//		System.out.println("platform killed");
//		
//		stabilize(new IRingNodeDebugService[]{store1, store2}).get();
//		
//		System.out.println(store1.getFingerTableString().get());
//		System.out.println(store2.getFingerTableString().get());
//
//		assertCircle(store1, store2);
//	}
	
	
	
	// -----------------------------
	// --------- HELPER ------------
	// -----------------------------
	
	private String findKeyForStoreId(IRingNodeDebugService ring, IRingNodeDebugService... others)
	{
		IID[] iids = new IID[others.length];
		for(int i = 0; i < others.length; i++)
		{
			IID iid = others[i].getId().get();
			iids[i] = iid;
		}
		
		return findKeyForStoreId(ring.getId().get(), iids);
	}
	
	private String findKeyForStoreId(IID storeId, IID... others)
	{
		Random random = new Random();
		boolean found = false;
		String foundKey = null;
		
//		System.out.println("Searching for matching key: " + storeId);
//		for(int i = 0; i < others.length; i++)
//		{
//			System.out.println("other id: " + others[i]);
//		}
		
		while (!found) {
			int nextInt = random.nextInt(Integer.MAX_VALUE);
			String s = "key_" + nextInt;
			foundKey = s;
			IID iid = ID.get(s);
			found = true;
			for(IID other : others)
			{
				if (!iid.isInInterval(other, storeId)) 
				{
					found = false;
					break;
				}
			}
		}
		return foundKey;
	}

	private void assertCircle(IRingNodeDebugService ... rns)
	{
		IRingNodeService[] circleContents = new IRingNodeService[rns.length];
		IRingNodeService suc = rns[0];
		System.out.print("circle is: ");
		System.out.print(suc.getId().get(timeout) + ", ");
		for(int i = 0; i < rns.length; i++)
		{
 			IFuture<IFinger> successor = suc.getSuccessor();
			IFinger sucsuc = successor.get(timeout);
			suc = getService(rns, sucsuc.getSid());
			System.out.print(suc.getId().get(timeout) + ", ");
			if (suc == null) {
				throw new RuntimeException("got null service");
			}
			circleContents[i] = suc;
		}
		
		for(int i = 0; i < rns.length; i++)
		{
			boolean found = false;
			IID wanted = rns[i].getId().get(timeout);
			for(int j = 0; j < circleContents.length; j++)
			{
				IID iid = circleContents[j].getId().get(timeout);
				if (iid.equals(wanted)) {
					found = true;
				}
			}
			assertTrue("Node " + wanted + " has to be in the circle",found);
		}
		System.out.println();
		
		assertEquals(rns[0].getId().get(), suc.getId().get());
	}

	private IRingNodeDebugService getService(IRingNodeDebugService[] rns, IServiceIdentifier sid)
	{
		for(int i = 0; i < rns.length; i++)
		{
			IService s = (IService)rns[i];
			IComponentIdentifier providerId = s.getServiceIdentifier().getProviderId();
			IComponentIdentifier providerId2 = sid.getProviderId();
			if (providerId.equals(providerId2)) {
				return rns[i];
			}
		}
		return null;
		
	}
	
	private void fixFingers(IRingNodeDebugService[] nodes) {
		for(int j = 0; j < nodes.length; j++)
		{
			for(int i = 0; i < nodes.length; i++)
			{
				try {
//					nodes[i].stabilize().get(10000);
					nodes[i].fixFingers().get(10000);
//					nodes[i].stabilize().get(10000);
				} catch (TimeoutException e) {
					System.out.println("to for node " + nodes[i].getId().get());
//					e.printStackTrace();
				}
			}
		}
	}

	private IFuture<Void> stabilize2(IRingNodeDebugService ... nodes) {
		stabilize(nodes).get();
		return stabilize(nodes);
	}
	
	private IFuture<Void> stabilize(IRingNodeDebugService ... nodes)
	{
		final Future<Void> future = new Future<Void>();

		CounterResultListener<Void> rejoinListener = new CounterResultListener<Void>((nodes.length), new DelegationResultListener<Void>(future))
		{

			@Override
			public void resultAvailable(Void result)
			{
				super.resultAvailable(result);
//				System.out.println("count: " + cnt);
			}

			@Override
			public void exceptionOccurredIfUndone(Exception exception)
			{
//				if(exception instanceof UnjoinedException)
//				{
//					System.out.println("initiate re-join");
//					rn2.join(rn1);
//				} else {
					exception.printStackTrace();
//				}
			}
		};

//		for(int j = 0; j < nodes.length; j++)
//		{
			for(int i = 0; i < nodes.length; i++)
			{
				try
				{
//					ServiceCall.getOrCreateNextInvocation().setTimeout(5000);
					nodes[i].stabilize().addResultListener(rejoinListener);
					// stabilize.addResultListener(rejoinListener);
					// nodes[i].fixFingers().get(10000);
					// nodes[i].stabilize().get(10000);
				}
				catch(TimeoutException e)
				{
					System.out.println("to for node " + nodes[i]);
//					e.printStackTrace();
				}
			}
//		}
		return future;
	}
	
	private ID createId2(int firstByte)
	{
		return new ID(new byte[]{(byte)(firstByte - 128)});
	}
}
