package jadex.micro.tutorial;

import jadex.bridge.IExternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  This chat service can provide a user profile.
 */
public class ChatServiceD3 extends ChatServiceD2 implements IExtendedChatService
{
	protected static List profiles;

	protected UserProfileD3 profile;
	
	static
	{
		profiles = new ArrayList();
		profiles.add(new UserProfileD3("John Doh", 33, false, "I like football, dart and beer."));
		profiles.add(new UserProfileD3("Anna Belle", 21, true, "I like classic music."));
		profiles.add(new UserProfileD3("Prof. Smith", 58, false, "I like Phdcomics."));
	}
	
	/**
	 *  Get the user profile.
	 *  @return The user profile.
	 */
	public IFuture getUserProfile()
	{
		if(profile==null)
			this.profile = (UserProfileD3)profiles.get((int)(Math.random()*profiles.size()));
		return new Future(profile);
	}
	
	/**
	 *  Create the gui.
	 */
	protected ChatGuiD2 createGui(IExternalAccess agent)
	{
		return new ChatGuiD3(agent);
	}
}
