package jadex.micro.examples.helpline;

import jadex.bridge.Argument;
import jadex.bridge.IArgument;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.collection.MultiCollection;
import jadex.commons.concurrent.DefaultResultListener;
import jadex.commons.service.RequiredServiceInfo;
import jadex.commons.service.SServiceProvider;
import jadex.commons.service.clock.IClockService;
import jadex.micro.IMicroExternalAccess;
import jadex.micro.MicroAgent;
import jadex.micro.MicroAgentMetaInfo;

import java.util.Collection;
import java.util.Iterator;

import javax.swing.SwingUtilities;

/**
 *  Helpline micro agent. 
 */
public class HelplineAgent extends MicroAgent
{
	//-------- attributes --------
	
	/** The map of information. */
	protected MultiCollection infos;
	
	//-------- methods --------
	
	/**
	 *  Called once after agent creation.
	 */
	public void agentCreated()
	{
//		this.infos = new MultiCollection(new HashMap(), TreeSet.class);
		this.infos = new MultiCollection();
		Object ini = getArgument("infos");
		if(ini!=null && SReflect.isIterable(ini))
		{
			for(Iterator it=SReflect.getIterator(ini); it.hasNext(); )
			{
				InformationEntry ie = (InformationEntry)it.next();
				infos.put(ie.getName(), ie);
			}
		}
//		addService(new HelplineService(getExternalAccess()));
		addService(new HelplineService(this));
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				HelplinePanel.createHelplineGui((IMicroExternalAccess)getExternalAccess());
			}
		});
	}
		
	/**
	 *  Add an information about a person.
	 *  @param name The person's name.
	 *  @param info The information.
	 */
	public void addInformation(final String name, final String info)
	{
//		SServiceProvider.getService(getServiceProvider(), IClockService.class)
		getRequiredService("clockservice")
			.addResultListener(createResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object result)
			{
				IClockService cs = (IClockService)result;
				infos.put(name, new InformationEntry(name, info, cs.getTime()));
			}
		}));
	}
	
	/**
	 *  Get all locally stored information about a person.
	 *  @param name The person's name.
	 *  @return Future that contains the information.
	 */
	public Collection getInformation(String name)
	{
		return (Collection)infos.get(name);
	}
	
	//-------- static methods --------

	/**
	 *  Get the meta information about the agent.
	 */
	public static MicroAgentMetaInfo getMetaInfo()
	{
		return new MicroAgentMetaInfo("This agent offers a helpline for getting information about missing persons.", null, 
			new IArgument[]{new Argument("infos", "Initial information records.", "InformationEntry[]")}
			, null, null, SUtil.createHashMap(new String[]{"componentviewer.viewerclass"}, new Object[]{"jadex.micro.examples.helpline.HelplineViewerPanel"}),
			new RequiredServiceInfo[]{new RequiredServiceInfo("clockservice", IClockService.class),
			new RequiredServiceInfo("remotehelplineservices", IHelpline.class, true, true, true, true),
			new RequiredServiceInfo("localhelplineservices", IHelpline.class, true, true, false, true)}, 
			new Class[]{IHelpline.class});
	}

}
