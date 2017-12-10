package jadex.bytecode.fastinvocation;

import jadex.commons.Tuple2;

public interface IExtractor
{
	public Tuple2<String[], Object[]> extract(Object target);
}
