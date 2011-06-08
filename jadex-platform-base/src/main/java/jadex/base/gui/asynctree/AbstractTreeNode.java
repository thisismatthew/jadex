package jadex.base.gui.asynctree;

import jadex.base.Starter;
import jadex.commons.SUtil;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;


/**
 *  Basic node object.
 */
public abstract class AbstractTreeNode	implements ITreeNode 
{
	//-------- attributes --------
	
	/** The parent node. */
	protected final ITreeNode	parent;
	
	/** The tree model. */
	protected final AsyncTreeModel	model;
	
	/** The tree. */
	// Hack!!! Model should not have access to ui, required for refresh only on expanded nodes.
	protected final JTree	tree;
	
	/** The cached children. */
	private List	children;
	
	/** Flag to indicate search in progress. */
	protected boolean	searching;

	/** Flag to indicate recursive refresh. */
	protected boolean	recurse;
	
	/** Flag to indicate that children were added / removed during ongoing search (->restart search). */
	protected boolean	dirty;
	
	/** The children future (result of next search). */
	protected Future	childrenfuture;
	
	//-------- constructors --------
	
	/**
	 *  Create a node.
	 */
	public AbstractTreeNode(ITreeNode parent, AsyncTreeModel model, JTree tree)
	{
		this.parent	= parent;
		this.model	= model;
		this.tree	= tree;
	}
	
	//-------- IComponentTreeNode interface --------
	
	/**
	 *  Called when the node is removed or the tree is closed.
	 */
	public void	dispose()
	{
	}
	
	/**
	 *  Get the parent node.
	 */
	public ITreeNode	getParent()
	{
		return parent;
	}
	
	/**
	 *  Get the child count.
	 */
	public int	getChildCount()
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		if(children==null && !searching)
		{
			searching	= true;
			searchChildren();
		}
		return children==null ? 0 : children.size();
	}
	
	/**
	 *  Get the given child.
	 */
	public ITreeNode	getChild(int index)
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		if(children==null && !searching)
		{
			searching	= true;
			searchChildren();
		}
		return children==null ? null : (ITreeNode)children.get(index);
	}
	
	/**
	 *  Get the index of a child.
	 */
	public int	getIndexOfChild(ITreeNode child)
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		if(children==null && !searching)
		{
			searching	= true;
			searchChildren();
		}
		return children==null ? -1 : children.indexOf(child);
	}
	
	/**
	 *  Check if the node is a leaf.
	 */
	public boolean	isLeaf()
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		return getChildCount()==0;
	}
	
	/**
	 *  Refresh the node.
	 *  @param recurse	Recursively refresh subnodes, if true.
	 */
	public void	refresh(boolean recurse)
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

//		System.out.println("refresh: "+getId());
		
		if(!searching)
		{
			searching	= true;
			this.recurse	= recurse;
			searchChildren();
		}
		else
		{
			// If search in progress upgrade to recursive, but do not downgrade.
			this.recurse	= this.recurse || recurse;
			dirty=true;
		}
		tree.repaint();
	}
	
	/**
	 *  Get the cached children, i.e. do not start any background processes for updating the children.
	 */
	public List	getCachedChildren()
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		return children!=null ? children : Collections.EMPTY_LIST;
	}
	
	/**
	 *  Get the current children, i.e. start a new update process and provide the result as a future.
	 */
	public IFuture	getChildren()
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		if(childrenfuture==null)
		{
			childrenfuture	= new Future();
		}
		
		IFuture	ret	= childrenfuture;
		
		searchChildren();	// might reset childrenfuture.
		
		return ret;
	}

	/**
	 *  True, if the node has properties that can be displayed.
	 */
	public boolean	hasProperties()
	{
		return false;
	}

	
	/**
	 *  Get or create a component displaying the node properties.
	 *  Only to be called if hasProperties() is true;
	 */
	public JComponent	getPropertiesComponent()
	{
		throw new UnsupportedOperationException("Node has no properties: "+this);
	}
	
	//-------- template methods --------
	
	
	/**
	 *  Get the icon for a node.
	 */
	public abstract Icon	getIcon();
	
	/**
	 *  Get tooltip text.
	 */
	public abstract String getTooltipText();

	/**
	 *  Asynchronously search for children.
	 *  Called once for each node.
	 *  Should call setChildren() once children are found.
	 */
	protected abstract void	searchChildren();
	
	/**
	 *  Set the children.
	 *  No children should be represented as empty list to avoid
	 *  ongoing search for children.
	 */
	protected void	setChildren(List newchildren)
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		List	oldcs	= children!=null ? new ArrayList(children) : null;
		List	newcs	= newchildren!=null ? new ArrayList(newchildren) : null;
		
		assert false || checkChildren(oldcs, newcs);
		
		searching	= false;
 		if(dirty)
		{
			// Restart search when nodes have been added/removed in the mean time.
			dirty	= false;
			searchChildren();
		}
		else
		{
//			System.err.println(""+model.hashCode()+" setChildren executing: "+parent+"/"+AbstractTreeNode.this+", "+children+", "+newcs);
			boolean	dorecurse	= recurse;
			recurse	= false;
			
			if(children==null)
				children	= new ArrayList();
			
			// New update algorithm (can cope with changing orderings)
			boolean	changed	= false;
			// Traverse through source list and remove changed nodes
			int	newidx	= 0;	// Pointer to target list.
			int removed	= 0;	// Counter for correct tree event index
			for(int i=0; oldcs!=null && i<oldcs.size(); i++)
			{
				ITreeNode	node	= (ITreeNode)oldcs.get(i);
				if(newcs!=null && newidx<newcs.size() && node.equals(newcs.get(newidx)))
				{
					// Node at correct position -> move on
					newidx++;
				}
				else
				{
					// Node removed or moved -> remove (moved nodes will be re-added later at correct position)
					children.remove(node);
					model.deregisterNode(node);
					model.fireNodeRemoved(AbstractTreeNode.this, node, i-removed);
					removed++;
					changed	= true;
				}
			}
			// Traverse through target list and add missing nodes
			for(int i=0; newcs!=null && i<newcs.size(); i++)
			{
				ITreeNode	node	= (ITreeNode)newcs.get(i);
				if(i>=children.size() || !node.equals(children.get(i)))
				{
					// Add missing or moved node.
					children.add(i, node);
					model.addNode(node);
					model.fireNodeAdded(AbstractTreeNode.this, node, i);
					changed	= true;
				}
			}
			if(changed)
				model.fireNodeChanged(AbstractTreeNode.this);
			
			// Old update algorithm (only when order does not change)
//			List	added	= new ArrayList();
//			List	removed	= new ArrayList();
//			if(oldcs!=null)
//			{
//				removed.addAll(oldcs);
//			}
//			if(newcs!=null)
//			{
//				added.addAll(newcs);
//				removed.removeAll(newcs);
//			}
//			if(oldcs!=null)
//			{
//				added.removeAll(oldcs);
//			}
//
//			if(!removed.isEmpty())
//			{
//				for(int i=removed.size()-1; i>=0; i--)
//				{
//					ITreeNode	node	= (ITreeNode)removed.get(i);
//					int index	= oldcs.indexOf(node);
//					Object	r	= children.remove(index);
//					assert SUtil.equals(r, node) : "Node inconsistency: "+oldcs+", "+newcs;
//					model.deregisterNode(node);
//					model.fireNodeRemoved(AbstractTreeNode.this, node, index);
//				}
//				if(added.isEmpty())
//					model.fireNodeChanged(AbstractTreeNode.this);
//			}
//			if(!added.isEmpty())
//			{
//				for(int i=0; i<added.size(); i++)
//				{
//					ITreeNode	node	= (ITreeNode)added.get(i);
//					int index	= newcs.indexOf(node);
//					children.add(index, node);
//					model.addNode(node);
//					model.fireNodeAdded(AbstractTreeNode.this, node, index);
//				}
//				model.fireNodeChanged(AbstractTreeNode.this);
//			}
				
			assert SUtil.equals(children, newcs) : "Node inconsistency:\noldcs="+oldcs+"\nnewcs="+newcs
//				+"\nadded="+added+"\nremoved="+removed
				+"\nresult="+children;
			
			if(childrenfuture!=null)
			{
				childrenfuture.setResult(new ArrayList(children));
				childrenfuture	= null;
			}
			
			if(dorecurse && tree.isExpanded(new TreePath(model.buildTreePath(AbstractTreeNode.this).toArray())))
			{
				for(int i=0; children!=null && i<children.size(); i++)
				{
					((ITreeNode)children.get(i)).refresh(dorecurse);
				}
			}
		}
	}
	
	/**
	 *  Check the children for validity.
	 *  I.e. it is not allowed to have two equal children in the list
	 *  or to alter the ordering of existing children.
	 */
	protected boolean checkChildren(List oldcs, List newcs)
	{
		// Check if duplicates are present. 
		if(newcs!=null && newcs.size()>1)
		{
			for(int i=0; i<newcs.size()-1; i++)
			{
				for(int j=i+1; j<newcs.size(); j++)
				{
					if(SUtil.equals(newcs.get(i), newcs.get(j)))
					{
						throw new RuntimeException("Found equal children: "+newcs);
					}
				}
			}
		}
		
//		// Check ordering of existing children.
//		if(oldcs!=null && newcs!=null)
//		{
//			List	intersection	= new ArrayList(oldcs);
//			intersection.retainAll(newcs);
//			for(int i=0; i<intersection.size()-1; i++)
//			{
//				for(int j=i+1; j<intersection.size(); j++)
//				{
//					int	index1	= newcs.indexOf(intersection.get(i));
//					int index2	= newcs.indexOf(intersection.get(j));
//					if(index1>=index2)
//					{
//						throw new RuntimeException("Found unequal ordering:\n oldcs = "+oldcs+"\nnewcs = "+newcs);
//					}
//				}
//			}
//		}

		return true;
	}
	
	/**
	 *  Get the model.
	 */
	public AsyncTreeModel	getModel()
	{
		return model;
	}

	/**
	 *  Get the tree.
	 */
	public JTree	getTree()
	{
		return tree;
	}

	/**
	 *  Add a child and update the tree.
	 *  Must be called from swing thread.
	 */
	public void addChild(int index, ITreeNode node)
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		// Ignore when node already removed.
		if(!model.isZombieNode(node.getId()))
		{
			if(children==null)
				children = new ArrayList();
			children.add(index, node);
			model.addNode(node);
			model.fireNodeAdded(this, node, index);
			if(searching)
				dirty	= true;
			
//			System.err.println("Node added: "+children);
		}
		else
		{
			model.deregisterNode(node);
		}
	}
	
	/**
	 *  Add a child and update the tree.
	 *  Must be called from swing thread.
	 */
	public void addChild(ITreeNode node)
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		addChild(getCachedChildren().size(), node);
	}
	
	/**
	 *  Remove a child and update the tree.
	 *  Must be called from swing thread.
	 */
	public void removeChild(ITreeNode node)
	{
		assert SwingUtilities.isEventDispatchThread() ||  Starter.isShutdown();

		int index	= getIndexOfChild(node);
		if(index!=-1)
		{
			children.remove(node);
			model.deregisterNode(node);
			model.fireNodeRemoved(this, node, index);
			if(searching)
				dirty	= true;
		}
	}
}
