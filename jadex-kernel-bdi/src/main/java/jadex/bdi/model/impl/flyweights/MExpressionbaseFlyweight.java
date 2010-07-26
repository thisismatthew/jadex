package jadex.bdi.model.impl.flyweights;

import jadex.bdi.model.IMExpression;
import jadex.bdi.model.IMExpressionReference;
import jadex.bdi.model.IMExpressionbase;
import jadex.bdi.model.OAVBDIMetaModel;
import jadex.rules.state.IOAVState;

import java.util.Collection;
import java.util.Iterator;

/**
 *  Flyweight for expression base model.
 */
public class MExpressionbaseFlyweight  extends MElementFlyweight implements IMExpressionbase
{
	//-------- constructors --------
	
	/**
	 *  Create a new Expressionbase flyweight.
	 */
	public MExpressionbaseFlyweight(IOAVState state, Object scope)
	{
		super(state, scope, scope);
	}
	
	//-------- methods concerning Expressions --------

	
	/**
	 *  Get a expression for a name.
	 *  @param name	The expression name.
	 */
	public IMExpression getExpression(final String name)
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressions, name);
					if(handle==null)
						throw new RuntimeException("Expression not found: "+name);
					object = new MExpressionFlyweight(getState(), getScope(), handle);
				}
			};
			return (IMExpression)invoc.object;
		}
		else
		{
			Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressions, name);
			if(handle==null)
				throw new RuntimeException("Expression not found: "+name);
			return new MExpressionFlyweight(getState(), getScope(), handle);
		}
	}

	/**
	 *  Returns all expressions.
	 *  @return All expressions.
	 */
	public IMExpression[] getExpressions()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection elems = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressions);
					IMExpression[] ret = new IMExpression[elems==null? 0: elems.size()];
					if(elems!=null)
					{
						int i=0;
						for(Iterator it=elems.iterator(); it.hasNext(); )
						{
							ret[i++] = new MExpressionFlyweight(getState(), getScope(), it.next());
						}
					}
					object = ret;
				}
			};
			return (IMExpression[])invoc.object;
		}
		else
		{
			Collection elems = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressions);
			IMExpression[] ret = new IMExpression[elems==null? 0: elems.size()];
			if(elems!=null)
			{
				int i=0;
				for(Iterator it=elems.iterator(); it.hasNext(); )
				{
					ret[i++] = new MExpressionFlyweight(getState(), getScope(), it.next());
				}
			}
			return ret;
		}
	}
	
	/**
	 *  Get a expression reference for a name.
	 *  @param name	The expression name.
	 */
	public IMExpressionReference getExpressionReference(final String name)
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressionrefs, name);
					if(handle==null)
						throw new RuntimeException("Expression reference not found: "+name);
					object = new MExpressionReferenceFlyweight(getState(), getScope(), handle);
				}
			};
			return (IMExpressionReference)invoc.object;
		}
		else
		{
			Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressionrefs, name);
			if(handle==null)
				throw new RuntimeException("Expression reference not found: "+name);
			return new MExpressionReferenceFlyweight(getState(), getScope(), handle);
		}
	}

	/**
	 *  Returns all expression references.
	 *  @return All expression references.
	 */
	public IMExpressionReference[] getExpressionReferences()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection elems = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressionrefs);
					IMExpressionReference[] ret = new IMExpressionReference[elems==null? 0: elems.size()];
					if(elems!=null)
					{
						int i=0;
						for(Iterator it=elems.iterator(); it.hasNext(); )
						{
							ret[i++] = new MExpressionReferenceFlyweight(getState(), getScope(), it.next());
						}
					}
					object = ret;
				}
			};
			return (IMExpressionReference[])invoc.object;
		}
		else
		{
			Collection elems = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_expressionrefs);
			IMExpressionReference[] ret = new IMExpressionReference[elems==null? 0: elems.size()];
			if(elems!=null)
			{
				int i=0;
				for(Iterator it=elems.iterator(); it.hasNext(); )
				{
					ret[i++] = new MExpressionReferenceFlyweight(getState(), getScope(), it.next());
				}
			}
			return ret;
		}
	}
}
