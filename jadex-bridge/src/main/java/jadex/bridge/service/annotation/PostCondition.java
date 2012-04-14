package jadex.bridge.service.annotation;

import jadex.bridge.service.annotation.PreCondition.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostCondition
{
	public static enum Type {NOTNULL, EXPRESSION}
	
	/**
	 *  The type.
	 */
	public Type value();

	/**
	 *  The expression will be parsed.
	 */
	public String expression() default "";
}