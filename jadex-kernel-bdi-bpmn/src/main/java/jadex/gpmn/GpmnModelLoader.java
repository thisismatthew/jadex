package jadex.gpmn;

import jadex.bpmn.model.MBpmnModel;
import jadex.commons.AbstractModelLoader;
import jadex.commons.ICacheableModel;
import jadex.commons.ResourceInfo;

/**
 *  Loader for eclipse STP GPMN models (.gpmn files).
 */
public class GpmnModelLoader extends AbstractModelLoader
{
	//-------- constants --------
	
	/** The GPMN file extension. */
	public static final String	FILE_EXTENSION_GPMN	= ".gpmn";
	
	//-------- constructors --------
	
	/**
	 *  Create a new GPMN model loader.
	 */
	public GpmnModelLoader()
	{
		super(new String[]{FILE_EXTENSION_GPMN});
	}

	//-------- methods --------
	
	/**
	 *  Load a BPMN model.
	 *  @param name	The filename or logical name (resolved via imports and extensions).
	 *  @param imports	The imports, if any.
	 */
	public MBpmnModel	loadBpmnModel(String name, String[] imports) throws Exception
	{
		return (MBpmnModel)loadModel(name, FILE_EXTENSION_GPMN, imports);
	}
	
	//-------- AbstractModelLoader methods --------
		
	/**
	 *  Load a model.
	 *  @param name	The original name (i.e. not filename).
	 *  @param info	The resource info.
	 */
	protected ICacheableModel doLoadModel(String name, ResourceInfo info) throws Exception
	{
		return (ICacheableModel)GpmnXMLReader.read(info, classloader);
	}
}
