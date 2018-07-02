package jadex.platform.service.registryv2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.ServiceStart;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.search.ServiceRegistry;
import jadex.bridge.service.types.registryv2.IRemoteRegistryService;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;

/**
 *  Service access to a remote registry.
 */
@Agent
public class RemoteRegistryAgent implements IRemoteRegistryService
{
	/** Component access. */
	@Agent
	protected IInternalAccess ia;
	
	/** The local service registry. */
	protected ServiceRegistry serviceregistry;
	
	protected IComponentIdentifier platformid;
	
	/**
	 *  Service initialization.
	 *  
	 *  @return Null, when done.
	 */
	@ServiceStart
	public IFuture<Void> start()
	{
		serviceregistry = (ServiceRegistry) ServiceRegistry.getRegistry(ia);
		platformid = ia.getComponentIdentifier().getRoot();
		return IFuture.DONE;
	}
	
	/**
	 *  Search remote registry for a single service.
	 *  
	 *  @param query The search query.
	 *  @return The first matching service or null if not found.
	 */
	public <T> IFuture<T> searchService(ServiceQuery<T> query)
	{
		T ret = null;
		boolean localowner = query.getOwner().getRoot().equals(platformid);
		if (!RequiredServiceInfo.isScopeOnLocalPlatform(query.getScope()) || localowner)
		{
			@SuppressWarnings("unchecked")
			Set<T> indexerresults = (Set<T>) serviceregistry.getServicesFromIndexer(query);
			if (localowner)
			{
				for (T ser : indexerresults)
				{
					if (serviceregistry.checkScope(ser, query.getOwner(), query.getScope()))
					{
						ret = ser;
						break;
					}
				}
				
			}
			else if (indexerresults.size() > 0)
			{
				ret = indexerresults.iterator().next();
			}
		}
		
		return new Future<>(ret);
	}
	
	/**
	 *  Search remote registry for services.
	 *  
	 *  @param query The search query.
	 *  @return The matching services or empty set if none are found.
	 */
	public <T> IFuture<Set<T>> searchServices(ServiceQuery<T> query)
	{
		Set<T> ret = Collections.emptySet();
		
		boolean localowner = query.getOwner().getRoot().equals(platformid);
		if (!RequiredServiceInfo.isScopeOnLocalPlatform(query.getScope()) || localowner)
		{
			@SuppressWarnings("unchecked")
			Set<T> indexerresults = (Set<T>) serviceregistry.getServicesFromIndexer(query);
			if (localowner)
			{
				ret = new HashSet<>();
				for (T ser : indexerresults)
				{
					if (serviceregistry.checkScope(ser, query.getOwner(), query.getScope()))
						ret.add(ser);
				}
			}
			else
			{
				ret = indexerresults;
			}
		}
		return new Future<Set<T>>(ret);
	}
}