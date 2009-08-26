package jadex.wfms.service;

import java.util.Collection;
import java.util.Set;

import jadex.wfms.IProcessModel;
import jadex.wfms.client.IClient;


import jadex.gpmn.model.MGpmnModel;
/**
 * Repository service for accessing process models.
 */
public interface IModelRepositoryService
{
	/**
	 * Gets a BPMN model.
	 * @param name name of the model
	 * @return the model
	 * /
	public MBpmnModel getBpmnModel(String name);*/
	
	/**
	 * Gets all available BPMN models.
	 * @return names of all BPMN models
	 * /
	public Set getBpmnModelNames();*/
	
	/**
	 * Gets a GPMN model.
	 * @param name name of the model
	 * @return the model
	 * /
	public String getGpmnModel(String name);*/
	
	/**
	 * Gets a GPMN model path.
	 * @param name name of the model
	 * @return path to the model
	 */
//	public String getGpmnModelPath(String name);
	
	/**
	 * Gets all available GPMN models.
	 * @return names of all GPMN models
	 * /
	public Set getGpmnModelNames();*/

	/**
	 *  Add a process model.
	 *  @param client The client.
	 *  @param name The name of the model.
	 *  @param path The path to the model.
	 */
	public void addProcessModel(String name, String path);
	
	/**
	 * Gets all available models.
	 * @return names of all models
	 */
	public Collection getModelNames();
	
	/**
	 *  Remove a process model.
	 *  @param client The client.
	 *  @param name The name of the model.
	 *  @param path The path to the model.
	 */
//	public void removeProcessModel(IClient client, String name);
	
	/**
	 *  Get a process model of a specific name.
	 *  @param name The model name.
	 *  @return The process model.
	 */
	public String getProcessModelPath(String name);
	
	/**
	 *  Get the imports.
	 */
	public String[] getImports();
}
