package jadex.bridge.sensor.service;

import jadex.bridge.IInternalAccess;
import jadex.bridge.nonfunctional.NFPropertyMetaInfo;
import jadex.bridge.nonfunctional.SimpleValueNFProperty;
import jadex.bridge.sensor.unit.TimeUnit;

/**
 *  Base property for time properties.
 */
public abstract class TimedProperty extends SimpleValueNFProperty<Long, TimeUnit>
{
	/**
	 *  Create a new property.
	 */
	public TimedProperty(String name, final IInternalAccess comp, long updaterate)
	{
		super(comp, new NFPropertyMetaInfo(name, long.class, TimeUnit.class, 
			updaterate>0? true: false, updaterate, null));
	}
}
