package jadex.launch.test;

import jadex.base.Starter;
import jadex.bridge.IExternalAccess;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISuspendable;
import jadex.commons.future.ThreadSuspendable;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 *  Test if the platform terminates itself.
 */
// Todo: Doesn't work on hudson server
// (race condition in init leads to micro factory not being found?)
public class MultiPlatformsTest2 extends TestCase
{
	public void	testMultiplePlatforms()
	{
		int number	= 100;
		long timeout	= 10000;
		
		List<IFuture<IExternalAccess>>	futures	= new ArrayList<IFuture<IExternalAccess>>();
		for(int i=0; i<number; i++)
		{
			if(i%10==0)
				System.out.println("Starting platform "+i);
			futures.add(Starter.createPlatform(new String[]{"-platformname", "testcases", "-niotransport", "false",
				"-configname", "allkernels",	// Todo: does not work with multi-kernel on Hudson!?
				"-gui", "false", 
				"-saveonexit", "false", "-welcome", "false", "-autoshutdown", "false"}));
		}
		
		IExternalAccess[]	platforms	= new IExternalAccess[number];
		ISuspendable	sus	= 	new ThreadSuspendable();
		for(int i=0; i<number; i++)
		{
			if(i%10==0)
				System.out.println("Waiting for platform "+i);
			platforms[i]	= futures.get(i).get(sus, timeout);
		}
		
//		try
//		{
//			Thread.sleep(3000000);
//		}
//		catch(InterruptedException e)
//		{
//		}
		
		for(int i=0; i<number; i++)
		{
			if(i%10==0)
				System.out.println("Killing platform "+i);
			platforms[i].killComponent().get(sus, timeout);
		}
	}
}
