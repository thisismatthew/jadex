package jadex.bdi.planlib.cms;

import jadex.adapter.base.fipa.CMSSearchComponents;
import jadex.adapter.base.fipa.Done;
import jadex.adapter.base.fipa.SFipa;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IComponentDescription;
import jadex.bridge.ISearchConstraints;

/**
 *  Search for components on a remote platform.
 */
public class CMSRemoteSearchComponentsPlan extends Plan
{
	/**
	 * The body method is called on the
	 * instatiated plan instance from the scheduler.
	 */
	public void body()
	{
		CMSSearchComponents sa = new CMSSearchComponents();
		sa.setComponentDescription((IComponentDescription)getParameter("description").getValue());
		sa.setSearchConstraints((ISearchConstraints)getParameter("constraints").getValue());

		IGoal req = createGoal("rp_initiate");
		req.getParameter("receiver").setValue(getParameter("cms").getValue());
		req.getParameter("action").setValue(sa);
		req.getParameter("ontology").setValue(SFipa.COMPONENT_MANAGEMENT_ONTOLOGY_NAME);
		dispatchSubgoalAndWait(req);

		getParameterSet("result").addValues(((CMSSearchComponents)((Done)req.getParameter("result")
			.getValue()).getAction()).getComponentDescriptions());
	}
}

