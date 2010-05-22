package deco4mas.examples.agentNegotiation.sma.strategy;

import jadex.bridge.IComponentIdentifier;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import deco4mas.examples.agentNegotiation.deco.ServiceProposal;

public interface IUtilityFunction
{
	/**
	 * Benchmark the given Set of ServicePropsals
	 * Should use evaluate for every single proposal
	 */
	public SortedMap<Double, IComponentIdentifier> benchmarkProposals(Set<ServiceProposal> participants);

	public Double evaluate(Map<String, Double> evaluateVector);
}
