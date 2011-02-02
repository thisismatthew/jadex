package jadex.tools.starter;

import jadex.base.gui.componentviewer.AbstractServiceViewerPanel;
import jadex.base.gui.plugin.IControlCenter;
import jadex.bridge.IComponentManagementService;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.Properties;
import jadex.commons.concurrent.DelegationResultListener;
import jadex.commons.concurrent.IResultListener;
import jadex.commons.service.IService;

import javax.swing.JComponent;

/**
 *  Panel for the daemon view.
 */
public class StarterViewerPanel extends AbstractServiceViewerPanel
{
	//-------- attributes --------
	
	/** The panel. */
	protected StarterServicePanel panel;
	
	//-------- methods --------
	
	/**
	 *  Called once to initialize the panel.
	 *  Called on the swing thread.
	 *  @param jcc	The jcc.
	 * 	@param component The component.
	 */
	public IFuture init(final IControlCenter jcc, final IService service)
	{
		final Future ret = new Future();
		super.init(jcc, service).addResultListener(new IResultListener()
		{
			public void resultAvailable(Object result)
			{
				panel = new StarterServicePanel(jcc, (IComponentManagementService)service);
				panel.init().addResultListener(new DelegationResultListener(ret));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});
		return ret;
	}
	
	/**
	 *  Informs the panel that it should stop all its computation
	 */
	public IFuture shutdown()
	{
		return new Future(null);//panel.shutdown();
	}
	
	/**
	 *  The component to be shown in the gui.
	 *  @return	The component to be displayed.
	 */
	public JComponent getComponent()
	{
		return panel;
	}
	
	/**
	 *  Advices the the panel to restore its properties from the argument
	 */
	public void setProperties(Properties props)
	{
		panel.setProperties(props);
	}

	/**
	 *  Advices the panel provide its setting as properties (if any).
	 *  This is done on project close or save.
	 */
	public Properties	getProperties()
	{
		return panel.getProperties();
	}
}