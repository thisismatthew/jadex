package jadex.base.gui.filetree;

import jadex.base.gui.asynctree.ITreeNode;

/**
 *  Common interface for all nodes of the file tree
 *  (except the invisible root node).
 *  Provides unified access to local and remote files.
 */
public interface IFileNode extends ITreeNode
{
	/**
	 *  Get the file name.
	 */
	public String	getFileName();
	
	/**
	 *  Check if the file is a directory. 
	 */
	public boolean	isDirectory();
}
