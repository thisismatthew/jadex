package jadex.extension.rs.publish;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.grizzly.http.server.Request;

/**
 * 
 */
public class SInvokeHelper 
{
	/**
	 *  Convert a multimap to normal map.
	 */
	public static Map<String, Object> convertMultiMap(MultivaluedMap<String, String> vals)
	{
		Map<String, Object> ret = new HashMap<String, Object>();
		
		boolean multimap = false;
		for(Map.Entry<String, List<String>> e: vals.entrySet())
		{
			List<String> val = e.getValue();
			multimap = val!=null && val.size()>1;
			if(multimap)
				break;
		}
		
		for(Map.Entry<String, List<String>> e: vals.entrySet())
		{
			List<String> val = e.getValue();
			if(val==null || val.size()==0)
			{
				ret.put(e.getKey(), null);
			}
			else
			{
				if(multimap)
				{
					String[] va = new String[val.size()]; 
					for(int i=0; i<va.length; i++)
					{
						va[i] = val.get(i);
					}				
					ret.put(e.getKey(), va);
				}
				else
				{
					ret.put(e.getKey(), val.iterator().next());
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Convert a map with arrays to normal map when no multi values exist.
	 */
	public static Map<String, Object> convertMultiMap(Map<String, String[]> vals)
	{
		Map<String, Object> ret = (Map)vals;
		
		boolean multimap = false;
		for(Map.Entry<String, String[]> e: vals.entrySet())
		{
			String[] val = e.getValue();
			multimap = val!=null && val.length>1;
			if(multimap)
				break;
		}
		
		if(!multimap)
		{
			ret = new HashMap<String, Object>();
			for(Map.Entry<String, String[]> e: vals.entrySet())
			{
				String[] val = e.getValue();
				if(val==null || val.length==0)
				{
					ret.put(e.getKey(), null);
				}
				else
				{
					ret.put(e.getKey(),val[0]);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Convert a map with arrays to normal map when no multi values exist.
	 */
	public static MultivaluedMap<String, String> convertToMultiMap(Map<String, String[]> vals)
	{
		MultivaluedMap<String, String> ret = new MultivaluedHashMap<String, String>();
		
		for(Map.Entry<String, String[]> e: vals.entrySet())
		{
			String[] val = e.getValue();
			
			if(val!=null && val.length>0)
			{
				for(String v: val)
				{
					ret.add(e.getKey(), v);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Extract caller values like ip and browser.
	 *  @param request The requrest.
	 *  @param vals The values.
	 */
	public static Map<String, String> extractCallerValues(Object request)
	{
		Map<String, String> ret = new HashMap<String, String>();
		
		// add request to map as internal parameter
		// cannot put request into map because map is cloned via service call
		if(request!=null)
		{
			if(request instanceof Request)
			{
				Request greq = (Request)request;
				ret.put("ip", greq.getRemoteAddr());
				ret.put("browser", greq.getHeader("User-Agent"));
			}
			else if(request instanceof HttpServletRequest)
			{
				HttpServletRequest sreq = (HttpServletRequest)request;
				ret.put("ip", sreq.getRemoteAddr());
				ret.put("browser", sreq.getHeader("User-Agent"));
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 */
	public static void debug(Object req)
	{
		if(req instanceof Request)
		{
			try
			{
				Request r = (Request)req;
				Field f = r.getClass().getDeclaredField("usingInputStream");
				f.setAccessible(true);
				f.set(r, Boolean.FALSE);
				System.out.println("params: "+r.getParameterNames());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		System.out.println(req);
	}
}
