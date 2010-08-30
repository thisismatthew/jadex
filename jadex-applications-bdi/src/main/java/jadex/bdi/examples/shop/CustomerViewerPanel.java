package jadex.bdi.examples.shop;

import jadex.base.gui.componentviewer.IComponentViewerPanel;
import jadex.base.gui.plugin.IControlCenter;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bridge.IExternalAccess;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.Properties;

import javax.swing.JComponent;

/**
 * 
 */
public class CustomerViewerPanel implements IComponentViewerPanel
{
	/** The jcc. */
	protected IControlCenter jcc;
	
	/** The component. */
	protected IExternalAccess component;
	
	/**
	 *  Called once to initialize the panel.
	 *  Called on the swing thread.
	 *  @param jcc	The jcc.
	 * 	@param component The component.
	 */
	public IFuture init(IControlCenter jcc, IExternalAccess component)
	{
		this.jcc = jcc;
		
		// Hack!!!
//		IBDIExternalAccess agent = (IBDIExternalAccess)component;
//		agent.getExternalAccess("customercap").addResultListener(new SwingDefaultResultListener((JComponent)null)
//		{
//			public void customResultAvailable(Object source, Object result)
//			{
//				CustomerViewerPanel.this.component = (IExternalAccess)result; 
//				ret.setResult(null);
//			}
//			
//			public void customExceptionOccurred(Object source, Exception exception)
//			{
//				ret.setException(exception);
//			}
//		});
		this.component = component;
	
		return new Future(null);
	}
	
	/**
	 *  Informs the panel that it should stop all its computation
	 */
	public IFuture shutdown()
	{
		return new Future(null);
	}

	/**
	 *  The id used for mapping properties.
	 */
	public String getId()
	{
		return "customer";
	}

	/**
	 *  The component to be shown in the gui.
	 *  @return	The component to be displayed.
	 */
	public JComponent getComponent()
	{
		return new CustomerPanel((IBDIExternalAccess)component);
	}

	/**
	 *  Advices the the panel to restore its properties from the argument
	 */
	public void setProperties(Properties ps)
	{
	}

	/**
	 *  Advices the panel provide its setting as properties (if any).
	 *  This is done on project close or save.
	 */
	public Properties	getProperties()
	{
		return null;
	}

}
