package jadex.adapter.base.envsupport.observer.graphics.drawable;


import jadex.javaparser.IParsedExpression;

import java.awt.Color;


public abstract class ColoredPrimitive extends RotatingPrimitive
{
	/** Color of the primitive. */
	protected Color		c_;

	/** OpenGL color cache. */
	protected float[]	oglColor_;
	
	/**
	 * Initializes the drawable.
	 */
	protected ColoredPrimitive()
	{
		super();
		setColor(Color.WHITE);
	}

	/**
	 * Initializes the drawable.
	 * 
	 * @param position position or position-binding
	 * @param xrotation xrotation or rotation-binding
	 * @param yrotation yrotation or rotation-binding
	 * @param zrotation zrotation or rotation-binding
	 * @param size size or size-binding
	 * @param c the drawable's color
	 */
	protected ColoredPrimitive(Object position, Object xrotation, Object yrotation, Object zrotation, Object size, Color c, IParsedExpression drawcondition)
	{
		super(position, xrotation, yrotation, zrotation, size, drawcondition);
		if (c == null)
			c = Color.WHITE;
		setColor(c);
	}

	/**
	 * Gets the color of the drawable
	 * 
	 * @return color of the drawable
	 */
	public Color getColor()
	{
		return c_;
	}

	/**
	 * Sets a new color for the drawable
	 * 
	 * @param c new color
	 */
	public void setColor(Color c)
	{
		c_ = c;
		oglColor_ = new float[4];
		oglColor_[0] = c_.getRed() / 255.0f;
		oglColor_[1] = c_.getGreen() / 255.0f;
		oglColor_[2] = c_.getBlue() / 255.0f;
		oglColor_[3] = c_.getAlpha() / 255.0f;
	}
}
