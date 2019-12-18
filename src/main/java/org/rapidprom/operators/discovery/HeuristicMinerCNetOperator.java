package org.rapidprom.operators.discovery;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.dataawarecnetminer.common.MinerContext;
import org.processmining.dataawarecnetminer.exception.MiningException;
import org.processmining.dataawarecnetminer.extension.bindings.BindingsNearestActivityImpl;
import org.processmining.dataawarecnetminer.extension.conditionaldependencies.ConditionalDependencyHeuristicConfig;
import org.processmining.dataawarecnetminer.mining.classic.HeuristicsCausalGraphBuilder.HeuristicsConfig;
import org.processmining.dataawarecnetminer.mining.classic.HeuristicsCausalGraphMiner;
import org.processmining.dataawarecnetminer.mining.classic.HeuristicsCausalNetMiner;
import org.processmining.dataawarecnetminer.model.DependencyAwareCausalGraph;
import org.processmining.dataawarecnetminer.model.EventRelationStorage;
import org.processmining.dataawarecnetminer.model.FrequencyAwareCausalNet;
import org.processmining.dataawarecnetminer.util.DataDiscoveryUtil;
import org.processmining.datadiscovery.RuleDiscoveryException;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.cnet.CausalNet;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.CausalNetIOObject;
import org.rapidprom.operators.abstr.AbstractRapidProMEventLogBasedOperator;
import org.rapidprom.operators.util.RapidProMProgress;

import com.google.common.collect.ImmutableSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * Heuristic Miner, a technique to discover Causal nets based on the
 * heuristics miner algorithm.
 * 
 * @author F. Mannhardt
 *
 */
public class HeuristicMinerCNetOperator extends AbstractRapidProMEventLogBasedOperator {

	public static final String PARAMETER_RELATIVE_TO_BEST = "Heuristic: Relative-to-best threshold",
			PARAMETER_RELATIVE_TO_BEST_DESCR = "Admissable distance between directly follows relations for an "
					+ "activity and the activity's best one. At 0 only the best directly follows "
					+ "relation will be shown for every activity, at 1 all will be shown.",
			PARAMETER_DEPENDENCY_THRESHOLD = "Heuristic: Dependency threshold",
			PARAMETER_DEPENDENCY_THRESHOLD_DESCR = "Strength of the directly follows relations determines when to "
					+ "Show arcs (based on how frequently one activity is followed by another).",
			PARAMETER_L1_THRESHOLD = "Heuristic: Length-one-loops threshold",
			PARAMETER_L1_THRESHOLD_DESCR = "Show arcs based on frequency of L1L observations",
			PARAMETER_L2_THRESHOLD = "Heuristic: Length-two-loops threshold",
			PARAMETER_L2_THRESHOLD_DESCR = "Show arcs based on frequency of L2L observations",
			PARAMETER_LONG_DISTANCE = "Heuristic: Long distance dependency",
			PARAMETER_LONG_DISTANCE_DESCR = "Consider eventually follows relations in the heuristic discovery.",
			PARAMETER_ALL_TASK_CONNECTED = "Heuristic: Connect all tasks",
			PARAMETER_ALL_TAKS_CONNECTED_DESCR = "Force every task to have at least one input and output arc, "
					+ "except one initial and one final activity.",
			PARAMETER_ACCEPTED_CONNECTED = "Heuristic: Connect accepted",
			PARAMETER_ACCEPTED_CONNECTED_DESCR = "Force every task that is included in the initial dependency graph to have at least one input and one output arc.",
			PARAMETER_OBSERVATION_THRESHOLD = "Heuristic: Frequency of observation",
			PARAMETER_OBSERVATION_THRESHOLD_DESCR = "", PARAMETER_BINDINGS_THRESHOLD = "Heuristic: Binding threshold",
			PARAMETER_BINDINGS_THRESHOLD_DESCR = "Strength of input and output binding to be considered for the heuristic discovery of C-net bindings.";
			//PARAMETER_CONDITION_THRESHOLD = "Data: Condition",
			//PARAMETER_CONDITION_THRESHOLD_DESCR = "Strength of the data conditions that are considered.";

	private static final String ATTRIBUTE_COLUMN = "attribute";

	//private InputPort inputAttributeSelection = getInputPorts().createPort("attribute selection (Example set)");

	private OutputPort outputCausalNet = getOutputPorts().createPort("model (ProM Causal Net)");

	public HeuristicMinerCNetOperator(OperatorDescription description) {
		super(description);
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		AttributeMetaData amd1 = new AttributeMetaData("attribute", Ontology.STRING);
		amd1.setRole(AttributeColumn.REGULAR);
		amd1.setNumberOfMissingValues(new MDInteger(0));
		metaData.addAttribute(amd1);
		//inputAttributeSelection.addPrecondition(new SimplePrecondition(inputAttributeSelection, metaData, false));
		getTransformer().addRule(new GenerateNewMDRule(outputCausalNet, CausalNetIOObject.class));
	}

	public void doWork() throws OperatorException {

		XLog log = getXLog();
		XEventClassifier classifier = getXEventClassifier();

		HeuristicsConfig heuristicConfig = getHeuristicsMinerConfig();

		Set<String> selectedAttributes = ImmutableSet.<String>of();

		MinerContext minerContext = createMinerContext();
		try {
			ConditionalDependencyHeuristicConfig graphConfig = getDataHeuristicMinerConfig();
			//DecisionTreeConfig discoveryConfig = getDataDiscoveryConfig();
			//graphConfig.setDataDiscoveryConfig(discoveryConfig);
			HeuristicsCausalNetMiner.CausalNetConfig netConfig = getCausalNetConfig();

			CausalNet causalNet;
			try {
				causalNet = mineCausalNet(log, classifier, selectedAttributes, graphConfig, netConfig, heuristicConfig,
						minerContext);
			} catch (RuleDiscoveryException | MiningException e) {
				throw new OperatorException("Failed discovering Causal Net: " + e.getMessage(), e);
			}

			CausalNetIOObject causalNetIOObject = new CausalNetIOObject(causalNet,
					RapidProMGlobalContext.instance().getPluginContext());
			outputCausalNet.deliver(causalNetIOObject);
		} finally {
			minerContext.getExecutor().shutdown();
		}
	}

	private MinerContext createMinerContext() {
		final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final Progress progress = new RapidProMProgress(getProgress());
		final PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();
		MinerContext minerContext = new MinerContext() {

			@Override
			public Progress getProgress() {
				return progress;
			}

			@Override
			public PluginContext getPluginContext() {
				return pluginContext;
			}

			@Override
			public ExecutorService getExecutor() {
				return executor;
			}
		};
		return minerContext;
	}

	private HeuristicsCausalNetMiner.CausalNetConfig getCausalNetConfig() throws UndefinedParameterError {
		HeuristicsCausalNetMiner.CausalNetConfig config = new HeuristicsCausalNetMiner.CausalNetConfig();
		config.setConsiderLongDistanceRelations(getParameterAsBoolean(PARAMETER_LONG_DISTANCE));
		config.setBindingsThreshold(getParameterAsDouble(PARAMETER_BINDINGS_THRESHOLD));
		return config;
	}

	private ConditionalDependencyHeuristicConfig getDataHeuristicMinerConfig() throws UndefinedParameterError {
		ConditionalDependencyHeuristicConfig config = new ConditionalDependencyHeuristicConfig();
		config.setAcceptedTasksConnected(getParameterAsBoolean(PARAMETER_ACCEPTED_CONNECTED));
		config.setAllTasksConnected(getParameterAsBoolean(PARAMETER_ALL_TASK_CONNECTED));
		config.setDependencyThreshold(getParameterAsDouble(PARAMETER_DEPENDENCY_THRESHOLD));
		config.setObservationThreshold(getParameterAsDouble(PARAMETER_OBSERVATION_THRESHOLD));
		config.setRelativeToBestThreshold(getParameterAsDouble(PARAMETER_RELATIVE_TO_BEST));
		//config.setGuardThreshold(getParameterAsDouble(PARAMETER_CONDITION_THRESHOLD));
		return config;
	}

	private HeuristicsConfig getHeuristicsMinerConfig() throws UndefinedParameterError {
		HeuristicsConfig config = new HeuristicsConfig();
		config.setAcceptedTasksConnected(getParameterAsBoolean(PARAMETER_ACCEPTED_CONNECTED));
		config.setAllTasksConnected(getParameterAsBoolean(PARAMETER_ALL_TASK_CONNECTED));
		config.setDependencyThreshold(getParameterAsDouble(PARAMETER_DEPENDENCY_THRESHOLD));
		config.setL1Threshold(getParameterAsDouble(PARAMETER_L1_THRESHOLD));
		config.setL2Threshold(getParameterAsDouble(PARAMETER_L2_THRESHOLD));
		config.setObservationThreshold(getParameterAsDouble(PARAMETER_OBSERVATION_THRESHOLD));
		config.setRelativeToBestThreshold(getParameterAsDouble(PARAMETER_RELATIVE_TO_BEST));
		return config;
	}

	protected CausalNet mineCausalNet(XLog log, XEventClassifier classifier, Set<String> selectedAttributes,
			ConditionalDependencyHeuristicConfig dataConfig, HeuristicsCausalNetMiner.CausalNetConfig netConfig,
			HeuristicsConfig heuristicConfig, MinerContext minerContext)
			throws RuleDiscoveryException, OperatorException, MiningException {

		Map<String, Class<?>> attributeTypes = DataDiscoveryUtil.getRelevantAttributeTypes(log);
		if (!attributeTypes.keySet().containsAll(selectedAttributes)) {
			selectedAttributes.removeAll(attributeTypes.keySet());
			throw new OperatorException("Invalid attributes: " + selectedAttributes);
		}

		EventRelationStorage eventRelations = EventRelationStorage.Factory.createEventRelations(log, classifier,
				minerContext.getExecutor());

		HeuristicsCausalGraphMiner miner = new HeuristicsCausalGraphMiner(eventRelations);
		miner.setHeuristicsConfig(heuristicConfig);
		DependencyAwareCausalGraph dependencyGraph = miner.mineCausalGraph();

		HeuristicsCausalNetMiner causalNetMiner = new HeuristicsCausalNetMiner(eventRelations);
		causalNetMiner.setConfig(netConfig);
		// TODO make bindings heuristic configurable
		FrequencyAwareCausalNet causalNet = causalNetMiner.mineCausalNet(minerContext, dependencyGraph,
				new BindingsNearestActivityImpl(eventRelations));

		return causalNet.getCNet();
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterTypeDouble parameter5 = new ParameterTypeDouble(PARAMETER_OBSERVATION_THRESHOLD,
				PARAMETER_OBSERVATION_THRESHOLD_DESCR, 0, 1, 0.1);
		parameterTypes.add(parameter5);

		ParameterTypeDouble parameter2 = new ParameterTypeDouble(PARAMETER_DEPENDENCY_THRESHOLD,
				PARAMETER_DEPENDENCY_THRESHOLD_DESCR, 0, 1, 0.9);
		parameterTypes.add(parameter2);

		ParameterTypeDouble parameterBind = new ParameterTypeDouble(PARAMETER_BINDINGS_THRESHOLD,
				PARAMETER_BINDINGS_THRESHOLD_DESCR, 0, 1, 0.1);
		parameterTypes.add(parameterBind);

		ParameterTypeDouble parameter3 = new ParameterTypeDouble(PARAMETER_L1_THRESHOLD, PARAMETER_L1_THRESHOLD_DESCR,
				0, 1, 0.9, true);
		parameterTypes.add(parameter3);

		ParameterTypeDouble parameter4 = new ParameterTypeDouble(PARAMETER_L2_THRESHOLD, PARAMETER_L2_THRESHOLD_DESCR,
				0, 1, 0.9, true);
		parameterTypes.add(parameter4);

		ParameterTypeDouble parameter1 = new ParameterTypeDouble(PARAMETER_RELATIVE_TO_BEST,
				PARAMETER_RELATIVE_TO_BEST_DESCR, 0, 1, 0.05, true);
		parameterTypes.add(parameter1);

		ParameterTypeBoolean parameter7 = new ParameterTypeBoolean(PARAMETER_LONG_DISTANCE,
				PARAMETER_LONG_DISTANCE_DESCR, false, true);
		parameterTypes.add(parameter7);

		ParameterTypeBoolean parameter6 = new ParameterTypeBoolean(PARAMETER_ALL_TASK_CONNECTED,
				PARAMETER_ALL_TAKS_CONNECTED_DESCR, false, true);
		parameterTypes.add(parameter6);

		ParameterTypeBoolean parameter8 = new ParameterTypeBoolean(PARAMETER_ACCEPTED_CONNECTED,
				PARAMETER_ACCEPTED_CONNECTED_DESCR, true);
		parameterTypes.add(parameter8);

		return parameterTypes;
	}

}