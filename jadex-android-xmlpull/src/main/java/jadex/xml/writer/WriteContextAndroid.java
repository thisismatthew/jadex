package jadex.xml.writer;

import jadex.commons.collection.MultiCollection;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.xmlpull.v1.XmlSerializer;

/**
 * Android Implementation of {@link AWriteContext}.
 */
public class WriteContextAndroid extends AWriteContext<XmlSerializer>
{
	// -------- constructors --------
	/**
	 * Create a new write context.
	 */
	public WriteContextAndroid(IObjectWriterHandler handler, XmlSerializer writer, Object usercontext, Object rootobject,
			ClassLoader classloader)
	{
		this(handler, writer, usercontext, rootobject, classloader, new IdentityHashMap(), new ArrayList(), new MultiCollection());
	}

	/**
	 * Create a new write context.
	 */
	public WriteContextAndroid(IObjectWriterHandler handler, XmlSerializer writer, Object usercontext, Object rootobject,
			ClassLoader classloader, Map writtenobs, List stack, MultiCollection preprocessors)
	{
		super(handler, writer, usercontext, rootobject, classloader, writtenobs, stack, preprocessors);
	}

	// -------- methods --------

}
