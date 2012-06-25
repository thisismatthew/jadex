package jadex.xml.reader;

import javax.xml.stream.XMLReporter;

import jadex.commons.SReflect;

public abstract class XMLReaderFactory
{

	private static XMLReaderFactory INSTANCE;
	
	protected XMLReaderFactory(){}

	public static XMLReaderFactory getInstance()
	{
		if (INSTANCE == null) {
			if (SReflect.isAndroid())
			{
				//INSTANCE = new XMLReaderFactoryAndroid();
			} else
			{
				INSTANCE = new XMLReaderFactoryDesktop();
			}
		}
		return INSTANCE;
	}
	

	public abstract AReader createReader();
	
	public abstract AReader createReader(boolean bulklink);
	
	public abstract AReader createReader(boolean bulklink, boolean validate, XMLReporter reporter);
	
	public abstract AReader createReader(boolean bulklink, boolean validate, boolean coalescing, XMLReporter reporter);
}
