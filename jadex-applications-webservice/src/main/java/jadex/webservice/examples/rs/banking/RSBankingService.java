package jadex.webservice.examples.rs.banking;

import jadex.commons.future.IFuture;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.core.ResourceConfig;

/**
 * 
 */
@Path("/banking")
//Jadex service can be fetched via url from properties
//String url = uriinfo.getBaseUri().toString();
//IBankingService bs = (IBankingService)rc.getProperties().get(url);
public class RSBankingService
{
	@Context 
	public ResourceConfig rc;
	
	@Context
	public UriInfo uriinfo;

	/**
	 *  Get the account statement.
	 *  @param request The request.
	 *  @return The account statement.
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getServiceInfo()
	{
		return "<html><body></body><h1>info</h1></html>";
	}
	
//	/**
//	 *  Get the account statement.
//	 *  @param request The request.
//	 *  @return The account statement.
//	 */
//	@POST
//	@Path("getAccountStatement")
//	@Produces(MediaType.TEXT_XML)
//	public String getAccountStatement(@PathParam("request") String request)
//	{
//		System.out.println("getAccountStatement");
//		String url = uriinfo.getBaseUri().toString();
//		IBankingService bs = (IBankingService)rc.getProperties().get(url);
//		Request req;
//		try
//		{
//			req = JavaReader.objectFromXML(request, null);
//		}
//		catch(Exception e)
//		{
//			req = new Request(new Date(), new Date());
//		}
//		AccountStatement ret = bs.getAccountStatement(req).get(new ThreadSuspendable(this));
//		
//		return JavaWriter.objectToXML(ret, null);
//	}
	
//	/**
//	 *  Get the account statement.
//	 *  @param request The request.
//	 *  @return The account statement.
//	 */
//	@GET
//	@Path("getAccountStatement/{request}")
//	@Produces(MediaType.APPLICATION_XML)
//	public String getAccountStatement(@PathParam("request") Request request)
//	{
//		System.out.println("getAccountStatement");
//		String url = uriinfo.getBaseUri().toString();
//		IBankingService bs = (IBankingService)rc.getProperties().get(url);
//		AccountStatement ret = bs.getAccountStatement(request).get(new ThreadSuspendable(this));
//		
//		return JavaWriter.objectToXML(ret, null);
//	}	
	
//	
//	/**
//	 *  Add an account statement.
//	 *  @param data The data.
//	 */
//	@POST
//	@Path("addTransactionData")
//	public void addTransactionData(String data)
//	{
//		System.out.println("addTransactionData");	
//	}
//	
//	/**
//	 *  Remove an account statement.
//	 *  @param data The data.
//	 */
//	@DELETE
//	@Path("removeTransactionData")
//	public void removeTransactionData(String data)
//	{
//		System.out.println("removeTransactionData");	
//	}
	
//	/**
//	 *  Get the account statement.
//	 *  @param request The request.
//	 *  @return The account statement.
//	 */
//	@GET
//	@Produces(MediaType.TEXT_HTML)
//	public String getHello()
//	{
//		System.out.println("invoked getHello");
//		return "<html><body><h1>Banking Hello</h1></body></html>";
//	}	
}
