package jadex.bdiv3x.features;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import jadex.bdiv3.actions.FindApplicableCandidatesAction;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.model.MMessageEvent;
import jadex.bdiv3.model.MMessageEvent.Direction;
import jadex.bdiv3.model.MParameter;
import jadex.bdiv3x.runtime.CapabilityWrapper;
import jadex.bdiv3x.runtime.RMessageEvent;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.IMessageFeature;
import jadex.bridge.component.IMsgHeader;
import jadex.bridge.component.impl.ComponentFeatureFactory;
import jadex.bridge.component.impl.IMessagePreprocessor;
import jadex.bridge.component.impl.MessageComponentFeature;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.types.security.ISecurityInfo;
import jadex.commons.SUtil;
import jadex.commons.collection.SCollection;
import jadex.commons.collection.WeakList;
import jadex.commons.transformation.BeanIntrospectorFactory;
import jadex.commons.transformation.traverser.BeanProperty;
import jadex.commons.transformation.traverser.IBeanIntrospector;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.SJavaParser;

/**
 *  Extension to allow message injection in agent methods.
 */
public class BDIXMessageComponentFeature extends MessageComponentFeature	implements IInternalBDIXMessageFeature
{
	//-------- constants --------
	
	/** The factory. */
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(IMessageFeature.class, BDIXMessageComponentFeature.class, IInternalBDIXMessageFeature.class);
	
	//-------- attributes --------
	
	/** Sent message tracking (msg->cnt). */
	protected Map<RMessageEvent<Object>, Integer> sent_mevents;
	
	/** Sent message tracking (insertion order). */
	protected List<RMessageEvent<Object>> sent_meventlist;
	
	/** The maximum number of outstanding messages. */
	protected long mevents_max;
	
	//-------- constructors --------
	
	/**
	 *  Create the feature.
	 */
	public BDIXMessageComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
		
		this.sent_mevents = new WeakHashMap<RMessageEvent<Object>, Integer>();
		this.sent_meventlist = new WeakList<RMessageEvent<Object>>();
	}
	
	//-------- IInternalMessageFeature interface --------
	
	/**
	 *  Test if there are matching message events in XML description.
	 */
	@Override
	protected void processUnhandledMessage(ISecurityInfo secinf, IMsgHeader header, Object body)
	{
		MMessageEvent mevent = null;
			
//		System.out.println("rec msg: "+body+", "+header);
			
		IInternalBDIAgentFeature bdif = (IInternalBDIAgentFeature)getComponent().getFeature(IBDIXAgentFeature.class);

		// Find all matching event models for received message.
		List<MMessageEvent>	events	= SCollection.createArrayList();
		List<MMessageEvent>	matched	= SCollection.createArrayList();
		int	degree	= 0;
			
		// Find original message event to know in which scope the
		//   new message event type should be searched
		// For messages without conversation all capabilities are considered.
		// For messages of ongoing conversations only the source capability is considered.
		RMessageEvent<Object>	original	= getInReplyMessageEvent(body);
		
		// Extract bean properties.
		Map<String, Object>	vals	= new LinkedHashMap<String, Object>();
		if(body!=null)
		{
			IBeanIntrospector	bi	= BeanIntrospectorFactory.getInstance().getBeanIntrospector();
			Map<String, BeanProperty>	bprops	= bi.getBeanProperties(body.getClass(), true, false);
			for(Map.Entry<String, BeanProperty> bprop: bprops.entrySet())
			{
				Object	value	= bprop.getValue().getPropertyValue(body);
				vals.put(bprop.getKey(), value);
				vals.put(SUtil.camelToSnakeCase(bprop.getKey()), value);
			}
		}

		
		degree = matchMessageEvents(vals, bdif.getBDIModel().getCapability().getMessageEvents(), matched, events, degree,
			original!=null, original==null ? null : original.getModelElement().getCapabilityName());

		if(events.size()==0)
		{
			getComponent().getLogger().severe(getComponent().getId()+" cannot process message, no message event matches: "+body+", "+header);
		}
		else
		{
			if(events.size()>1)
			{
				// Multiple matches of highest degree.
				getComponent().getLogger().severe(getComponent().getId()+" cannot decide which event matches message, " +
					"using first: "+body+", "+header+", "+events);
			}
			else if(matched.size()>1)
			{
				// Multiple matches but different degrees.
				getComponent().getLogger().info(getComponent().getId()+" multiple events matching message, using " +
					"message event with highest specialization degree: "+body+", "+header+" ("+degree+"), "+events.get(0)+", "+matched);
			}
				
			mevent = events.get(0);
		}
			
		if(mevent!=null)
		{
			RMessageEvent<Object> revent = new RMessageEvent<Object>(mevent, body, getInternalAccess(), original);
			FindApplicableCandidatesAction fac = new FindApplicableCandidatesAction(revent);
			getComponent().getFeature(IExecutionFeature.class).scheduleStep(fac);
		}
	}
	
	/**
	 *  Match message events with a message adapter.
	 */
	protected int matchMessageEvents(Map<String, Object> message, List<MMessageEvent> mevents, List<MMessageEvent> matched, List<MMessageEvent> events, int degree, boolean checkscope, String scope)
	{
		for(MMessageEvent mevent: mevents)
		{
			if(!checkscope || SUtil.equals(mevent.getCapabilityName(), scope))
			{
				Direction dir = mevent.getDirection();
				if(dir==null)
					System.out.println("null");
	
				try
				{
					if((dir.equals(Direction.RECEIVE)
						|| dir.equals(Direction.SENDRECEIVE))
						&& match(mevent, message))
					{
						matched.add(mevent);
						if(mevent.getSpecializationDegree()>degree)
						{
							degree	= mevent.getSpecializationDegree();
							events.clear();
							events.add(mevent);
						}
						else if(mevent.getSpecializationDegree()==degree)
						{
							events.add(mevent);
						}
					}
				}
				catch(RuntimeException e)
				{
					StringWriter	sw	= new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					getComponent().getLogger().severe(sw.toString());
				}
			}
		}
		return degree;
	}
		
	/**
	 *  Match a message with a message event.
	 *  @param msgevent The message event.
	 *  @return True, if message matches the message event.
	 */
	protected boolean match(MMessageEvent msgevent, Map<String, Object> msg)
	{
		boolean	match	= true;

		// Match against parameters specified in the event type.
		for(MParameter param: msgevent.getParameters())
		{
			if(param.getDirection().equals(MParameter.Direction.FIXED) && param.getDefaultValue()!=null)
			{
				Object pvalue = msg.get(param.getName());
				Object mvalue = SJavaParser.parseExpression(param.getDefaultValue(), getComponent().getModel().getAllImports(), 
					getComponent().getClassLoader()).getValue(CapabilityWrapper.getFetcher(getInternalAccess(), param.getDefaultValue().getLanguage()));
				if(!SUtil.equals(pvalue, mvalue))
				{
					match	= false;
					break;
				}

//				System.out.println("matched "+msgevent.getName()+"."+param.getName()+": "+pvalue+", "+mvalue+", "+match);
			}
		}

		// todo:
		// Match against parameter sets specified in the event type.
		// todo: this implements a default strategy for param sets by checking if all values
		// todo: of the message event are also contained in the native message
		// todo: this allows further values being contained in the native message
//			IMParameterSet[]	paramsets	= msgevent.getParameterSets();
//			for(int i=0; match && i<paramsets.length; i++)
//			{
//				if(paramsets[i].getDirection().equals(IMParameterSet.DIRECTION_FIXED))
//				{
//					// Create and save the default values that must be contained in the native message to match.
//					List vals = new ArrayList();
//					if(paramsets[i].getDefaultValues().length>0)
//					{
//						IMExpression[] dvs = paramsets[i].getDefaultValues();
//						for(int j=0; j<dvs.length; j++)
//							vals.add(RExpression.evaluateExpression(dvs[i], null)); // Hack!
//					}
//					else if(paramsets[i].getDefaultValuesExpression()!=null)
//					{
//						Iterator it = SReflect.getIterator(RExpression.evaluateExpression(paramsets[i].getDefaultValuesExpression(), null)); // Hack!
//						while(it.hasNext())
//							vals.add(it.next());
//					}
//
//					// Create the message values and store them in a set for quick contains tests.
//					Object mvalue = getValue(paramsets[i].getName(), scope);
//					Set mvals = new HashSet();
//					Iterator	it	= SReflect.getIterator(mvalue);
//					while(it.hasNext())
//						mvals.add(it.next());
//					// Match each required value of the list.
//					match = mvals.containsAll(vals);
//					//System.out.println("matched "+msgevent.getName()+"."+params[i].getName()+": "+pvalue+", "+mvalue+", "+match);
//				}
//			}

		// Match against match expression.
		UnparsedExpression matchexp = msgevent.getMatchExpression();
		if(match && matchexp!=null)
		{
			Map<String, Object> exparams = new HashMap<String, Object>();
			
//			List<String> names = new ArrayList<String>();
//			for(String name: mt.getParameterNames())
//				names.add(name);
//			for(String name: mt.getParameterSetNames())
//				names.add(name);
//			
//			for(String name: mt.getParameterNames())
//			{
//				try
//				{
//					Object pvalue = msg.get(name);
//					// Hack! converts "-" to "_" because variable names must not contain "-" in Java
//					String paramname = "$"+ SUtil.replace(name, "-", "_");
//					exparams.put(paramname, pvalue);
//				}
//				catch(Exception e)
//				{
//					e.printStackTrace();
//				}
//			}
			
//			for(int i=0; i<params.length; i++)
//			{
//				try
//				{
//					Object	mvalue	= getValue(params[i].getName(), scope);
//					// Hack! converts "-" to "_" because variable names must not contain "-" in Java
//					String paramname = "$"+ SUtil.replace(params[i].getName(), "-", "_");
//					exparams.put(paramname, mvalue);
//				}
//				catch(Exception e)
//				{
//					e.printStackTrace();
//				}
//			}
//			for(int i=0; i<paramsets.length; i++)
//			{
//				try
//				{
//					Object mvalue = getValue(paramsets[i].getName(), scope);
//					// Hack! converts "-" to "_" because variable names must not contain "-" in Java
//					String paramsetname = "$"+SUtil.replace(paramsets[i].getName(), "-", "_");
//					exparams.put(paramsetname, mvalue);
//				}
//				catch(Exception e)
//				{
//					e.printStackTrace();
//				}
//			}

			try
			{
				exparams.put("$messagemap", msg);
				for(String prop: msg.keySet())
				{
					exparams.put("$"+prop, msg.get(prop));
					// Convert CamelCase to snake_case for FIPA backwards compatibility
			        exparams.put("$"+SUtil.camelToSnakeCase(prop), msg.get(prop));
//			        System.out.println("added: $"+prop+"/"+snake_case+" -> "+msg.get(prop));
				}
				IParsedExpression exp = SJavaParser.parseExpression(matchexp, getComponent().getModel().getAllImports(), getComponent().getClassLoader());
				match = ((Boolean)exp.getValue(CapabilityWrapper.getFetcher(getInternalAccess(), matchexp.getLanguage(), exparams))).booleanValue();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				match = false;
			}
		}

		return match;
	}
	
	/**
	 *  Register a conversation or reply-with to be able
	 *  to send back answers to the source capability.
	 *  @param msgevent The message event.
	 */
	public void registerMessageEvent(RMessageEvent<Object> msgevent)
	{
		if(mevents_max!=0 && sent_mevents.size()>mevents_max)
		{
			getComponent().getLogger().severe("Agent does not save conversation due " +
				"to too many outstanding messages. Increase buffer in runtime.xml - storedmessages.size");
		}
		else
		{
			if(sent_mevents.containsKey(msgevent))
			{
				sent_mevents.put(msgevent, sent_mevents.get(msgevent)+1);
			}
			else
			{
				sent_mevents.put(msgevent, 1);
				sent_meventlist.add(msgevent);
			}
		}
	}
	
	/**
	 *  Deregister a conversation or reply-with.
	 *  @param msgevent The message event.
	 */
	public void deregisterMessageEvent(RMessageEvent<Object> msgevent)
	{
		if(sent_mevents.containsKey(msgevent))
		{
			int	cnt = sent_mevents.get(msgevent)-1;
			if(cnt>0)
			{
				sent_mevents.put((RMessageEvent<Object>)msgevent, cnt);
			}
			else
			{
				sent_mevents.remove(msgevent);
				sent_meventlist.remove(msgevent);
			}
		}
	}
	
	/**
	 *  Find a message event that the given native message is a reply to.
	 *  @param message The (native) message.
	 */
	public RMessageEvent<Object> getInReplyMessageEvent(Object message)
	{
//		System.out.println("+++"+getComponent().getComponentIdentifier()+" has open conversations: "+sent_mevents.size()+" "+sent_mevents);
		
		RMessageEvent<Object>	ret	= null;
		@SuppressWarnings("unchecked")
		RMessageEvent<Object>[] smes = (RMessageEvent[])sent_meventlist.toArray(new RMessageEvent[0]);

		// Reverse loop -> prefer the newest messages for finding replies.
		// todo: conversations should be better supported
		// todo: indexing for msgevents for speed.
		for(int i=smes.length-1; i>-1; i--)
		{
			IMessagePreprocessor<Object>	proc	= getPreprocessor(message);
			if(proc!=null && proc.isReply(smes[i].getMessage(), message))
			{
				// Break tie by using shorter (full) capability name -> prefers outer capa before inner (e.g. cnp_cap vs. cnp_cap/cm_cap).
				if(ret==null || smes[i].getModelElement().getCapabilityName()==null
					|| smes[i].getModelElement().getCapabilityName().length()<(ret.getModelElement().getCapabilityName()==null ? 0 : ret.getModelElement().getCapabilityName().length()))
				{
					ret	= smes[i];
				}
			}
		}
		
//		System.out.println("is reply? "+message+", "+ret+", "+sent_mevents);
		
		return ret;
	}
}
