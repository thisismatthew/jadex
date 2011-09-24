package jadex.base.gui.modeltree;

import jadex.base.gui.SwingDefaultResultListener;
import jadex.base.gui.asynctree.ITreeNode;
import jadex.base.gui.filetree.FileTreePanel;
import jadex.commons.SUtil;
import jadex.commons.gui.SGUI;
import jadex.commons.gui.ToolTipAction;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIDefaults;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;

/**
 *  Collapse all paths.
 */
public class CollapseAllAction extends ToolTipAction
{
	//-------- constants --------
	
	/** The image icons. */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"collapseall",	SGUI.makeIcon(ModelTreePanel.class, "/jadex/base/gui/images/collapse.png"),
	});
	
	//-------- attributes --------
	
	/** The tree. */
	protected FileTreePanel treepanel;
	
	/** The file chooser. */
	protected JFileChooser filechooser;
	
	//-------- constructors --------
	
	/**
	 *  Create a new action
	 */
	public CollapseAllAction(FileTreePanel treepanel)
	{
		this(getName(), getIcon(), getTooltipText(), treepanel);
	}
	
	/**
	 *  Create a new action 
	 */
	public CollapseAllAction(String name, Icon icon, String desc, FileTreePanel treepanel)
	{
		super(name, icon, desc);
		this.treepanel = treepanel;
		
		filechooser = new JFileChooser(".");
		filechooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		filechooser.addChoosableFileFilter(new FileFilter()
		{
			public String getDescription()
			{
				return "Paths or .jar files";
			}

			public boolean accept(File f)
			{
				String name = f.getName().toLowerCase();
				return f.isDirectory() || name.endsWith(".jar");
			}
		});
	}
	
	//-------- methods --------
	
	/**
	 *  Test if action is available in current context.
	 *  @return True, if available.
	 */
	public boolean isEnabled()
	{
		ITreeNode rm = (ITreeNode)treepanel.getTree().getLastSelectedPathComponent();
		return rm==null && !treepanel.isRemote();
	}
	
	/**
	 *  Action performed.
	 */
	public void actionPerformed(ActionEvent e)
	{
		final ITreeNode root = (ITreeNode)treepanel.getTree().getModel().getRoot();
		root.getChildren().addResultListener(new SwingDefaultResultListener()
		{
			public void customResultAvailable(Object result)
			{
				if(result!=null)
				{
					List childs = (List)result;
					for(int i=0; i<childs.size(); i++)
					{
						ITreeNode child = (ITreeNode)childs.get(i);
						TreePath tp = new TreePath(new Object[]{root, child});
						treepanel.getTree().collapsePath(tp);
					}
				}
			}
		});
	}		
	
	/**
	 *  Get the icon.
	 *  @return The icon.
	 */
	public static Icon getIcon()
	{
		return icons.getIcon("collapseall");
	}
	
	/**
	 *  Get the name.
	 *  @return The name.
	 */
	public static String getName()
	{
		return "Collapse all.";
	}
	
	/**
	 *  Get the tooltip text.
	 *  @return The tooltip text.
	 */
	public static String getTooltipText()
	{
		return "Collapse all opended path.";
	}
}
