package jadex.bpmn.editor.gui.propertypanels;

import jadex.bpmn.editor.gui.ModelContainer;
import jadex.bpmn.editor.model.visual.VActivity;
import jadex.bpmn.model.MActivity;
import jadex.bridge.ClassInfo;
import jadex.bridge.modelinfo.UnparsedExpression;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;

/**
 *  Property panel for timer event activities.
 *
 */
public class TimerEventPropertyPanel extends BasePropertyPanel
{
	/** The event activity. */
	protected VActivity event;
	
	/**
	 *  Creates a new property panel.
	 *  @param container The model container.
	 */
	public TimerEventPropertyPanel(ModelContainer container, VActivity event)
	{
		super("Timer Event", container);
		this.event = event;
		
		int y = 0;
		int colnum = 0;
		JPanel column = createColumn(colnum++);
		
		JLabel label = new JLabel("Duration");
		JTextArea durarea = new JTextArea();
		UnparsedExpression dur = (UnparsedExpression) ((MActivity) event.getBpmnElement()).getPropertyValue("duration");
		if (dur != null)
		{
			durarea.setText(dur.getValue());
		}
		durarea.getDocument().addDocumentListener(new DocumentAdapter()
		{
			public void update(DocumentEvent e)
			{
				UnparsedExpression dur = new UnparsedExpression("duration", "java.lane.Number", getText(e.getDocument()), null);
				((MActivity) TimerEventPropertyPanel.this.event.getBpmnElement()).setPropertyValue("duration", dur);
				modelcontainer.setDirty(true);
			}
		});
		configureAndAddInputLine(column, label, durarea, y++);
		
		addVerticalFiller(column, y);
	}
}
