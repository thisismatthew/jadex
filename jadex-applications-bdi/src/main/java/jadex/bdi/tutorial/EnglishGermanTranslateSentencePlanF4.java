package jadex.bdi.tutorial;

import jadex.adapter.base.fipa.IDF;
import jadex.adapter.base.fipa.IDFAgentDescription;
import jadex.adapter.base.fipa.IDFServiceDescription;
import jadex.adapter.base.fipa.SFipa;
import jadex.bdi.runtime.GoalFailureException;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IMessageEvent;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IAgentIdentifier;

import java.util.StringTokenizer;

/**
 *  Tries to translate a sentence word by word
 *  using a translation service for words.
 */
public class EnglishGermanTranslateSentencePlanF4 extends Plan
{
	//-------- attributes --------

	/** The translation agent. */
	protected IAgentIdentifier ta;

	//-------- constructors --------

	/**
	 *  Create a new plan.
	 */
	public EnglishGermanTranslateSentencePlanF4()
	{
		getLogger().info("Created:"+this);
	}

	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		while(true)
		{
			// Read the user request.
			IMessageEvent mevent = waitForMessageEvent("request_translatesentence");

			// Save the words of the sentence.
			String cont = (String)mevent.getParameter(SFipa.CONTENT).getValue();
			StringTokenizer stok = new StringTokenizer(cont, " ");
			stok.nextToken();
			stok.nextToken();
			String[] words = new String[stok.countTokens()];
			String[] twords = new String[words.length];
			for(int i=0; stok.hasMoreTokens(); i++)
			{
				words[i] = stok.nextToken();
			}

			// Search a translation agent.
			while(ta==null)
			{
				IDF	dfservice	= (IDF)getScope().getPlatform().getService(IDF.class);
				
				// Create a service description to search for.
				IDFServiceDescription sd = dfservice.createDFServiceDescription(null, "translate english_german", null);
				IDFAgentDescription ad = dfservice.createDFAgentDescription(getAgentIdentifier(), sd);
				
				/*ServiceDescription sd = new ServiceDescription();
				sd.setType("translate english_german");
				AgentDescription ad = new AgentDescription();
				dfadesc.addService(sd);*/

				// Use a subgoal to search for a translation agent
				IGoal ft = createGoal("df_search");
                ft.getParameter("description").setValue(ad);
              	//ft.getParameter("df", null); //new AID("df@vsispro3:1099/JADE") mit "http://vsispro3.informatik.uni-hamburg.de:1521/acc")

				dispatchSubgoalAndWait(ft);
                //Object result = ft.getResult();
                IDFAgentDescription[]	result = (IDFAgentDescription[])ft.getParameterSet("result").getValues();

				if(result.length>0)
				{
					this.ta = result[0].getName();
				}
				else
				{
					//if(result instanceof Exception)
					//	((Exception)result).printStackTrace();
					System.out.println("No translation agent found.");
					waitFor(5000);
				}
			}

			// Translate the words.
			for(int i=0; i<words.length; i++)
			{
				//getLogger().info("Asking now: "+words[i]);
		     	IGoal tw = createGoal("rp_initiate");
		   		tw.getParameter("action").setValue("translate english_german "+words[i]);
				tw.getParameter("receiver").setValue(this.ta);
				try
				{
					dispatchSubgoalAndWait(tw);
					//twords[i] = (String)tw.getResult();
					twords[i] = (String)tw.getParameter("result").getValue();
				}
				catch(GoalFailureException gfe)
				{
					twords[i] = "n/a";
				}
			}

			// Send the reply.
			StringBuffer buf = new StringBuffer();
			buf.append("Translated: ");
			for(int i=0; i<words.length; i++)
			{
				buf.append(words[i]+" ");
			}
			buf.append("\nto: ");
			for(int i=0; i<words.length; i++)
			{
				buf.append(twords[i]+" ");
			}
			//getLogger().info(buf.toString());

			IMessageEvent rep = getEventbase().createReply(mevent, "inform");
			rep.getParameter(SFipa.CONTENT).setValue(buf.toString());
			sendMessage(rep);
		}
	}
}
