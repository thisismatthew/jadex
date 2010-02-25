package jadex.tools.common;

import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentIdentifier;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 *  A panel for displaying/editing an agent identifier.
 */
public class ComponentIdentifierPanel extends JPanel
{
	//-------- attributes --------

	/** The agent identifier.*/
	protected IComponentManagementService ces;
	
	/** The agent identifier.*/
	protected IComponentIdentifier	aid;

	/** The name textfield. */
	protected JTextField	tfname; 

	/** Listener for name updates. */
	protected DocumentListener	namelistener;	
	
	/** Flag indicating that the user is currently editing the name. */
	protected boolean	nameediting;	
	
	/** The addresses table. */
	//protected JTable	taddresses;
	protected EditableList	taddresses;

	/** The editable state. */
	protected boolean	editable;	
	
	//-------- constructors --------

	/**
	 *  Create a new agent identifier panel.
	 *  @param aid	The agent identifier (or null for new).
	 */
	public ComponentIdentifierPanel(IComponentIdentifier aid, IComponentManagementService ces)
	{
		this.ces	= ces;
		this.aid	= aid!=null ? aid : ces.createComponentIdentifier(null, false, null);
		this.editable	= true;

		// Constraints for labels (displayed left).
		int	row	= 0;
		GridBagConstraints	leftcons	= new GridBagConstraints(0, 0, 1, 1, 0, 0,
			GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1,1,1,1), 0, 0);
		// Constraints for components (displayed right of a label).
		GridBagConstraints	rightcons	= (GridBagConstraints)leftcons.clone();
		rightcons.gridx	= 1;
		rightcons.gridwidth	= GridBagConstraints.REMAINDER;
		rightcons.weightx	= 1;
		// Constraints for full-size components (resize in all directions).
		GridBagConstraints	fullcons	= (GridBagConstraints)rightcons.clone();
		fullcons.gridx	= 0;
		fullcons.weighty	= 1;
		fullcons.fill	= GridBagConstraints.BOTH;

		// Initialize component.
		this.setLayout(new GridBagLayout());

		// Name
		tfname	= new JTextField(this.aid.getName(), 20);
		this.namelistener	= new NameListener();
		tfname.getDocument().addDocumentListener(namelistener);
		leftcons.gridy	= rightcons.gridy	= fullcons.gridy	= row++;
		this.add(new JLabel("Name: "), leftcons);
		this.add(tfname, rightcons);

		taddresses = new EditableList("Addresses");
		taddresses.getModel().addTableModelListener(new TableModelListener()
		{
			public void tableChanged(TableModelEvent e)
			{
				//System.out.println("event: "+e);
				ComponentIdentifierPanel.this.aid	= ComponentIdentifierPanel.this.ces
					.createComponentIdentifier(ComponentIdentifierPanel.this.aid.getName(), false, taddresses.getEntries());
				aidChanged();
			}
		});

		JScrollPane	scroll	= new JScrollPane(taddresses);
		leftcons.gridy	= rightcons.gridy	= fullcons.gridy	= row++;
		this.add(scroll, fullcons);
	}

	/**
	 *  Template method to be overriden by subclasses.
	 *  Called when the AID has been changed through user input.
	 */
	protected void aidChanged()
	{
	}

	//-------- methods --------

	/**
	 *  Get the agent identifier.
	 */
	public IComponentIdentifier	getAgentIdentifier()
	{
		return this.aid;
	}

	/**
	 *  Set the agent identifier.
	 */
	public void setAgentIdentifier(IComponentIdentifier aid)
	{
		this.aid	= aid!=null ? aid : ces.createComponentIdentifier(null, false, null);
		taddresses.setEntries(this.aid.getAddresses());
		refresh();
	}

	/**
	 *  Change the editable state.
	 */
	public void	setEditable(boolean editable)
	{
		if(this.editable!=editable)
		{
			this.editable	= editable;
			tfname.setEditable(editable);
			refresh();
		}
	}
	
	//-------- helper methods --------
	
	/**
	 *  Update the ui, when the aid has changed.
	 */
	protected void refresh()
	{
		// Update the gui.
		if(!nameediting)
		{
			tfname.getDocument().removeDocumentListener(namelistener);
			tfname.setText(this.aid.getName());
			tfname.getDocument().addDocumentListener(namelistener);
		}

		taddresses.refresh();
		this.invalidate();
		this.validate();
		this.repaint();
	}

	//-------- helper classes --------

	public class NameListener implements DocumentListener
	{
		public void changedUpdate(DocumentEvent e)
		{
			update();
		}

		public void insertUpdate(DocumentEvent e)
		{
			update();
		}

		public void removeUpdate(DocumentEvent e)
		{
			update();
		}
		
		protected void	update()
		{
			nameediting	= true;
			ComponentIdentifierPanel.this.aid	= ces.createComponentIdentifier(tfname.getText(), true, aid.getAddresses());
			aidChanged();
			nameediting	= false;
		}
	}
}
