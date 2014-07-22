package jadex.bridge.service.component;

import jadex.bridge.service.IService;

/**
 *  Component feature for provided services.
 */
public interface IProvidedServicesFeature
{
//	/**
//	 *  Add a service to the container.
//	 *  The service is started, if the container is already running.
//	 *  @param service The service.
//	 *  @param info The provided service info.
//	 *  @return A future that is done when the service has completed starting.  
//	 */
//	public IFuture<Void>	addService(IInternalService service, ProvidedServiceInfo info);
//
//	/**
//	 *  Removes a service from the container (shutdowns also the service if the container is running).
//	 *  @param service The service identifier.
//	 *  @return A future that is done when the service has completed its shutdown.  
//	 */
//	public IFuture<Void>	removeService(IServiceIdentifier sid);
		
	/**
	 *  Get provided (declared) service.
	 *  @param name The service name.
	 *  @return The service.
	 */
	public IService getProvidedService(String name);
	
	/**
	 *  Get provided (declared) service.
	 *  @param clazz The interface.
	 *  @return The service.
	 */
	public <T> T getProvidedService(Class<T> clazz);

	/**
	 *  Get provided (declared) service.
	 *  @param clazz The interface.
	 *  @return The service.
	 */
	public <T> T[] getProvidedServices(Class<T> clazz);
	
	/**
	 *  Get the provided service
	 */
	public <T> T getProvidedServiceRawImpl(Class<T> clazz);
}