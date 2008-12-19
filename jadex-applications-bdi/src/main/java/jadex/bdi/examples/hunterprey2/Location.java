package jadex.bdi.examples.hunterprey2;

import jadex.bdi.planlib.simsupport.common.math.IVector2;
import jadex.bdi.planlib.simsupport.common.math.Vector2Double;


/**
 *  Editable Java class for concept Location of hunterprey ontology.
 */
public class Location
{
	//-------- attributes ----------

	/** The x-coordinate. */
	protected int x;

	/** The y-coordinate. */
	protected int y;

	//-------- constructors --------

	/**
	 *  Create a new Location.
	 */
	public Location()
	{
		// Empty constructor required for JavaBeans (do not remove).
	}

	/**
	 *  Create a new Location.
	 */
	public Location(int x, int y)
	{
		// Constructor using required slots (change if desired).
		setX(x);
		setY(y);
	}

	//-------- accessor methods --------

	/**
	 *  Get the x of this Location.
	 *  The x-coordinate.
	 * @return x
	 */
	public int getX()
	{
		return this.x;
	}

	/**
	 *  Set the x of this Location.
	 *  The x-coordinate.
	 * @param x the value to be set
	 */
	public void setX(int x)
	{
		this.x = x;
	}

	/**
	 *  Get the y of this Location.
	 *  The y-coordinate.
	 * @return y
	 */
	public int getY()
	{
		return this.y;
	}

	/**
	 *  Set the y of this Location.
	 *  The y-coordinate.
	 * @param y the value to be set
	 */
	public void setY(int y)
	{
		this.y = y;
	}

	//-------- custom code --------

	/**
	 *  Test if two locations are equal.
	 */
	public boolean equals(Object o)
	{
		return (o instanceof Location) && ((Location)o).getX() == getX() && ((Location)o).getY() == getY();
	}

	/**
	 *  The hashcode is the 16 bit shifted x position
	 *  plus the y position.
	 *  @return The hashcode.  
	 */
	public int hashCode()
	{
		return getX() << 16 + getY();
	}
	
	
	/** 
	 * simulation-engine convenience method
	 * @return
	 */
	public IVector2 getAsIVector() {
		return new Vector2Double(this.x, this.y);
	}
	
	public String toString()
	{
		
		return "Location("+x+","+y+")";
	}
}
