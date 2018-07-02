package jadex.bridge.service.search;

import jadex.bridge.IComponentIdentifier;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.TerminableIntermediateFuture;
import jadex.commons.future.UnlimitedIntermediateDelegationResultListener;

/**
 *  Info with query and result future.
 */
public class ServiceQueryInfo<T>
{
	/** The query. */
	protected ServiceQuery<T> query;
	
	/** The futures. */
	protected TerminableIntermediateFuture<T> future;
	protected ISubscriptionIntermediateFuture<T> remotefuture;
	
	/** The superpeer on which this query was (potentially) added. */
	protected IComponentIdentifier superpeer;

	/**
	 *  Create a new query info.
	 */
	public ServiceQueryInfo(ServiceQuery<T> query, TerminableIntermediateFuture<T> future)
	{
		this.query = query;
		this.future = future;
	}

	/**
	 *  Get the query.
	 *  @return The query
	 */
	public ServiceQuery<T> getQuery()
	{
		return query;
	}

	/**
	 *  Set the query.
	 *  @param query The query to set
	 */
	public void setQuery(ServiceQuery<T> query)
	{
		this.query = query;
	}

	/**
	 *  Get the future.
	 *  @return The future
	 */
	public TerminableIntermediateFuture<T> getFuture()
	{
		return future;
	}

	/**
	 *  Set the future.
	 *  @param future The future to set
	 */
	public void setFuture(TerminableIntermediateFuture<T> future)
	{
		this.future = future;
	}
	
	/**
	 *  Get the remotefuture.
	 *  @return the remotefuture
	 */
	public ISubscriptionIntermediateFuture<T> getRemoteFuture()
	{
		return remotefuture;
	}

	/**
	 *  Set the remotefuture.
	 *  @param remotefuture The remotefuture to set
	 */
	public void setRemoteFuture(ISubscriptionIntermediateFuture<T> remotefuture)
	{
		if(this.remotefuture!=null)
			remotefuture.terminate();
		
		this.remotefuture = remotefuture;
		remotefuture.addResultListener(new UnlimitedIntermediateDelegationResultListener<T>(future));
	}

	/**
	 *  Get the superpeer.
	 *  @return the superpeer
	 */
	public IComponentIdentifier getSuperpeer()
	{
		return superpeer;
	}

	/**
	 *  Set the superpeer.
	 *  @param superpeer The superpeer to set
	 */
	public void setSuperpeer(IComponentIdentifier superpeer)
	{
		this.superpeer = superpeer;
	}
}