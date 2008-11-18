package jadex.bdi.planlib.simsupport.common.math;

import java.math.BigDecimal;

/** Implementation of a cartesian 2-vector using double components.
 */
public class Vector2Double implements IVector2, Cloneable
{
	/** Zero vector.
	 */
	public static final IVector2 ZERO = new Vector2Double(0.0);
	
	private double x_;
	private double y_;

	private static final double PI2 = 2*Math.PI;

	/** Creates a new Vector2Double with the value (0,0).
	 */
	public Vector2Double()
	{
		x_ = 0;
		y_ = 0;
	}

	/** Creates a new Vector2 with the same value as the input vector.
	 */
	public Vector2Double(IVector2 vector)
	{
		x_ = vector.getXAsDouble();
		y_ = vector.getYAsDouble();
	}

	/** Creates a new Vector2 using the scalar to assign the
	 *  value (scalar,scalar).
	 */
	public Vector2Double(double scalar)
	{
		x_ = scalar;
		y_ = scalar;
	}

	/** Creates a new Vector2 with the given value.
	 */
	public Vector2Double(double x, double y)
	{
		x_ = x;
		y_ = y;
	}
	
	public IVector2 add(double scalar)
	{
		x_ += scalar;
		y_ += scalar;
		return this;
	}

	public IVector2 add(IVector1 scalar)
	{
		x_ += scalar.getAsDouble();
		y_ += scalar.getAsDouble();
		return this;
	}

	public IVector2 add(IVector2 vector)
	{
		x_ += vector.getXAsDouble();
		y_ += vector.getYAsDouble();
		return this;
	}
	
	public IVector2 subtract(double scalar)
	{
		x_ -= scalar;
		y_ -= scalar;
		return this;
	}

	public IVector2 subtract(IVector1 scalar)
	{
		x_ -= scalar.getAsDouble();
		y_ -= scalar.getAsDouble();
		return this;
	}

	public IVector2 subtract(IVector2 vector)
	{
		x_ -= vector.getXAsDouble();
		y_ -= vector.getYAsDouble();
		return this;
	}
	
	public IVector2 mod(IVector2 modulus)
	{
		double mx = modulus.getXAsDouble();
		double my = modulus.getYAsDouble();
		x_ = (x_ + mx) % mx;
		y_ = (y_ + my) % my;
		return this;
	}
	
	public IVector2 multiply(double scalar)
	{
		x_ *= scalar;
		y_ *= scalar;
		return this;
	}

	public IVector2 multiply(IVector1 scalar)
	{
		x_ *= scalar.getAsDouble();
		y_ *= scalar.getAsDouble();
		return this;
	}

	public IVector2 multiply(IVector2 vector)
	{
		x_ *= vector.getXAsDouble();
		y_ *= vector.getYAsDouble();
		return this;
	}
	
	public IVector2 zero()
	{
		x_ = 0.0;
		y_ = 0.0;
		return this;
	}

	public IVector2 negateX()
	{
		x_ = -x_;
		return this;
	}

	public IVector2 negateY()
	{
		y_ = -y_;
		return this;
	}

	public IVector2 negate()
	{
		x_ = -x_;
		y_ = -y_;
		return this;
	}
	
	public IVector2 randomX(IVector1 lower, IVector1 upper)
	{
		double l = lower.getAsDouble();
		double u = upper.getAsDouble();
		double r = Math.random();
		r *= (u - l);
		r += l;
		x_ = r;
		return this;
	}
	
	public IVector2 randomY(IVector1 lower, IVector1 upper)
	{
		double l = lower.getAsDouble();
		double u = upper.getAsDouble();
		double r = Math.random();
		r *= (u - l);
		r += l;
		y_ = r;
		return this;
	}
	
	public IVector2 normalize()
	{
		double length = Math.sqrt((x_ * x_) + (y_ * y_));
		x_ /= length;
		y_ /= length;
		return this;
	}

	public boolean applyReflectionBoundaryX(IVector1 boundary)
	{
		if (x_ < 0.0)
		{
			x_ = -x_;
			return true;
		}
		double bound = boundary.getAsDouble();
		if (x_ > bound)
		{
			x_ = bound - (x_ - bound);
			return true;
		}

		return false;
	}

	public boolean applyReflectionBoundaryY(IVector1 boundary)
	{
		if (y_ < 0.0)
		{
			y_ = -y_;
			return true;
		}
		double bound = boundary.getAsDouble();
		if (y_ > bound)
		{
			y_ = bound - (y_ - bound);
			return true;
		}

		return false;
	}

	public IVector1 getLength()
	{
		return new Vector1Double(Math.sqrt((x_ * x_) + (y_ * y_)));
	}

	public IVector1 getDirection()
	{
		return new Vector1Double(Math.atan2(y_, x_));
	}
	
	public float getDirectionAsFloat()
	{
		return (float) Math.atan2(y_, x_);
	}

	public double getDirectionAsDouble()
	{
		return Math.atan2(y_, x_);
	}

	public IVector1 getDistance(IVector2 vector)
	{
		double dx = x_ - vector.getXAsDouble();
		double dy = y_ - vector.getYAsDouble();
		return new Vector1Double(Math.sqrt((dx * dx) + (dy * dy)));
	}

	public IVector1 getX()
	{
		return new Vector1Double(x_);
	}

	public IVector1 getY()
	{
		return new Vector1Double(y_);
	}
	
	public float getXAsFloat()
	{
		return (float) x_;
	}

	public float getYAsFloat()
	{
		return (float) y_;
	}

	public double getXAsDouble()
	{
		return x_;
	}

	public double getYAsDouble()
	{
		return y_;
	}

	public BigDecimal getXAsBigDecimal()
	{
		return new BigDecimal(x_);
	}

	public BigDecimal getYAsBigDecimal()
	{
		return new BigDecimal(y_);
	}

	public IVector2 copy()
	{
		return new Vector2Double(x_, y_);
	}

	public Object clone() throws CloneNotSupportedException
	{
		return copy();
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof IVector2)
		{
			return equals((IVector2) obj);
		}

		return false;
	}

	public boolean equals(IVector2 vector)
	{
		return (x_ == vector.getXAsDouble() && y_ == vector.getYAsDouble());
	}
}

