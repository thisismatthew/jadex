package deco4mas.examples.agentNegotiation;

public class ServiceType
{

	private String name;

	private Double maxCost;
	private Double minCost;
	private Double medCost;

	private Double maxDuration;
	private Double minDuration;
	private Double medDuration;

	public ServiceType(String name, Double maxCost, Double minCost, Double medCost, Double maxDuration, Double minDuration,
		Double medDuration)
	{
		this.name = name;
		this.maxCost = maxCost;
		this.minCost = minCost;
		this.medCost = medCost;
		this.maxDuration = maxDuration;
		this.minDuration = minDuration;
		this.medDuration = medDuration;
	}

	public String getName()
	{
		return name;
	}

	public Double getMaxCost()
	{
		return maxCost;
	}

	public Double getMinCost()
	{
		return minCost;
	}

	public Double getMedCost()
	{
		return medCost;
	}

	public Double getMaxDuration()
	{
		return maxDuration;
	}

	public Double getMinDuration()
	{
		return minDuration;
	}

	public Double getMedDuration()
	{
		return medDuration;
	}
}
