package jadex.adapter.base.envsupport.environment.space2d;

import jadex.adapter.base.envsupport.environment.AbstractEnvironmentSpace;
import jadex.adapter.base.envsupport.environment.EnvironmentEvent;
import jadex.adapter.base.envsupport.environment.IEnvironmentSpace;
import jadex.adapter.base.envsupport.environment.IPerceptGenerator;
import jadex.adapter.base.envsupport.environment.ISpaceObject;
import jadex.adapter.base.envsupport.environment.PerceptType;
import jadex.adapter.base.envsupport.math.IVector1;
import jadex.adapter.base.envsupport.math.IVector2;
import jadex.adapter.base.envsupport.math.Vector1Double;
import jadex.bridge.IAgentIdentifier;
import jadex.bridge.IApplicationContext;
import jadex.bridge.ISpace;
import jadex.commons.IPropertyObject;
import jadex.commons.SimplePropertyObject;
import jadex.commons.collection.MultiCollection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  Percept generator for moving agents.
 */
public class DefaultVisionGenerator extends SimplePropertyObject implements IPerceptGenerator
{
	//-------- constants --------

	/** The maxrange property. */
	public static String PROPERTY_MAXRANGE = "range";

	/** The maxrange property. */
	public static String PROPERTY_RANGE = "range_property";
	
	/** The percept types property. */
	public static String PROPERTY_PERCEPTTYPES = "percepttypes";
	
	
	/** The appeared percept type. */
	public static String APPEARED = "appeared";
	
	/** The disappeared percept type. */
	public static String DISAPPEARED = "disappeared";
	
	/** The created percept type. */
	public static String CREATED = "created";
	
	/** The destroyed percept type. */
	public static String DESTROYED = "destroyed";
	
	/** The moved percept type. */
	public static String MOVED = "moved";
	

	/** Empty spaceobjects array. */
	protected static final ISpaceObject[] EMPTY_SPACEOBJECTS = new ISpaceObject[0];
	
	//-------- attributes --------
	
	/** The percept receiving agent types. */
	protected Map actiontypes;
	
	//-------- IPerceptGenerator --------
		
	/**
	 *  Called when an agent was added to the space.
	 *  @param agent The agent identifier.
	 *  @param space The space.
	 */
	public void agentAdded(IAgentIdentifier agent, ISpace space)
	{
	}
	
	/**
	 *  Called when an agent was remove from the space.
	 *  @param agent The agent identifier.
	 *  @param space The space.
	 */
	public void agentRemoved(IAgentIdentifier agent, ISpace space)
	{
	}
	
	//-------- IEnvironmentListener --------
	
	/**
	 *  Dispatch an environment event to this listener.
	 *  @param event The event.
	 */
	public void dispatchEnvironmentEvent(EnvironmentEvent event)
	{
		Space2D	space = (Space2D)event.getSpace();
		IVector1 maxrange = getRange(this);
		
		IVector2 pos = (IVector2)event.getSpaceObject().getProperty(Space2D.POSITION);
		IVector2 oldpos = (IVector2)event.getInfo();
		IAgentIdentifier eventowner	= (IAgentIdentifier)event.getSpaceObject().getProperty(ISpaceObject.PROPERTY_OWNER);

		if(EnvironmentEvent.OBJECT_POSITION_CHANGED.equals(event.getType()))
		{
			ISpaceObject[] objects = pos==null? EMPTY_SPACEOBJECTS: space.getNearObjects(pos, maxrange, null);
			Set	unchanged;
			ISpaceObject[] oldobjects = null;
			if(oldpos!=null)
			{
				oldobjects = space.getNearObjects(oldpos, maxrange, null);
				unchanged = new HashSet(Arrays.asList(objects));
				unchanged.retainAll(Arrays.asList(oldobjects));
			}
			else
			{
				unchanged	= Collections.EMPTY_SET;
			}
			
			// Objects, which are in current range, but not previously seen.
			for(int i=0; i<objects.length; i++)
			{
				IVector2 objpos = (IVector2)objects[i].getProperty(Space2D.POSITION);

				if(!unchanged.contains(objects[i]))
				{
					// Create event for agent that is seen by moving agent.
					IAgentIdentifier owner = (IAgentIdentifier)objects[i].getProperty(ISpaceObject.PROPERTY_OWNER);
					if(owner!=null)
					{
						if(!getRange(objects[i]).greater(space.getDistance(pos==null? oldpos: pos, objpos)))
						{
							String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), event.getSpaceObject().getType(), APPEARED);
							if(percepttype!=null)
								((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, event.getSpaceObject(), owner);
						}
					}
					
					// Create event for moving agent.
					if(eventowner!=null)
					{
						if(!getRange(event.getSpaceObject()).greater(space.getDistance(pos==null? oldpos: pos, objpos)))
						{
							String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), objects[i].getType(), APPEARED);
							if(percepttype!=null)
								((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, objects[i], eventowner);
						}
					}
				}
				
				// Post movement to agents that stayed in vision range
				IAgentIdentifier owner = (IAgentIdentifier)objects[i].getProperty(ISpaceObject.PROPERTY_OWNER);
				if(owner!=null)
				{
					if(!getRange(objects[i]).greater(space.getDistance(pos==null? oldpos: pos, objpos)))
					{
						String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), event.getSpaceObject().getType(), MOVED);
						if(percepttype!=null)
							((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, event.getSpaceObject(), owner);
					}
				}
			}

			// Objects, which were previously seen, but are no longer in range.
			for(int i=0; oldobjects!=null && i<oldobjects.length; i++)
			{
				IAgentIdentifier owner = (IAgentIdentifier)oldobjects[i].getProperty(ISpaceObject.PROPERTY_OWNER);
				IVector2 objpos = (IVector2)oldobjects[i].getProperty(Space2D.POSITION);

				if(owner!=null)
				{
					if(!getRange(oldobjects[i]).greater(space.getDistance(pos==null? oldpos: pos, objpos)))
					{
						String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), event.getSpaceObject().getType(), DISAPPEARED);
						if(percepttype!=null)
							((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, event.getSpaceObject(), owner);
					}
				}
				
				if(!unchanged.contains(oldobjects[i]))
				{
					if(eventowner!=null)	
					{
						if(!getRange(event.getSpaceObject()).greater(space.getDistance(pos==null? oldpos: pos, objpos)))
						{
							String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), oldobjects[i].getType(), DISAPPEARED);
							if(percepttype!=null)
								((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, oldobjects[i], eventowner);
						}
					}
				}
			}		
		}
		else if(EnvironmentEvent.OBJECT_CREATED.equals(event.getType()))
		{
			if(pos!=null)
			{
				ISpaceObject[]	objects	= space.getNearObjects(pos, maxrange, null);
				
				// Post appearance for object itself (if agent) as well as all agents in vision range
				for(int i=0; i<objects.length; i++)
				{
					IAgentIdentifier owner = (IAgentIdentifier)objects[i].getProperty(ISpaceObject.PROPERTY_OWNER);
					IVector2 objpos = (IVector2)objects[i].getProperty(Space2D.POSITION);
					if(owner!=null)
					{
						if(!getRange(objects[i]).greater(space.getDistance(pos, objpos)))
						{
							String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), event.getSpaceObject().getType(), CREATED);
							if(percepttype!=null)
								((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, event.getSpaceObject(), owner);
						}
					}
					
					if(eventowner!=null)
					{
						if(!getRange(event.getSpaceObject()).greater(space.getDistance(pos==null? oldpos: pos, objpos)))
						{
							String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), objects[i].getType(), CREATED);
							if(percepttype!=null)
								((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, objects[i], eventowner);
						}
					}
				}
			}
		}
		else if(EnvironmentEvent.OBJECT_DESTROYED.equals(event.getType()))
		{
			if(pos!=null)
			{
				ISpaceObject[]	objects	= space.getNearObjects(pos, maxrange, null);
				
				// Post disappearance for all agents in vision range
				for(int i=0; i<objects.length; i++)
				{
					IAgentIdentifier owner = (IAgentIdentifier)objects[i].getProperty(ISpaceObject.PROPERTY_OWNER);
					if(owner!=null)
					{
						IVector2 objpos = (IVector2)objects[i].getProperty(Space2D.POSITION);
						if(!getRange(objects[i]).greater(space.getDistance(pos, objpos)))
						{
							String percepttype = getPerceptType(space, ((IApplicationContext)event.getSpace().getContext()).getAgentType(owner), event.getSpaceObject().getType(), DESTROYED);
							if(percepttype!=null)
								((AbstractEnvironmentSpace)event.getSpace()).createPercept(percepttype, event.getSpaceObject(), owner);
						}
					}
				}
			}
		}
	}

	/**
	 *  Get the percept type.
	 *  @param space The space.
	 *  @param agenttype The agent type.
	 *  @param objecttype The object type.
	 *  @param actiontype The action type.
	 *  @return The matching percept. 
	 */
	protected String getPerceptType(IEnvironmentSpace space, String agenttype, String objecttype, String actiontype)
	{
		String ret = null;
		
//		if(agenttype.equals("Collector") && objecttype.equals("garbage") && actiontype.equals(APPEARED))
//			System.out.println("here");
		
		Object[] percepttypes = getPerceptTypes();
		for(int i=0; i<percepttypes.length; i++)
		{
			PerceptType pt = (PerceptType)space.getPerceptType(((String[])percepttypes[i])[0]);
			if((pt.getAgentTypes()==null || pt.getAgentTypes().contains(agenttype))
				&& (pt.getObjectTypes()==null || pt.getObjectTypes().contains(objecttype))
				&& (getActionTypes(pt)==null || getActionTypes(pt).contains(actiontype)))
			{
				ret = pt.getName();
			}
		}
		
//		if(ret==null)
//			System.out.println("No percept found for: "+agenttype+" "+objecttype+" "+actiontype);
		
		return ret;
	}
	
	/**
	 *  Get the action types for a percept.
	 *  @param pt The percept type.
	 */
	protected Set getActionTypes(PerceptType pt)
	{
		if(actiontypes==null)
		{
			actiontypes = new MultiCollection(new HashMap(), HashSet.class);
			Object[] percepttypes = getPerceptTypes();
			for(int i=0; i<percepttypes.length; i++)
			{
				String[] per = (String[])percepttypes[i];
				for(int j=1; j<per.length; j++)
				{
					actiontypes.put(per[0], per[j]);
				}
			}
		}
		Set ret = (Set)actiontypes.get(pt.getName());
		return ret==null? Collections.EMPTY_SET: ret;
	}
	
	/**
	 *  Get the percept types defined for this generator.
	 *  @return The percept types.
	 */
	protected Object[] getPerceptTypes()
	{
		return (Object[])getProperty(PROPERTY_PERCEPTTYPES);
	}
	
	/**
	 *  Get the range.
	 *  @return The range.
	 */
	protected IVector1 getRange(IPropertyObject avatar)
	{
		Object tmp = avatar.getProperty(getRangePropertyName());
		return tmp==null? (IVector1)getProperty(PROPERTY_MAXRANGE): 
			tmp instanceof Number? new Vector1Double(((Number)tmp).doubleValue()): (IVector1)tmp;
	}
	
	/**
	 *  Get the range property name.
	 *  @return The range property name.
	 */
	protected String getRangePropertyName()
	{
		Object tmp = getProperty(PROPERTY_RANGE);
		return tmp==null? "range": (String)tmp;
	}
	
	/**
	 * 
	 * /
	protected boolean isObjectInRange(Space2D space, ISpaceObject source, ISpaceObject target)
	{
		IVector2 pos1 = (IVector2)source.getProperty(Space2D.POSITION);
		IVector2 pos2 = (IVector2)target.getProperty(Space2D.POSITION);
		return !getRange(source).greater(space.getDistance(pos1, pos2));
	}*/
}