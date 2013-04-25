package jadex.bpmn.editor.gui;

import jadex.bpmn.editor.BpmnEditor;
import jadex.bpmn.editor.model.visual.BpmnVisualModelWriter;
import jadex.bpmn.model.io.SBpmnModelWriter;
import jadex.commons.SUtil;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.xmlgraphics.java2d.GraphicContext;
import org.apache.xmlgraphics.java2d.ps.EPSDocumentGraphics2D;

import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxStylesheet;

public class BpmnMenuBar extends JMenuBar
{
	/** The editor window. */
	protected BpmnEditorWindow editorwindow;
	
	/**
	 *  Creates the menu bar.
	 *  
	 *  @param editwindow The editor window.
	 */
	public BpmnMenuBar(BpmnEditorWindow editwindow)
	{
		
		this.editorwindow = editwindow;
		
		JMenu filemenu = new JMenu("File");
		
		filemenu.add(createNewMenuItem());
		filemenu.add(createOpenMenuItem());
		filemenu.addSeparator();
		filemenu.add(createSaveMenuItem());
		filemenu.add(createSaveAsMenuItem());
		filemenu.add(createExportMenuItem());
		filemenu.addSeparator();
		
		JMenuItem item = new JMenuItem(new AbstractAction("Save Settings")
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					editorwindow.getSettings().save();
				}
				catch (IOException e1)
				{
					Logger.getLogger(BpmnEditor.APP_NAME).log(Level.SEVERE, e1.toString());
				}
			}
		});
		filemenu.add(item);
		
		JCheckBoxMenuItem saveonexititem = new JCheckBoxMenuItem(new AbstractAction("Save Settings on Exit")
		{
			public void actionPerformed(ActionEvent e)
			{
				JCheckBoxMenuItem saveonexititem = (JCheckBoxMenuItem) e.getSource();
				editorwindow.getSettings().setSaveSettingsOnExit(saveonexititem.getState());
			}
		});
		saveonexititem.setState(editorwindow.getSettings().isSaveSettingsOnExit());
		filemenu.add(saveonexititem);
		
		filemenu.addSeparator();
		filemenu.add(createExitMenuItem());
		add(filemenu);
		
		
		JMenu viewmenu = new JMenu("View");
		add(viewmenu);
		
		/* Styles */
		JMenu stylemenu = new JMenu("Styles");
		final ButtonGroup stylegroup = new ButtonGroup();
		Action styleaction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				List<ModelContainer> containers = editorwindow.getModelContainers();
				for (ModelContainer modelcontainer : containers)
				{
					if (modelcontainer != null)
					{
						JRadioButtonMenuItem button = (JRadioButtonMenuItem) e.getSource();
						mxStylesheet sheet = (mxStylesheet) button.getClientProperty("sheet");
						modelcontainer.getGraph().setStylesheet(sheet);
						modelcontainer.getGraph().refresh();
						editorwindow.getSettings().setSelectedSheet(button.getText());
					}
				}
			}
		};
		
		for (int i = 0; i < BpmnEditor.STYLE_SHEETS.length; ++i)
		{
			JRadioButtonMenuItem view = new JRadioButtonMenuItem(styleaction);
			view.putClientProperty("sheet", BpmnEditor.STYLE_SHEETS[i].getSecondEntity());
			
			if (BpmnEditor.STYLE_SHEETS[i].getFirstEntity().equals(editwindow.getSettings().getSelectedSheet()))
			{
				view.setSelected(true);
			}
			
			view.setText(BpmnEditor.STYLE_SHEETS[i].getFirstEntity());
			stylegroup.add(view);
			stylemenu.add(view);
			
		}
		
		viewmenu.add(stylemenu);
		
		editorwindow.addTabListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				ModelContainer modelcontainer = editorwindow.getSelectedModelContainer();
				if (modelcontainer != null)
				{
					mxStylesheet sheet = modelcontainer.getGraph().getStylesheet();
					Enumeration<AbstractButton> buttons = stylegroup.getElements();
					while (buttons.hasMoreElements())
					{
						AbstractButton button = buttons.nextElement();
						mxStylesheet bsheet = (mxStylesheet) button.getClientProperty("sheet");
						if (bsheet.equals(sheet))
						{
							button.setSelected(true);
							return;
						}
					}
				}
			}
		});
		
		/** Icon sizes */
		JMenu iconmenu = new JMenu("Icon Size");
		ButtonGroup icongroup = new ButtonGroup();
		Action iconaction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				ModelContainer modelcontainer = editorwindow.getSelectedModelContainer();
				if (modelcontainer != null)
				{
					JRadioButtonMenuItem button = (JRadioButtonMenuItem) e.getSource();
					int iconsize = (Integer) button.getClientProperty("size");
					((BpmnToolbar) modelcontainer.getEditingToolbar()).setIconSize(iconsize);
					editorwindow.getSettings().setToolbarIconSize(iconsize);
				}
			}
		};
		
		for (int i = 0; i < GuiConstants.ICON_SIZES.length; ++i)
		{
			JRadioButtonMenuItem isbutton = new JRadioButtonMenuItem(iconaction);
			isbutton.putClientProperty("size", GuiConstants.ICON_SIZES[i]);
			if (GuiConstants.ICON_SIZES[i] == editorwindow.getSettings().getToolbarIconSize())
			{
				isbutton.setSelected(true);
			}
			isbutton.setText("" + GuiConstants.ICON_SIZES[i] + "x" + GuiConstants.ICON_SIZES[i]);
			icongroup.add(isbutton);
			iconmenu.add(isbutton);
		}
		viewmenu.add(iconmenu);
		
		JMenu helpmenu = new JMenu("Help");
		add(helpmenu);
		
		JMenuItem aboutitem = new JMenuItem(new AbstractAction("About " + BpmnEditor.APP_NAME)
		{
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(getParent(),
					    BpmnEditor.APP_NAME + " Build " + BpmnEditor.BUILD + "\nModel Build " + getModelBuild(),
					    BpmnEditor.APP_NAME,
					    JOptionPane.INFORMATION_MESSAGE);
			}
		});
		helpmenu.add(aboutitem);
	}
	
	/**
	 *  Creates the "New" menu item.
	 *  
	 *  @return The menu item.
	 */
	protected JMenuItem createNewMenuItem()
	{
		JMenuItem newitem = new JMenuItem(new AbstractAction("New")
		{
			public void actionPerformed(ActionEvent e)
			{
				editorwindow.initializeNewModel(editorwindow.newModelTab(null));
			}
		});
		
		newitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK));
		
		return newitem;
	}
	
	/**
	 *  Creates the "Open" menu item.
	 *  
	 *  @return The menu item.
	 */
	protected JMenuItem createOpenMenuItem()
	{
		JMenuItem openitem = new JMenuItem(new AbstractAction("Open...")
		{
			public void actionPerformed(ActionEvent e)
			{
				File curfile = null;
				ModelContainer curcont = editorwindow.getSelectedModelContainer();
				if (curcont != null)
				{
					curfile = curcont.getFile();
				}
				if (curfile == null)
				{
					curfile = editorwindow.getSettings().getLastFile();
				}
				
				BetterFileChooser fc = new BetterFileChooser(curfile);
				FileFilter filter = new FileNameExtensionFilter("BPMN model file", "bpmn2");
				fc.addChoosableFileFilter(filter);
				fc.setFileFilter(filter);
				filter = new FileNameExtensionFilter("Legacy BPMN model file", "bpmn");
				//fc.addChoosableFileFilter(filter);
				int result = fc.showOpenDialog(getParent());
				if (JFileChooser.APPROVE_OPTION == result)
				{
					try
					{
						File file = fc.getSelectedFile();
						editorwindow.loadModel(file);
					}
					catch (Exception e1)
					{
						displayIOError(e1);
					}
				}
			}
		});
		
		openitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
		
		return openitem;
	}
	
	/**
	 *  Creates the "Save" menu item.
	 *  
	 *  @return The menu item.
	 */
	protected JMenuItem createSaveMenuItem()
	{
		JMenuItem saveitem = new JMenuItem(new AbstractAction("Save")
		{
			public void actionPerformed(ActionEvent e)
			{
				ModelContainer modelcontainer = editorwindow.getSelectedModelContainer();
				if (modelcontainer != null)
				{
					if (modelcontainer.getFile() != null && modelcontainer.getFile().getName().endsWith(".bpmn2"))
					{
						try
						{
							SBpmnModelWriter.writeModel(modelcontainer.getFile(), modelcontainer.getBpmnModel(), new BpmnVisualModelWriter(modelcontainer.getGraph()));
							editorwindow.getSettings().setLastFile(modelcontainer.getFile());
							modelcontainer.setDirty(false);
						}
						catch (IOException e1)
						{
							displayIOError(e1);
						}
					}
					else
					{
						saveWithDialog();
					}
				}
			}
		});
		
		saveitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
		
		return saveitem;
	}
	
	/**
	 *  Creates the "Save as" menu item.
	 *  
	 *  @return The menu item.
	 */
	protected JMenuItem createSaveAsMenuItem()
	{
		JMenuItem saveasitem = new JMenuItem(new AbstractAction("Save As...")
		{
			public void actionPerformed(ActionEvent e)
			{
				saveWithDialog();
			}
		});
		
		return saveasitem;
	}
	
	/**
	 *  Creates the "Export" menu item.
	 *  
	 *  @return The menu item.
	 */
	protected JMenuItem createExportMenuItem()
	{
		JMenuItem exportitem = new JMenuItem(new AbstractAction("Export...")
		{
			public void actionPerformed(ActionEvent e)
			{
				ModelContainer modelcontainer = editorwindow.getSelectedModelContainer();
				if (modelcontainer != null)
				{
					BetterFileChooser fc = new BetterFileChooser(modelcontainer.getFile());
					FileNameExtensionFilter filter = new FileNameExtensionFilter("EPS file", "eps");
					fc.addChoosableFileFilter(filter);
					fc.setFileFilter(filter);
					int result = fc.showSaveDialog(modelcontainer.getGraphComponent());
					if (JFileChooser.APPROVE_OPTION == result)
					{
						try
						{
							EPSDocumentGraphics2D g2d = new EPSDocumentGraphics2D(false);
							g2d.setGraphicContext(new GraphicContext());
							
							BpmnGraph graph = modelcontainer.getGraph();
							
							int x = Integer.MAX_VALUE;
							int y = Integer.MAX_VALUE;
							int w = 0;
							int h = 0;
							
							for (int i = 0; i < graph.getModel().getChildCount(graph.getDefaultParent()); ++i)
							{
								mxICell cell = (mxICell) graph.getModel().getChildAt(graph.getDefaultParent(), i);
								mxRectangle geo = graph.getCellBounds(cell);
								//mxGeometry geo = cell.getGeometry();
								if (geo.getX() < x)
								{
									x = (int) geo.getX();
								}
								if (geo.getY() < y)
								{
									y = (int) geo.getY();
								}
								if (geo.getX() + geo.getWidth() - x > w)
								{
									w = (int) Math.ceil(geo.getX() + geo.getWidth() - x);
								}
								if (geo.getY() + geo.getHeight() - y > h)
								{
									h = (int) Math.ceil(geo.getY() + geo.getHeight() - y);
								}
							}
							
							// Avoid cutting off shadows.
							w += 4;
							h += 4;
							
							File tmpfile = File.createTempFile("export", ".eps");
							//modelcontainer.getGraphComponent().paint(g)
							FileOutputStream fos = new FileOutputStream(tmpfile);
							g2d.setupDocument(fos, w, h);
							g2d.setClip(0, 0, w, h);
							g2d.translate(-x, -y);
							Object[] selcells = modelcontainer.getGraph().getSelectionModel().getCells();
							modelcontainer.getGraph().getSelectionModel().removeCells(selcells);
				        	
							modelcontainer.getGraphComponent().getGraphControl().paint(g2d);
				        	modelcontainer.getGraph().setSelectionCells(selcells);
				        	g2d.finish();
				        	fos.close();
				        	
				        	File file = fc.getSelectedFile();
				        	String ext = "." + filter.getExtensions()[0];
				        	if (!file.getName().endsWith(ext))
							{
								file = new File(file.getAbsolutePath() + ext);
							}
				        	
				        	SUtil.moveFile(tmpfile, file);
						}
						catch (IOException e1)
						{
							displayIOError(e1);
						}
					}
				}
			}
		});
		
		exportitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK));
		
		return exportitem;
	}
	
	/**
	 *  Creates the "Exit" menu item.
	 *  
	 *  @return The menu item.
	 */
	protected JMenuItem createExitMenuItem()
	{
		JMenuItem exititem = new JMenuItem(new AbstractAction("Exit")
		{
			public void actionPerformed(ActionEvent e)
			{
				editorwindow.terminate();
			}
		});
		
		exititem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK));
		
		return exititem;
	}
	
	/**
	 *  Saves a model by asking the user for a file.
	 */
	protected void saveWithDialog()
	{
		ModelContainer modelcontainer = editorwindow.getSelectedModelContainer();
		if (modelcontainer != null)
		{
			BetterFileChooser fc = new BetterFileChooser(modelcontainer.getFile());
			FileFilter filter = new FileNameExtensionFilter("BPMN model file (*.bpmn2)", "bpmn2");
			fc.addChoosableFileFilter(filter);
			fc.setFileFilter(filter);
			fc.setAcceptAllFileFilterUsed(false);
			
			if (modelcontainer.getFile() != null)
			{
				fc.setSelectedFile(modelcontainer.getFile());
			}
			
			int result = fc.showSaveDialog(getParent());
			if (JFileChooser.APPROVE_OPTION == result)
			{
				try
				{
					FileNameExtensionFilter ef = (FileNameExtensionFilter) fc.getFileFilter();
					String ext = "." + ef.getExtensions()[0];
					File file = fc.getSelectedFile();
					if (!file.getName().endsWith(ext))
					{
						file = new File(file.getAbsolutePath() + ext);
					}
					SBpmnModelWriter.writeModel(file, modelcontainer.getBpmnModel(), new BpmnVisualModelWriter(modelcontainer.getGraph()));
					modelcontainer.setDirty(false);
					modelcontainer.setFile(file);
					editorwindow.getSettings().setLastFile(file);
				}
				catch (IOException e1)
				{
					displayIOError(e1);
				}
			}
		}
	}
	
	/**
	 *  Displays an error message after a failed IO operation.
	 *  
	 *  @param e The error.
	 */
	protected void displayIOError(Exception e)
	{
		e.printStackTrace();
		JOptionPane.showMessageDialog(getParent(),
			    e.getMessage(),
			    BpmnEditor.APP_NAME,
			    JOptionPane.ERROR_MESSAGE);
		//e1.printStackTrace();
	}
	
	/**
	 *  Returns the model build.
	 *  
	 *  @return The model build.
	 */
	public static int getModelBuild()
	{
		int build = 0;
		try
		{
			Field buildfield = SBpmnModelWriter.class.getField("BUILD");
			if (buildfield != null)
			{
				build = buildfield.getInt(null);
			}
		}
		catch (Exception e)
		{
		}
		
		return build;
	}
}
