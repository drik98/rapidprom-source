package org.rapidprom.operators.discovery;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.model.XLog;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.DeclareIOObject;
import org.rapidprom.operators.abstr.AbstractRapidProMEventLogBasedOperator;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.tools.LogService;

import minerful.MinerFulMinerLauncher;
import minerful.concept.ProcessModel;
import minerful.miner.params.MinerFulCmdParameters;
import minerful.params.InputLogCmdParameters;
import minerful.params.InputLogCmdParameters.EventClassification;
import minerful.params.SystemCmdParameters;
import minerful.postprocessing.params.PostProcessingCmdParameters;
import minerful.postprocessing.params.PostProcessingCmdParameters.PostProcessingAnalysisType;

public class DeclareMinerOperator extends AbstractRapidProMEventLogBasedOperator {

	private static final String PARAMETER_SUPPORT_KEY = "Support Threshold",
			PARAMETER_SUPPORT_DESC = "For a sure total fit with the event log, this parameter should be set to 1.0",
			PARAMETER_INTEREST_KEY = "Interest Factor Threshold",
			PARAMETER_INTEREST_DESC = "The higher this is, the higher the frequency of occurrence of tasks involved in the returned constraints",
			PARAMETER_CONFIDENCE_KEY = "Confidence Threshold",
			PARAMETER_CONFIDENCE_DESC = " The higher this is, the higher the frequency of occurrence of tasks triggering the returned constraints";

	private OutputPort outputDeclareModel = getOutputPorts().createPort("model (Declare)");

	public DeclareMinerOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(outputDeclareModel, DeclareIOObject.class));
	}

	public void doWork() throws OperatorException {

		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: declare miner");
		long time = System.currentTimeMillis();

		InputLogCmdParameters inputLogParams = new InputLogCmdParameters();
		MinerFulCmdParameters minerFulParams = new MinerFulCmdParameters();
		SystemCmdParameters systemParams = new SystemCmdParameters();
		PostProcessingCmdParameters postParams = new PostProcessingCmdParameters();

		XLog myXLog = getXLog();
		inputLogParams.eventClassification = EventClassification.name;
		// // Use the one below if you want to classify events not just by their task
		// name!
		// inputLogParams.eventClassification = EventClassification.logspec;
		// postParams.supportThreshold = 0.95; // For a sure total fit with the event log, this parameter should be set to
											// 1.0
		postParams.supportThreshold = getParameterAsDouble(PARAMETER_SUPPORT_KEY);
		
		
		// postParams.confidenceThreshold = 0.66; // The higher this is, the higher the frequency of occurrence of tasks
												// triggering the returned constraints
		postParams.confidenceThreshold = getParameterAsDouble(PARAMETER_CONFIDENCE_KEY);
		
		// postParams.interestFactorThreshold = 0.5; // The higher this is, the higher the frequency of occurrence of tasks
													// involved in the returned constraints
		postParams.interestFactorThreshold = getParameterAsDouble(PARAMETER_INTEREST_KEY);

		// Remove redundant constraints. WARNING: this may take some time.
		// The language of the model remains completely unchanged. What changes is the
		// number of constraints in it.
		// postParams.postProcessingAnalysisType =
		// PostProcessingAnalysisType.HIERARCHYCONFLICTREDUNDANCYDOUBLE;
		// To leave the default post-processing, comment the line above. To completely
		// remove any form of post-processing, comment the line above and uncomment the
		// following one
		// postParams.postProcessingAnalysisType = PostProcessingAnalysisType.NONE;

		// Run the discovery algorithm
		System.out.println("Running the discovery algorithm...");

		MinerFulMinerLauncher miFuMiLa = new MinerFulMinerLauncher(inputLogParams, minerFulParams, postParams,
				systemParams);
		ProcessModel processModel = miFuMiLa.mine(myXLog);
		DeclareIOObject result = new DeclareIOObject(processModel,
				RapidProMGlobalContext.instance().getPluginContext());

		outputDeclareModel.deliver(result);
		logger.log(Level.INFO, "End: declare miner (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterTypeDouble parameter1 = new ParameterTypeDouble(PARAMETER_SUPPORT_KEY, PARAMETER_SUPPORT_DESC, 0, 1, 0.95);
		parameterTypes.add(parameter1);

		ParameterTypeDouble parameter2 = new ParameterTypeDouble(PARAMETER_CONFIDENCE_KEY, PARAMETER_CONFIDENCE_DESC, 0, 1, 0.66);
		parameterTypes.add(parameter2);

		ParameterTypeDouble parameter3 = new ParameterTypeDouble(PARAMETER_INTEREST_KEY, PARAMETER_INTEREST_DESC, 0, 1, 0.5);
		parameterTypes.add(parameter3);

		return parameterTypes;
	}

}
