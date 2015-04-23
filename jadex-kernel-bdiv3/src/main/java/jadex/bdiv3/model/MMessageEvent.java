package jadex.bdiv3.model;

import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.types.message.MessageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class MMessageEvent extends MElement
{
	public static Map<String, Direction> dirs = new HashMap<String, MMessageEvent.Direction>();

	/** The message direction. */
	public enum Direction
	{
		SEND("send"),
		RECEIVE("receive"),
		SENDRECEIVE("send_receive");
		
		protected String str;
		
		/**
		 *  Create a new direction
		 */
		Direction(String str)
		{
			this.str = str;
			dirs.put(str, this);
		} 
		
		/**
		 *  Get the string representation.
		 *  @return The string representation.
		 */
		public String getString()
		{
			return str;
		}
		
		/**
		 * 
		 */
		public static Direction getDirection(String name)
		{
			return dirs.get(name);
		}
	}
	
	/** The parameters. */
	protected List<MParameter> parameters;
	
	/** The direction. */
	protected Direction direction;
	
	/** The message type. */
	protected MessageType type;
	
	/** The spec. degree. */
	protected int degree;

	/** The match expression. */
	protected UnparsedExpression matchexp;
	
	/**
	 *  Get the direction.
	 *  @return The direction
	 */
	public Direction getDirection()
	{
		return direction;
	}

	/**
	 *  The direction to set.
	 *  @param direction The direction to set
	 */
	public void setDirection(Direction direction)
	{
		this.direction = direction;
	}
	
	/**
	 *  Get the type.
	 *  @return The type
	 */
	public MessageType getType()
	{
		return type;
	}

	/**
	 *  The type to set.
	 *  @param type The type to set
	 */
	public void setType(MessageType type)
	{
		this.type = type;
	}
	
	/**
	 *  Get the parameters.
	 *  @return The parameters.
	 */
	public List<MParameter> getParameters()
	{
		return parameters;
	}
	
	/**
	 *  Get a parameter by name.
	 */
	public MParameter getParameter(String name)
	{
		MParameter ret = null;
		if(parameters!=null && name!=null)
		{
			for(MParameter param: parameters)
			{
				if(param.getName().equals(name))
				{
					ret = param;
					break;
				}
			}
		}
		return ret;
	}
	
	/**
	 *  Test if goal has a parameter.
	 */
	public boolean hasParameter(String name)
	{
		return getParameter(name)!=null;
	}

	/**
	 *  Set the parameters.
	 *  @param parameters The parameters to set.
	 */
	public void setParameters(List<MParameter> parameters)
	{
		this.parameters = parameters;
	}
	
	/**
	 *  Add a parameter.
	 *  @param parameter The parameter.
	 */
	public void addParameter(MParameter parameter)
	{
		if(parameters==null)
			parameters = new ArrayList<MParameter>();
		this.parameters.add(parameter);
	}
	
	/**
	 *  Get the matchExpression.
	 *  @return The matchExpression
	 */
	public UnparsedExpression getMatchExpression()
	{
		return matchexp;
	}

	/**
	 *  The match expression to set.
	 *  @param matchexp The matchExpression to set
	 */
	public void setMatchExpression(UnparsedExpression matchexp)
	{
		this.matchexp = matchexp;
	}

	/**
	 *  Get the specialization degree.
	 *  @return The degree.
	 */
	public int getSpecializationDegree()
	{
		if(degree==-1)
		{
			// Calculate specificity by summing up fixed parameters and parameter sets.
			if(parameters!=null)
			{
				for(MParameter param: parameters)
				{
					if(param.getDirection().equals(jadex.bdiv3.model.MParameter.Direction.FIXED)
						&& param.getValue()!=null)
//						&& (parametersets[i].getDefaultValuesExpression()!=null || parametersets[i].getDefaultValues().length>0))
					{
						degree++;
					}
				}
			}
			
			// Messages with match expression have higher degree.
			if(getMatchExpression()!=null)
				degree++;
			
			if(degree==-1)
				degree = 0;
		}
		return degree;
	}
}