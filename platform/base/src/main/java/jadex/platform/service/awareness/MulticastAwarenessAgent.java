package jadex.platform.service.awareness;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import jadex.bridge.service.annotation.OnEnd;
import jadex.bridge.service.annotation.OnInit;
import jadex.bridge.service.annotation.Service;
import jadex.commons.Boolean3;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;

/**
 *  Implements passive awareness via multicast.
 */
@Service
@Agent(autoprovide = Boolean3.TRUE, autostart=Boolean3.FALSE)
@Arguments({
	@Argument(name="address", clazz=String.class, defaultvalue="\"232.0.9.1\""),
	@Argument(name="port", clazz=int.class, defaultvalue="32091")
})
public class MulticastAwarenessAgent	extends LocalNetworkAwarenessBaseAgent
{
	/**
	 *  At startup create a multicast socket for listening.
	 */
	//@AgentCreated
	@OnInit
	public void	start() throws Exception
	{
		sendsocket	= new DatagramSocket(0);
		recvsocket = new MulticastSocket(port);
		((MulticastSocket)recvsocket).joinGroup(InetAddress.getByName(address));
		
		super.init();
	}

	/**
	 * Stop the service.
	 */
//	@ServiceShutdown
	@OnEnd
	public void shutdown() throws Exception
	{
		((MulticastSocket)recvsocket).leaveGroup(InetAddress.getByName(address));
		
		super.shutdown();
	}
}
