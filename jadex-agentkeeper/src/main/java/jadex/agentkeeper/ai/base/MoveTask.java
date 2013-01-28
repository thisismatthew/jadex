package jadex.agentkeeper.ai.base;

import jadex.agentkeeper.ai.creatures.orc.OrcBDI;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.IFuture;
import jadex.commons.transformation.annotations.Classname;
import jadex.extension.envsupport.environment.AbstractTask;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.Space2D;
import jadex.extension.envsupport.math.IVector2;
import jadex.extension.envsupport.math.Vector1Double;
import jadex.extension.envsupport.math.Vector2Double;

import java.util.Iterator;
import java.util.Set;

/**
 *  Move an object towards a destination.
 */
public class MoveTask extends AbstractTask
{
	//-------- constants --------
	
	/** The destination property. */
	public static final String	PROPERTY_TYPENAME = "move";
	
	/** The destination property. */
	public static final String	PROPERTY_DESTINATION = "destination";

	/** The scope property. */
	public static final String	PROPERTY_SCOPE = "scope";
	
	/** The scope property. */
	public static final String	PROPERTY_AGENT = "agent";

	/** The speed property of the moving object (units per second). */
	public static final String	PROPERTY_SPEED	= "speed";
	
	
	//-------- IObjectTask methods --------
	
	/**
	 *  Executes the task.
	 *  Handles exceptions. Subclasses should implement doExecute() instead.
	 *  @param space	The environment in which the task is executing.
	 *  @param obj	The object that is executing the task.
	 *  @param progress	The time that has passed according to the environment executor.
	 */
	public void execute(IEnvironmentSpace space, ISpaceObject obj, long progress, IClockService clock)
	{
		IVector2 idis = (IVector2)getProperty(PROPERTY_DESTINATION);

		double	speed	= ((Number)getProperty(PROPERTY_SPEED)).doubleValue();
		
		double	maxdist	= progress*speed*0.0005;
		IVector2	loc	= (IVector2)obj.getProperty(Space2D.PROPERTY_POSITION);
		
		OrcBDI		capa = (OrcBDI)getProperty(PROPERTY_AGENT);
		
		double r = 0;
		double dist = ((Space2D)space).getDistance(loc, idis).getAsDouble();
		IVector2	newloc;
		boolean fin = false;
		
		Vector2Double destination = new Vector2Double(idis.getXAsDouble(), idis.getYAsDouble());
		
		if(dist>r)
		{
			
			// Todo: how to handle border conditions!?
			newloc	= (Vector2Double)(dist<=maxdist? destination 
				: destination.copy().subtract(loc).normalize().multiply(maxdist).add(loc));
			

			System.out.println("newloc: " + newloc);

			((Space2D)space).setPosition(obj.getId(), newloc);
			capa.setMyPosition(new Vector2Double(newloc.getXAsDouble(), newloc.getYAsDouble()));

		}
		else
		{
			
			fin = true;
			newloc = loc; 
		}
		
		
		if(newloc==destination || fin)
		{
			System.out.println("newloc == destination or fin!");
			setFinished(space, obj, true);
		}
			
	}
}
