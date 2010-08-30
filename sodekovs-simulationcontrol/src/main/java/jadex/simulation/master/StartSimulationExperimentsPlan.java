package jadex.simulation.master;

import jadex.application.runtime.IApplication;
import jadex.application.runtime.IApplicationExternalAccess;
import jadex.application.space.envsupport.MEnvSpaceInstance;
import jadex.application.space.envsupport.environment.AbstractEnvironmentSpace;
import jadex.application.space.envsupport.evaluation.DefaultDataProvider;
import jadex.application.space.envsupport.evaluation.IObjectSource;
import jadex.application.space.envsupport.evaluation.ITableDataConsumer;
import jadex.application.space.envsupport.evaluation.ITableDataProvider;
import jadex.application.space.envsupport.evaluation.SpaceObjectSource;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.Plan;
import jadex.bridge.CreationInfo;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.commons.IFuture;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.javaparser.IExpressionParser;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.javaccimpl.JavaCCExpressionParser;
import jadex.service.library.ILibraryService;
import jadex.simulation.helper.Constants;
import jadex.simulation.helper.ObjectCloner;
import jadex.simulation.helper.XMLHandler;
import jadex.simulation.model.Data;
import jadex.simulation.model.Dataconsumer;
import jadex.simulation.model.Dataprovider;
import jadex.simulation.model.ObservedEvent;
import jadex.simulation.model.Optimization;
import jadex.simulation.model.SimulationConfiguration;
import jadex.simulation.model.Source;
import jadex.simulation.model.result.ExperimentResult;
import jadex.simulation.model.result.IntermediateResult;
import jadex.simulation.model.result.RowResult;
import jadex.xml.IContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartSimulationExperimentsPlan extends Plan {

	public void body() {
		System.out.println("#StartSimulationExpPlan# Start Simulation Experiments at Master.");

		HashMap beliefbaseFacts = (HashMap) getBeliefbase().getBelief("generalSimulationFacts").getFact();
		SimulationConfiguration simConf = (SimulationConfiguration) getBeliefbase().getBelief("simulationConf").getFact();

		System.out.println("#StartSimulationExpPlan# Path: " + simConf.getApplicationReference());

		// how many experiments to do within this row
		long experimentsPerRowToMake = ((Long) beliefbaseFacts.get(Constants.EXPERIMENTS_PER_ROW_TO_DO)).longValue();
		int totalRuns = ((Integer) beliefbaseFacts.get(Constants.TOTAL_EXPERIMENT_COUNTER)).intValue();
		int expInRow = ((Integer) beliefbaseFacts.get(Constants.ROW_EXPERIMENT_COUNTER)).intValue();
		int rowCounter = ((Integer) beliefbaseFacts.get(Constants.EXPERIMENT_ROW_COUNTER)).intValue();

		// Prepare values for the experiments of this row
		String fileName = simConf.getApplicationReference();
		String configName = simConf.getApplicationConfiguration();

		// prepare object that handles the result
		RowResult rowResult = new RowResult();
		rowResult.setStarttime(getClock().getTime());
		rowResult.setId(String.valueOf(rowCounter));

		// Put args into application. This args are passed to the
		// application.xml to parameterize application.
		Map args = new HashMap();
		
		// check, whether parameters have to be swept
		if (simConf.getOptimization().getParameterSweeping() != null) {
			sweepParameters(simConf, args);
		}

		// update GUI: create new panel/table for new ensemble
		// ControlCenter gui = (ControlCenter)
		// getBeliefbase().getBelief("tmpGUI").getFact();
		// gui.createNewEnsembleTable(rowCounter);

		for (long i = 0; i < experimentsPerRowToMake; i++) {
			
			
			// Put SimulationConfiguration into the application.xml as parameter for
			// the SimulationClient.
			Map simFacts = new HashMap();
			try {
				simFacts.put(Constants.SIMULATION_FACTS_FOR_CLIENT, ObjectCloner.deepCopy(simConf));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			args.put(Constants.SIMULATION_FACTS_FOR_CLIENT, simFacts);
//			args.put(Constants.OBSERVED_EVENTS_MAP, observedEvents);				

			
			String experimentID = rowCounter + "." + expInRow;
			String appName = simConf.getName() + experimentID;

			args.put(Constants.EXPERIMENT_ID, experimentID);
			// put tmp_start_time into args in order to observer duration of
			// experiment
			long tmp_start_time = getClock().getTime();
			args.put("tmp_start_time", tmp_start_time);
			((HashMap) args.get(Constants.SIMULATION_FACTS_FOR_CLIENT)).put(Constants.EXPERIMENT_ID, experimentID);

			startApplication(appName, fileName, configName, args);

			System.out.println("#*****************************************************************.");
			System.out.println("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024000);
			System.out.println("#*****************************************************************.");

			System.out.println("#StartSimulationExpPlan# Started new Simulation Experiment. Nr.:" + experimentID + "(" + totalRuns + ") with Optimization Values: "
					+ simConf.getOptimization().getData().getName() + " = " + simConf.getOptimization().getParameterSweeping().getCurrentValue());
			totalRuns++;
			expInRow++;

			// update static part of control center
			// gui.updateStaticTable(rowCounter, expInRow);

			waitForInternalEvent("triggerNewExperiment");
			System.out.println("#StartSimulationExpPlan# Received Results of Client!!!!");
			// HACK: Ein warten scheint notwendig zu sein..., damit Ausf�hrung
			// korrekt l�uft.
			// waitFor(2000);
			// System.out.println("2Received Results!!!!");
			beliefbaseFacts.put(Constants.TOTAL_EXPERIMENT_COUNTER, new Integer(totalRuns));
			beliefbaseFacts.put(Constants.ROW_EXPERIMENT_COUNTER, new Integer(expInRow));
			getBeliefbase().getBelief("generalSimulationFacts").setFact(beliefbaseFacts);

			// update experiment counter in intermediate results
			IntermediateResult interRes = (IntermediateResult) getBeliefbase().getBelief("intermediateResults").getFact();
			interRes.setCurrentExperimentNumber(expInRow);
			getBeliefbase().getBelief("intermediateResults").setFact(interRes);
		}

		// Increment row counter
		rowCounter++;
		beliefbaseFacts.put(Constants.EXPERIMENT_ROW_COUNTER, new Integer(rowCounter));
		getBeliefbase().getBelief("generalSimulationFacts").setFact(beliefbaseFacts);

		// store results of row
		HashMap<Integer, ExperimentResult> experimentResults = (HashMap<Integer, ExperimentResult>) getBeliefbase().getBelief("experimentResults").getFact();// contains
		// the
		// results of the
		// experiments done
		// in this row
		HashMap rowResults = (HashMap) getBeliefbase().getBelief("rowResults").getFact();

		// System.out.println("#StartSimEx# tttttttttttttttttttttttttt.");
		// for(ExperimentResult ttt : experimentResults.values()){
		// System.out.println("#StartSimEx# ttt " + ttt.toString());
		// XMLHandler.writeXML(ttt, "rowRes.xml", ExperimentResult.class);
		// }
		//		
		ArrayList<ExperimentResult> experimentList = new ArrayList<ExperimentResult>(experimentResults.values());
		rowResult.setExperimentsResults(experimentList);
		rowResult.setEndtime(getClock().getTime());
		rowResult.setName("Tmp-Test");
		rowResult.setOptimizationName(experimentList.get(0).getOptimizationParameterName());
		rowResult.setOptimizationValue(experimentList.get(0).getOptimizationValue());
		rowResult.setFinalStatsMap( ((IntermediateResult)getBeliefbase().getBelief("intermediateResults").getFact()).getIntermediateStats());

		rowResults.put(rowResult.getId(), rowResult);

		getBeliefbase().getBelief("experimentResults").setFact(new HashMap());
//		getBeliefbase().getBelief("intermediateResults").setFact(new IntermediateResult(rowCounter, 0, simConf));
		getBeliefbase().getBelief("rowResults").setFact(rowResults);

		// System.out.println("#StartSimEx# Try to write RowResult to XML-File.");

		// System.out.println("#StartSimExp# Write Row Res to XML");
		// XMLHandler.writeXML(rowResult, "rowRes" +".xml", RowResult.class);

		// evaluate row
		dispatchInternalEvent(createInternalEvent("triggerExperimentRowEvaluation"));
	}


	private void startApplication(String appName, String fileName, String configName, Map args) {

		try {
			IComponentManagementService executionService = (IComponentManagementService) getScope().getServiceContainer().getService(IComponentManagementService.class);

			// create application in order to add additional components to
			// application
			IFuture fut = executionService.createComponent(appName, fileName, new CreationInfo(configName, args, null, false, false), null);
			IComponentIdentifier comp = (IComponentIdentifier) fut.get(this);
			// add data consumer and provider
//			addDataConsumerAndProvider(comp, executionService, (SimulationConfiguration) ((Map) args.get(Constants.SIMULATION_FACTS_FOR_CLIENT)).get(Constants.SIMULATION_FACTS_FOR_CLIENT));

		} catch (Exception e) {
			// JOptionPane.showMessageDialog(SGUI.getWindowParent(StarterPanel.this),
			// "Could not start application: "+e,
			// "Application Problem", JOptionPane.INFORMATION_MESSAGE);
			System.out.println("Could not start application...." + e);
		}
	}

	/**
	 * TODO: differentiate between step and values!!!!
	 * 
	 * @param simConf
	 * @param args
	 */
	private void sweepParameters(SimulationConfiguration simConf, Map args) {
		Optimization opt = simConf.getOptimization();
		String parameterName = opt.getData().getName();
		int val = -1;

		// iterate through parameter space with step size
		if (opt.getParameterSweeping().getType().equalsIgnoreCase(Constants.OPTIMIZATION_TYPE_SPACE)) {
			if (opt.getParameterSweeping().getParameterSweepCounter() == 0) {
				val = opt.getParameterSweeping().getConfiguration().getStart();

			} else {
				int step = opt.getParameterSweeping().getConfiguration().getStep();
				int currentVal = opt.getParameterSweeping().getCurrentValue();
				val = currentVal + step;

			}
			// iterate through list of parameters
		} else if (opt.getParameterSweeping().getType().equalsIgnoreCase(Constants.OPTIMIZATION_TYPE_LIST)) {
			if (opt.getParameterSweeping().getParameterSweepCounter() == 0) {
				val = Integer.valueOf(opt.getParameterSweeping().getConfiguration().getValuesAsList().get(0));

			} else {
				val = Integer.valueOf(opt.getParameterSweeping().getConfiguration().getValuesAsList().get(opt.getParameterSweeping().getParameterSweepCounter()));

			}
		} else {
			System.err.println("#StartSimulationExperiment# Error on identifying type for sweeping parameter(s): " + opt.getParameterSweeping().getType());
		}

		// update SimulationConf to be up to date
		opt.getParameterSweeping().setCurrentValue(val);
		opt.getParameterSweeping().incrementParameterSweepCounter();

		// parametrize application-xml
		args.put(parameterName, new Integer(val));
	}
}
