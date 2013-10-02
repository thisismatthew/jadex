package jadex.bdiv3.examples.university;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanPrecondition;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.IPlan;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Configuration;
import jadex.micro.annotation.Configurations;

/**
 *  Go to university example taken from  
 *  Winikoff, Padgham: developing intelligent agent systems, 2004.
 */
//@BDIConfigurations({
//	@BDIConfiguration(name="sunny", initialbeliefs=@NameValue(name="raining", value="false")),
//	@BDIConfiguration(name="rainy", initialbeliefs=@NameValue(name="raining", value="true"))
//})
@Configurations({@Configuration(name="sunny"), @Configuration(name="rainy")})
@Agent
public class UniversityBDI
{
	/** The bdi agent. */
	@Agent
	protected BDIAgent agent;
	
	@Belief
	protected boolean raining = agent.getConfiguration().equals("rainy");
	
	@Goal
	protected class ComeToUniGoal
	{
	}
	
	@Goal
	protected static class TakeXGoal
	{
		public enum Type{TRAIN, TRAM};
		
		protected Type type;
		
		public TakeXGoal(Type type)
		{
			this.type = type;
		}

		public Type getType()
		{
			return type;
		}
	}
	
	@AgentBody
	public void body()
	{
		System.out.println("rainy: "+raining);
//		if(agent.getConfiguration().equals("rainy"))
//			raining = true;
		try
		{
			agent.dispatchTopLevelGoal(new ComeToUniGoal()).get();
		}
		catch(Exception e)
		{
			System.out.println("stayed at home");
		}
	}
	
	// Walk only if its not raining and not as first choice
	@Plan(trigger=@Trigger(goals=ComeToUniGoal.class), priority=-1)
	protected class WalkPlan
	{
		@PlanPrecondition
		protected boolean checkWeather()
		{
			return !raining;
		}
		
		@PlanBody
		protected void walk()
		{
			System.out.println("Walked to Uni.");
		}
	}
	
	// Only take train when its raining (too expensive)
	@Plan(trigger=@Trigger(goals=ComeToUniGoal.class))
	protected class TrainPlan
	{
		@PlanPrecondition
		protected boolean checkWeather()
		{
			return raining;
		}
		
		@PlanBody
		protected void takeTrain(IPlan plan)
		{
			System.out.println("Trying to take train to Uni.");
			plan.dispatchSubgoal(new TakeXGoal(TakeXGoal.Type.TRAIN)).get();
			System.out.println("Took train to Uni.");
		}
	}

	// Tram is always a good idea
	@Plan(trigger=@Trigger(goals=ComeToUniGoal.class))
	protected void tramPlan(IPlan plan)
	{
		System.out.println("Trying to take tram to Uni.");
		plan.dispatchSubgoal(new TakeXGoal(TakeXGoal.Type.TRAM)).get();
		System.out.println("Took tram to Uni.");
	}
	
	@Plan(trigger=@Trigger(goals=TakeXGoal.class))
	protected void takeX(TakeXGoal goal)
	{
		System.out.println("Walking to station.");
		System.out.println("Checking time table.");
		if(Math.random()>0.5)
		{
			System.out.println("Wait time is too long, failed.");
			throw new PlanFailureException();
		}
		else
		{
			System.out.println("Taking "+goal.getType());
		}
	}
}
