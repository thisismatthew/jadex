/**
 * 
 */
package jadex.editor.bpmn.runtime.task;

import jadex.editor.bpmn.editor.JadexBpmnEditor;
import jadex.editor.bpmn.editor.JadexBpmnEditorActivator;
import jadex.editor.bpmn.editor.preferences.JadexPreferencesPage;
import jadex.editor.bpmn.editor.preferences.JadexTaskProviderTypeListEditor;

import java.io.InvalidObjectException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.UniqueEList;

/**
 * This TaskProvider reads the Jadex BPMN preference list for TaskProvider,
 * collects all provided tasks and returns them in a single list.
 * 
 * The getTaskMetaInfoFor(className) method is a proxy to the original 
 * TaskProvider method.
 * 
 * @author Claas
 * 
 */
public class PreferenceTaskProviderProxy implements IEditorTaskProvider
{
	/** The list of ITaskProvider */
	private static List<String> iTaskProviderCache;

	/** Map for IRuntimeProvider access. className -> provider */
	private SortedMap<String, Object> providerMap;

	/**
	 * Default constructor
	 */
	public PreferenceTaskProviderProxy()
	{
		super();

		iTaskProviderCache = new UniqueEList<String>();
		providerMap = new TreeMap<String, Object>();

		// initialize map and cache
		getAvailableTaskImplementations();
	}

	/* (non-Javadoc)
	 * @see jadex.tools.bpmn.runtime.task.IEditorTaskProvider#dispose()
	 */
	@Override
	public void dispose()
	{
		// TODO Auto-generated method stub
		
	}

	
	/* (non-Javadoc)
	 * @see jadex.tools.bpmn.runtime.task.IEditorTaskProvider#refresh()
	 */
	@Override
	public void refresh()
	{
		iTaskProviderCache.clear();
		providerMap.clear();
		WorkspaceClassLoaderHelper.getWorkspaceClassLoader(true);
		getAvailableTaskImplementations();
	}

	/**
	 * Returns a compound array of all Tasks defined by the TaskProviders
	 * in from the Jadex preference store list.  
	 * 
	 * @see jadex.editor.bpmn.runtime.task.IEditorTaskProvider#
	 * getAvailableTaskImplementations()
	 */
	@Override
	public String[] getAvailableTaskImplementations()
	{

		List<String> preferenceList = JadexTaskProviderTypeListEditor.parseStringList(JadexBpmnEditorActivator.getDefault().getPreferenceStore().getString(JadexPreferencesPage.PREFERENCE_TASKPROVIDER_STRINGLIST));
		
		if (!iTaskProviderCache.equals(preferenceList))
		{
			loadTaskMetaInfos(preferenceList);
		}
		
		return providerMap.keySet().toArray(new String[providerMap.size()]);

	}
	
	/**
	 * Loads the provider and provided TaskMetaInfos into the provider map
	 * 
	 * @param iTaskProviderList to load
	 */
	protected void loadTaskMetaInfos(List<String> iTaskProviderList)
	{
		// replace cache and clear map
		if (iTaskProviderList == null)
		{
			iTaskProviderCache.clear();
		}
		else
		{
			iTaskProviderCache = iTaskProviderList;
		}
		providerMap.clear();
		
		ClassLoader classLoader = WorkspaceClassLoaderHelper
				.getWorkspaceClassLoader(false);

		if (classLoader == null)
		{
			return;
		}

		for (String className : iTaskProviderList)
		{
			try
			{
				
				Class<?> clazz = classLoader.loadClass(className);
				Object instance = clazz.newInstance();
				String[] tasks;
				IEditorTaskProvider provider;
				
				if (instance instanceof IEditorTaskProvider)
				{
					provider = (IEditorTaskProvider) instance;
					tasks = provider.getAvailableTaskImplementations();
				} 
				else
				{
					// use reflection proxy
					provider = new TaskProviderProxy(instance);
					tasks = provider.getAvailableTaskImplementations();
				}
				for (int i = 0; i < tasks.length; i++)
				{
					providerMap.put(tasks[i], provider);
				}
				
			}
			catch (Exception e)
			{
				JadexBpmnEditor.log("Problem during TaskMetaInfo load in "+this.getClass().getSimpleName(), e, IStatus.ERROR);
			}
			
		}
	}

	/**
	 * Proxy method to the original TaskProvider method
	 * 
	 * @see
	 * jadex.editor.bpmn.runtime.task.IEditorTaskProvider#getTaskMetaInfo
	 * (java.lang.String)
	 */
	@Override
	public IEditorTaskMetaInfo getTaskMetaInfo(String fqClassName)
	{

		Object obj = providerMap.get(fqClassName);
		if (obj != null && obj instanceof IEditorTaskProvider)
		{
			// use interface if implemented
			IEditorTaskProvider provider = (IEditorTaskProvider) obj;
			return provider.getTaskMetaInfo(fqClassName);
		}
		else if (obj != null)
		{
			throw new RuntimeException("Unexpected class in "+this, new InvalidObjectException("Object doesn't implement the IEditorTaskProvider interface nor its a Proxy object!" + obj));
		}
		
		// fall through
		return TaskProviderSupport.NO_TASK_META_INFO_PROVIDED;

	}
	
	public static IStatus checkTaskProviderClass(String fullQualifiedClassName)
	{
		IStatus status = null;
		
		if (iTaskProviderCache.contains(fullQualifiedClassName))
		{
			status = new Status(IStatus.OK, JadexBpmnEditorActivator.ID, "Already in list");;
		}
		
		try
		{
			// first check implementing marker interface
			ClassLoader classLoader = WorkspaceClassLoaderHelper
					.getWorkspaceClassLoader(false);
			Class<?> clazz = classLoader.loadClass(fullQualifiedClassName);
			if (Arrays.asList(clazz.getInterfaces()).contains(
					IEditorTaskProvider.class))
			{
				status = new Status(IStatus.OK, JadexBpmnEditorActivator.ID, "Implements IRuntimeTaskProvider");
				return status;
			}
			
			// second check implementing needed methods
			Method getAvailableTasksMethod = clazz
					.getMethod(IEditorTaskProvider.METHOD_IJADEXTASKPROVIDER_GET_AVAILABLE_TASK_IMPLEMENTATIONS);
			Method getTaskMetaInfoMethod = clazz
					.getMethod(IEditorTaskProvider.METHOD_IJADEXTASKPROVIDER_GET_TASK_META_INFO);
			
			if (getAvailableTasksMethod != null && getTaskMetaInfoMethod != null)
			{
				status = new Status(IStatus.OK, JadexBpmnEditorActivator.ID, "Implements '"+IEditorTaskProvider.METHOD_IJADEXTASKPROVIDER_GET_AVAILABLE_TASK_IMPLEMENTATIONS+"' and '"+IEditorTaskProvider.METHOD_IJADEXTASKPROVIDER_GET_TASK_META_INFO+"'");
				return status;
			}
		}
		// if something goes wrong, report it
		catch (Exception e)
		{
			JadexBpmnEditor.log("Exception during task provider check in "+PreferenceTaskProviderProxy.class.getSimpleName(), e, IStatus.ERROR);
			status = new Status(
					IStatus.ERROR,
					JadexBpmnEditorActivator.ID,
					IStatus.CANCEL,
					"Exception checking '"
							+ fullQualifiedClassName
							+ "'. The selected class does not implement the IRuntimeTaskProvider interface nor the specified methods",
					e);
		}
		
		return status;
	}

}
