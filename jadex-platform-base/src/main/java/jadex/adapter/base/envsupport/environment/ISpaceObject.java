package jadex.adapter.base.envsupport.environment;

import java.util.Map;

/**
 *  Interface for a space object.
 */
public interface ISpaceObject
{
	//-------- constants --------
	
	/** The constant for the object id property. */
	public static final String OBJECT_ID = "object_id";
	
	/** The constant for the actor id property. */
	public static final String ACTOR_ID  = "actor_id";
	
	//-------- methods --------

	/**
	 *  Get the objects id.
	 *  @return The object id.
	 */
	public Object getId();
	
	/**
	 *  Returns the type of the object.
	 *  @return the type
	 */
	public Object getType();
	
	/**
	 *  Returns an object's property.
	 *  @param name The name of the property.
	 *  @return The property.
	 */
	public Object getProperty(String name);

	/**
	 *  Sets an object's property.
	 *  @param name name of the property
	 *  @param value the property
	 */
	public void setProperty(String name, Object value);

	/**
	 *  Returns a copy of all of the object's properties.
	 *  @return the properties
	 */
	public Map getProperties();

	/**
	 *  Adds a new task for the object.
	 *  @param task new task
	 */
	public void addTask(IObjectTask task);

	/**
	 *  Returns a task by its id.
	 *  @param id The id of the task.
	 *  @return The task.
	 */
	public IObjectTask getTask(Object id);

	/**
	 *  Removes a task from the object.
	 *  @param id The id of the task
	 */
	public void removeTask(Object id);
	
	/**
	 *  Removes all tasks from the object.
	 */
	public void clearTasks();
	
	/**
	 *  Updates the object to the current time.
	 *  @param time the current time
	 *  @param deltaT the time difference that has passed
	 */
	// Internal method (not for user)
//	public void updateObject(long time, IVector1 deltaT);
	
	/**
	 *  Fires an ObjectEvent.
	 *  @param event the ObjectEvent.
	 */
	public void fireObjectEvent(ObjectEvent event);
}
