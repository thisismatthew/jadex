package jadex.tools.serviceviewer.dfservice;

import jadex.base.fipa.IDF;
import jadex.base.fipa.IDFComponentDescription;
import jadex.base.fipa.IDFServiceDescription;
import jadex.commons.Properties;
import jadex.commons.SGUI;
import jadex.commons.concurrent.SwingDefaultResultListener;
import jadex.service.IService;
import jadex.tools.common.GuiProperties;
import jadex.tools.common.plugin.IControlCenter;
import jadex.tools.dfbrowser.DFBrowserPlugin_old;
import jadex.tools.serviceviewer.IServiceViewerPanel;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *  DFBrowserPlugin
 */
public class DFBrowserPanel	extends JPanel implements IServiceViewerPanel
{
	//-------- constants --------
	
	/** The image icons. */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"refresh", SGUI.makeIcon(DFBrowserPlugin_old.class, "/jadex/tools/common/images/new_refresh_anim00.png")
	});
	
	//-------- attributes --------
	
	/** The df service. */
	protected IDF df;

	/** The component table. */
	protected DFComponentTable component_table;

	/** The service table. */
	protected DFServiceTable service_table;

	/** The service panel. */
	protected ServiceDescriptionPanel service_panel;

	/** The second split pane. */
	protected JSplitPane split2;

	/** The third split pane. */
	protected JSplitPane split3;
	
	/** The old component descriptions. */
	protected IDFComponentDescription[] old_ads;

	/** The refresh timer. */
	protected Timer	timer;

	//-------- constructors --------
	
	/**
	 *  Create a service panel
	 */
	public DFBrowserPanel()
	{
		// Public noarg constructor required.
	}
	
	//-------- IServiceViewerPanel interface --------
	
	/**
	 *  Called once to initialize the panel.
	 *  Called on the swing thread.
	 *  @param jcc	The jcc.
	 * 	@param service	The service.
	 */
	public void init(IControlCenter jcc, IService service)
	{
		this.df	= (IDF)service;
		service_panel = new ServiceDescriptionPanel();
		service_table = new DFServiceTable();
		JScrollPane stscroll = new JScrollPane(service_table);
		stscroll.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Registered Services"));
		
		component_table = new DFComponentTable(this);
		JScrollPane atscroll = new JScrollPane(component_table);
		atscroll.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Registered Component Descriptions"));
		
		component_table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				//updateServices(old_ads);
				IDFComponentDescription[] selcomponents = component_table.getSelectedComponents();
				service_table.setComponentDescriptions(selcomponents);
			}
		});
		service_table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				updateDetailedService();
			}
		});
	
		setLayout(new BorderLayout());
		
		split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split3.setDividerLocation(130);
		split3.setOneTouchExpandable(true);
		split3.add(stscroll);
		split3.add(service_panel);
		split3.setResizeWeight(1.0);
		split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split2.setDividerLocation(130);
		split2.add(atscroll);
		split2.add(split3);
		split2.setResizeWeight(0.5);
		add(split2, BorderLayout.CENTER);
		
		int defrefresh	= 5000;
		timer = new Timer(defrefresh, new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				refresh();
			}
		});
		if(defrefresh>0)
			timer.start();
		int[]	refreshs	= new int[]{0, 1000, 5000, 30000};
		JPanel	settings	= new JPanel(new GridBagLayout());
		settings.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Refresh Settings"));
		GridBagConstraints	gbc	= new GridBagConstraints();
		gbc.fill	= GridBagConstraints.NONE;
		ButtonGroup group = new ButtonGroup();
		JRadioButton[]	buttons	= new JRadioButton[refreshs.length];
		for(int i=0; i<buttons.length; i++)
		{
			final int	refresh	= refreshs[i];
			JRadioButton	rb	= new JRadioButton(refresh>0 ? Integer.toString(refresh/1000)+" s" : "No refresh");
			rb.setSelected(refresh==defrefresh);
			group.add(rb);
			settings.add(rb, gbc);
			rb.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if(refresh>0)
					{
						timer.setDelay(refresh);
						timer.setInitialDelay(refresh);
						timer.restart();
					}
					else
					{
						timer.stop();
					}
				}
			});
		}
		gbc.anchor	= GridBagConstraints.EAST;
		gbc.weightx	= 1.0;	// Last button gets remaining size.
		JButton	button	= new JButton("Refresh", icons.getIcon("refresh"));
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				refresh();
			}
		});
		settings.add(button, gbc);

		add(settings, BorderLayout.NORTH);

		GuiProperties.setupHelp(this, "tools.dfbrowser");
		
		refresh();
	}
	
	/**
	 *  Informs the plugin that it should stop all its computation
	 */
	public void shutdown()
	{
		if(timer.isRunning())
			timer.stop();
	}

	
	/**
	 *  Get the component.
	 */
	public JComponent getComponent()
	{
		return this;
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

	//-------- methods --------
	
	/**
	 *  Refresh the view.
	 */
	protected void refresh()
	{
		df.search(df.createDFComponentDescription(null, null), null).addResultListener(new SwingDefaultResultListener(this)
		{
			public void customResultAvailable(Object source, Object result) 
			{
				IDFComponentDescription[] ads = (IDFComponentDescription[])result;
//				System.out.println("Found: "+SUtil.arrayToString(ads));
				
				if(old_ads == null || !Arrays.equals(old_ads, ads))
				{
					component_table.setComponentDescriptions(ads);
					updateServices(ads);
					updateDetailedService();
					old_ads = ads;
				}
			}	
		});
	}
	
	/**
	 *  Update the services panel.
	 *  @param ads The component descriptions.
	 */
	public void updateServices(IDFComponentDescription[] ads)
	{
		IDFComponentDescription[] selcomponents = component_table.getSelectedComponents();
		if(selcomponents.length==0)
			service_table.setComponentDescriptions(ads);
	}
	
	/**
	 *  Update the detail view of services.
	 */
	public void updateDetailedService()
	{
		Object[] sdescs = service_table.getSelectedServices();
		service_panel.setService((IDFComponentDescription)sdescs[1], 
			(IDFServiceDescription)sdescs[0]);
	}

	/**
	 * @param description
	 */
	protected void removeComponentRegistration(final IDFComponentDescription description)
	{
		df.deregister(description).addResultListener(new SwingDefaultResultListener(this)
		{
			public void customResultAvailable(Object source, Object result) 
			{
				refresh();
			}
		});
	}	
}
