package jadex.component;

import jadex.commons.AbstractModelLoader;
import jadex.commons.ICacheableModel;
import jadex.commons.ResourceInfo;
import jadex.component.model.MComponentType;

import java.util.Set;

/**
 *  Loader for application files.
 */
public class ComponentModelLoader extends AbstractModelLoader
{
	//-------- constants --------
	
	/** The component file extension. */
	public static final String	FILE_EXTENSION_COMPONENT = ".component.xml";
	
	//-------- attributes --------
	
	/** The xml reader. */
	protected ComponentXMLReader reader;
	
	//-------- constructors --------
	
	/**
	 *  Create a new BPMN model loader.
	 */
	public  ComponentModelLoader(Set[] mappings)
	{
		super(new String[]{FILE_EXTENSION_COMPONENT});
		this.reader = new ComponentXMLReader(mappings);
	}

	//-------- methods --------
	
	/**
	 *  Load a component model.
	 *  @param name	The filename or logical name (resolved via imports and extensions).
	 *  @param imports	The imports, if any.
	 */
	public MComponentType loadComponentModel(String name, String[] imports, ClassLoader classloader) throws Exception
	{
		return (MComponentType)loadModel(name, FILE_EXTENSION_COMPONENT, imports, classloader);
	}
	
	//-------- AbstractModelLoader methods --------
		
	/**
	 *  Load a model.
	 *  @param name	The original name (i.e. not filename).
	 *  @param info	The resource info.
	 */
	protected ICacheableModel doLoadModel(String name, String[] imports, ResourceInfo info, ClassLoader classloader) throws Exception
	{
		return (ICacheableModel)reader.read(info, classloader);
	}
}