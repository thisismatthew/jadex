package jadex.xml.writer;

import java.util.logging.Logger;

import jadex.commons.SReflect;
import jadex.xml.reader.XMLReaderFactory;

/**
 * Factory to create XML Writers.
 */
public abstract class XMLWriterFactory
{
	// -------- attributes --------

	/** The instance of this factory */
	private static XMLWriterFactory INSTANCE;

	// -------- constructors --------
	/**
	 * Constructor.
	 */
	protected XMLWriterFactory()
	{
	}

	// -------- methods --------
	/**
	 * Returns the instance of this factory.
	 * 
	 * @return the factory instance
	 */
	public static XMLWriterFactory getInstance()
	{
		if (INSTANCE == null)
		{
			if (SReflect.isAndroid())
			{
				Class<?> clz;
				try
				{
					clz = SReflect.classForName("jadex.xml.writer.XMLWriterFactoryAndroid", null);
					if (clz != null) {
						INSTANCE = (XMLWriterFactory) clz.newInstance();
					}
				} catch (ClassNotFoundException e)
				{
					e.printStackTrace();
				} catch (InstantiationException e)
				{
					e.printStackTrace();
				} catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			} else
			{
				INSTANCE = new XMLWriterFactoryDesktop();
			}
		}
		return INSTANCE;
	}

	/**
	 *  Create a new reader (with genids=true and indent=true).
	 */
	public abstract AWriter createWriter();

	/**
	 * Create an XMLWRiter
	 * @param genIds flag for generating ids.
	 * @return the writer
	 */
	public abstract AWriter createWriter(boolean genIds);
	

	/**
	 * Creates a new default XML Reader.
	 * 
	 * @param genids
	 *            flag for generating ids
	 * @param indents
	 * 
	 * @return reader
	 */
	public abstract AWriter createWriter(boolean genids, boolean indents);

}
