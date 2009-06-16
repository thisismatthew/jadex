package jadex.bdi.examples.marsworld_env.sentry;

import java.util.HashMap;
import java.util.Map;

import jadex.adapter.base.agr.AGRSpace;
import jadex.adapter.base.agr.Group;
import jadex.adapter.base.envsupport.environment.IEnvironmentSpace;
import jadex.adapter.base.envsupport.environment.ISpaceObject;
import jadex.adapter.base.envsupport.environment.space2d.Space2D;
import jadex.adapter.base.fipa.SFipa;
import jadex.bdi.examples.cleanerworld_env.cleaner.LoadBatteryTask;
import jadex.bdi.examples.marsworld_env.RequestProduction;
import jadex.bdi.examples.marsworld_env.producer.ProduceOreTask;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IMessageEvent;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IAgentIdentifier;


/**
 *  Inform the sentry agent about a new target.
 */
public class AnalyzeTargetPlan extends Plan
{
	//-------- constructors --------

	/**
	 *  Create a new plan.
	 */
	public AnalyzeTargetPlan()
	{
		getLogger().info("Created: "+this);
	}

	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		ISpaceObject target = (ISpaceObject)getParameter("target").getValue();

		// Move to the target.
		IGoal go_target = createGoal("move.move_dest");
		go_target.getParameter("destination").setValue(target.getProperty(Space2D.PROPERTY_POSITION));
		dispatchSubgoalAndWait(go_target);

		// Analyse the target.
		try
		{
			ISpaceObject	myself	= (ISpaceObject)getBeliefbase().getBelief("move.myself").getFact();
			SyncResultListener	res	= new SyncResultListener();
			Map props = new HashMap();
			props.put(AnalyzeTargetTask.PROPERTY_TARGET, target);
			props.put(AnalyzeTargetTask.PROPERTY_LISTENER, res);
			IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("move.environment").getFact();
			space.createObjectTask(AnalyzeTargetTask.PROPERTY_TYPENAME, props, myself.getId());

//			myself.addTask(new AnalyzeTargetTask(target, res));
			Number	ore	= (Number)res.waitForResult();
//			System.out.println("Analyzed target: "+getAgentName()+", "+ore+" ore found.");
			if(ore.intValue()>0)
				callProducerAgent(target);
	
			// Hack??? Should be done in task, but aborts plan before producers are called.
			target.setProperty(AnalyzeTargetTask.PROPERTY_STATE, AnalyzeTargetTask.STATE_ANALYZED);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			// Fails for one agent, when two agents try to analyze the same target at once.
		}
	}

	/**
	 *  Sending a location to the Producer Agent.
	 *  Therefore it has first to be looked up in the DF.
	 *  @param target
	 */
	private void callProducerAgent(ISpaceObject target)
	{
//		System.out.println("Calling some Production Agent...");

		AGRSpace agrs = (AGRSpace)getScope().getApplicationContext().getSpace("myagrspace");
		Group group = agrs.getGroup("mymarsteam");
		IAgentIdentifier[]	producers	= group.getAgentsForRole("producer");

		if(producers!=null && producers.length>0)
		{
			int sel = (int)(Math.random()*producers.length); // todo: Select not randomly
//			System.out.println("Found agents: "+producers.length+" selected: "+sel);

			RequestProduction rp = new RequestProduction();
			rp.setTarget(target);
			//Action action = new Action();
			//action.setAction(rp);
			//action.setActor(SJade.convertAIDtoJade(producers[sel].getName()));
			IMessageEvent mevent = createMessageEvent("request_producer");
			mevent.getParameterSet(SFipa.RECEIVERS).addValue(producers[sel]);
			mevent.getParameter(SFipa.CONTENT).setValue(rp);
			sendMessage(mevent);
//			System.out.println("Sentry Agent: sent location to: "+producers[sel].getName());
		}
	}
}
