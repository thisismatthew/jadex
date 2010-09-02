package jadex.base.gui.componenttree;

import jadex.base.service.remote.ProxyAgent;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.commons.Future;
import jadex.commons.ICommand;
import jadex.commons.IFuture;
import jadex.commons.SGUI;
import jadex.commons.concurrent.DelegationResultListener;
import jadex.commons.concurrent.IResultListener;
import jadex.commons.concurrent.SwingDefaultResultListener;
import jadex.commons.gui.CombiIcon;
import jadex.commons.service.IService;
import jadex.micro.IMicroExternalAccess;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.Timer;
import javax.swing.UIDefaults;

/**
 *  Node that represents a remote component and blends in the
 *  tree of components as virtual children of this node.
 */
public class ProxyComponentTreeNode extends ComponentTreeNode 
{
	//-------- constants --------
	
	/**
	 * The image icons.
	 */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"overlay_proxy_noconnection", SGUI.makeIcon(ProxyComponentTreeNode.class, "/jadex/base/gui/images/overlay_proxy_noconnection.png"),
		"overlay_proxy_connection", SGUI.makeIcon(ProxyComponentTreeNode.class, "/jadex/base/gui/images/overlay_proxy_connection.png"),
	});
	
	//-------- attribute --------

	/** The remote component identifier.*/
	protected IComponentIdentifier cid;
	
	/** The connection state. */
	protected boolean connected;
	
	/** The auto refresh timer. */
	protected Timer timer;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service container node.
	 */
	public ProxyComponentTreeNode(final IComponentTreeNode parent, ComponentTreeModel model, IComponentDescription desc,
		IComponentManagementService cms, Component ui, ComponentIconCache iconcache)
	{
		super(parent, model, desc, cms, ui, iconcache);
		this.connected = false;
		
		timer = new Timer(10000, new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// hmm?! with or without subtree?
				refresh(true);
			}
		});
		timer.start();
	}
	
	/**
	 *  Called when the node is removed or the tree is closed.
	 */
	public void	dispose()
	{
		timer.stop();
	}
	
	/**
	 *  Get the icon for a node.
	 */
	public Icon	getIcon()
	{
		Icon ret = null;
		Icon base = super.getIcon();
		if(base!=null)
		{
			ret = new CombiIcon(new Icon[]{base, connected? 
				icons.getIcon("overlay_proxy_connection"): icons.getIcon("overlay_proxy_noconnection")});
		}
		return ret;
	}
	
	/**
	 *  Asynchronously search for children.
	 *  Called once for each node.
	 *  Should call setChildren() once children are found.
	 */
	protected void	searchChildren()
	{
		// Do not use 'ui' in SwingDefLis because failures will be shown be node color
		getRemoteComponentIdentifier().addResultListener(new SwingDefaultResultListener((Component)null)
		{
			public void customResultAvailable(Object source, Object result)
			{
				final Future	future	= new Future();
				searchChildren(cms, ProxyComponentTreeNode.this, desc, cid, ui, iconcache, future)
					.addResultListener(new SwingDefaultResultListener((Component)null)
				{
					public void customResultAvailable(Object source, Object result)
					{
						setChildren((List)result).addResultListener(new DelegationResultListener(future));
						connected = true;
					}
					
					public void customExceptionOccurred(Object source, Exception exception)
					{
						setChildren(Collections.EMPTY_LIST);
						connected = false;
//						exception.printStackTrace();
					}
				});
			}
			
			public void customExceptionOccurred(Object source, Exception exception)
			{
				setChildren(Collections.EMPTY_LIST);
				connected = false;
//				exception.printStackTrace();
			}
		});				
	}
	
	/**
	 *  Create a string representation.
	 */
	public String toString()
	{
		return cid==null? desc.getName().getLocalName(): desc.getName().getLocalName()+"("+cid+")";
	}
	
	/**
	 *  Asynchronously search for children.
	 *  Called once for each node.
	 *  Should call setChildren() once children are found.
	 */
	protected static IFuture searchChildren(final IComponentManagementService cms, final IComponentTreeNode parentnode,
		final IComponentDescription desc, final IComponentIdentifier cid, final Component ui, final  ComponentIconCache iconcache,
		final // future for determining when services can be added to service container.
		Future future)
	{
		final Future ret = new Future();
	
		final List children = new ArrayList();
		final boolean	ready[]	= new boolean[2];	// 0: children, 1: services;

		IComponentTreeNode tmp = parentnode;
		while(!(tmp instanceof ProxyComponentTreeNode))
			tmp = tmp.getParent();
		final ProxyComponentTreeNode proxy = (ProxyComponentTreeNode)tmp;
		
		cms.getExternalAccess(proxy.getDescription().getName()).addResultListener(new IResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
				final IMicroExternalAccess exta = (IMicroExternalAccess)result;
				exta.scheduleStep(new ICommand()
				{
					public void execute(Object agent)
					{
						ProxyAgent pa = (ProxyAgent)agent;
						pa.getVirtualChildren(cid).addResultListener(new SwingDefaultResultListener((Component)null)
						{
							public void customResultAvailable(Object source, Object result)
							{
								IComponentDescription[] descs = (IComponentDescription[])
									((Collection)result).toArray(new IComponentDescription[((Collection)result).size()]);
								for(int i=0; i<descs.length; i++)
								{
									IComponentTreeNode node = proxy.getModel().getNode(descs[i].getName());
									if(node==null)
									{
										node = new VirtualComponentTreeNode(parentnode, proxy.getModel(), descs[i], cms, ui, iconcache);
									}
//									System.err.println(proxy.getModel().hashCode()+", "+ready.hashCode()+" searchChildren.add "+node);
									children.add(node);
								}
								
								ready[0] = true;
								if(ready[0] && ready[1])
								{
									ret.setResult(children);
								}
							}
							
							public void customExceptionOccurred(Object source, Exception exception)
							{
								// 2 parallel search branches, i.e. one may fail first
								ret.setExceptionIfUndone(exception);
							}
						});
					}
				});
				
				exta.scheduleStep(new ICommand()
				{
					public void execute(Object agent)
					{
						ProxyAgent pa = (ProxyAgent)agent;
						pa.getRemoteServices(cid).addResultListener(new SwingDefaultResultListener((Component)null)
						{
							public void customResultAvailable(Object source, Object result)
							{
								List services = (List)result;
								if(services!=null && !services.isEmpty())
								{
									ServiceContainerNode scn = (ServiceContainerNode)
										proxy.getModel().getNode(desc.getName().getName()+"ServiceContainer");
									if(scn==null)
										scn	= new ServiceContainerNode(parentnode, proxy.getModel());
//									System.err.println(proxy.getModel().hashCode()+", "+ready.hashCode()+" searchChildren.add "+scn);
									children.add(0, scn);
									final List subchildren = new ArrayList();
									for(int i=0; i<services.size(); i++)
									{
										IService service = (IService)services.get(i);
										ServiceNode	sn = (ServiceNode)proxy.getModel().getNode(service.getServiceIdentifier());
										if(sn==null)
											sn = new ServiceNode(scn, proxy.getModel(), service);
										subchildren.add(sn);
									}
									
									final ServiceContainerNode	node	= scn;
									future.addResultListener(new SwingDefaultResultListener((Component)null)
									{
										public void customResultAvailable(Object source, Object result)
										{
											node.setChildren(subchildren);
										}
										public void customExceptionOccurred(Object source, Exception exception)
										{
											ret.setExceptionIfUndone(exception);
										}
									});
								}

								ready[1] = true;
								if(ready[0] &&  ready[1])
								{
									ret.setResult(children);
								}
							}
							
							public void customExceptionOccurred(Object source, Exception exception)
							{
								// 2 parallel search branches, i.e. one may fail first
								ret.setExceptionIfUndone(exception);
							}
						});
					}
				});
			}
			
			public void exceptionOccurred(Object source, Exception exception)
			{
				ret.setException(exception);
			}
		});
		
		return ret;
	}

	/**
	 *  Get the remote component identifier.
	 *  @return The remote identifier.
	 */
	public IFuture getRemoteComponentIdentifier()
	{
		final Future ret = new Future();
		
		if(cid==null)
		{
			cms.getExternalAccess(desc.getName()).addResultListener(new IResultListener()
			{
				public void resultAvailable(Object source, Object result)
				{
					final IMicroExternalAccess exta = (IMicroExternalAccess)result;
					exta.scheduleStep(new ICommand()
					{
						public void execute(Object agent)
						{
							ProxyAgent pa = (ProxyAgent)agent;
							cid = pa.getRemotePlatformIdentifier();
							ret.setResult(cid);
						}
					});
				}
				
				public void exceptionOccurred(Object source, Exception exception)
				{
					ret.setException(exception);
				}
			});
		}
		else
		{
			ret.setResult(cid);
		}
		
		return ret;
	}

	/**
	 *  Get the connected.
	 *  @return the connected.
	 */
	public boolean isConnected()
	{
		return connected;
	}
	
	
}
