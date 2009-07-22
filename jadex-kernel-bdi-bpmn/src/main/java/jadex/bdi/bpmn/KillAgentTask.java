package jadex.bdi.bpmn;

import jadex.bpmn.runtime.IProcessInstance;
import jadex.bpmn.runtime.ITaskContext;
import jadex.bpmn.runtime.task.AbstractTask;

/**
 *  Task to kill the agent.
 */
public class KillAgentTask extends AbstractTask
{
	/**
	 *  Execute the task.
	 */
	public void doExecute(ITaskContext context, IProcessInstance instance)
	{
		((BpmnPlanBodyInstance)instance).killAgent();
	}
}
