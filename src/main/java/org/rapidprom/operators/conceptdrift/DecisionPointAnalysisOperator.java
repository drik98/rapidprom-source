package org.rapidprom.operators.conceptdrift;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.processcomparator.algorithms.Utils;
import org.processmining.variantfinder.parameters.Settings;
import org.processmining.variantfinder.parameters.TreeSettings;
import org.processmining.variantfinder.parameters.TsSettings;
import org.processmining.variantfinder.plugins.ConceptDriftFinder;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.XLogIOObject;

import com.google.common.collect.Lists;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

public class DecisionPointAnalysisOperator extends Operator {

	public static final String ALPHA_KEY = "alpha",
			ALPHA_DESCR = "alpha used for statistical testing: level of significance", LEAF_KEY = "min_leaf_percent",
			LEAF_DESCR = "The minimum percentage of instances (cases) that shoud land on a leaf, otherwise it will be pruned",
			CLASS_KEY = "class", CLASS_DESCR = "class attribute that defines which kind of drift will be analyzed",
			DEPENDENT_KEY = "timestamp", DEPENDENT_DESCR = "timestamp attribute";

	private InputPort input1 = getInputPorts().createPort("event log", XLogIOObject.class);
	private OutputPort output = getOutputPorts().createPort("drift points");

	public DecisionPointAnalysisOperator(OperatorDescription description) {
		super(description);

		ExampleSetMetaData md1 = new ExampleSetMetaData();
		AttributeMetaData amd1 = new AttributeMetaData("Drift ID", Ontology.NUMERICAL);
		amd1.setRole(AttributeColumn.REGULAR);
		amd1.setNumberOfMissingValues(new MDInteger(0));
		md1.addAttribute(amd1);
		AttributeMetaData amd4 = new AttributeMetaData("Time (ms)", Ontology.NUMERICAL);
		amd4.setRole(AttributeColumn.REGULAR);
		amd4.setNumberOfMissingValues(new MDInteger(0));
		md1.addAttribute(amd4);

		getTransformer().addRule(new GenerateNewMDRule(output, md1));
	}

	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: detecting concept drift");
		long time = System.currentTimeMillis();

		XLogIOObject log = input1.getData(XLogIOObject.class);

		PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();
		ConceptDriftFinder driftFinder = new ConceptDriftFinder();

		TreeSettings treeSettings = new TreeSettings(Lists.newArrayList(getParameterAsString(DEPENDENT_KEY)),
				getParameterAsString(CLASS_KEY), getParameterAsDouble(LEAF_KEY), false, getParameterAsDouble(ALPHA_KEY),
				400, 400);
		TsSettings tsSettings = new TsSettings(Utils.getTSSettings(pluginContext, log.getArtifact()), true, 0.01, false);
		Settings settings = new Settings(tsSettings, treeSettings);

		List<String> driftPoints = driftFinder.calculateDriftPoints(pluginContext, log.getArtifact(), settings);
		fillDriftPoints(driftPoints);

		logger.log(Level.INFO, "End: detecting concept drift (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterTypeString par0 = new ParameterTypeString(CLASS_KEY, CLASS_DESCR, "concept:nextActivity");
		ParameterTypeString par1 = new ParameterTypeString(DEPENDENT_KEY, DEPENDENT_DESCR, "time:timestamp");
		ParameterTypeDouble par2 = new ParameterTypeDouble(ALPHA_KEY, ALPHA_DESCR, 0, 1, 0.05);
		ParameterTypeDouble par3 = new ParameterTypeDouble(LEAF_KEY, LEAF_DESCR, 0, 1, 0.05);

		parameterTypes.add(par0);
		parameterTypes.add(par1);
		parameterTypes.add(par2);
		parameterTypes.add(par3);

		return parameterTypes;
	}

	@SuppressWarnings("deprecation")
	private void fillDriftPoints(List<String> drifts) {
		ExampleSet es = null;
		MemoryExampleTable table = null;
		List<Attribute> attributes = new LinkedList<Attribute>();
		attributes.add(AttributeFactory.createAttribute("Drift ID", Ontology.NUMERICAL));
		attributes.add(AttributeFactory.createAttribute("Time (ms)", Ontology.NUMERICAL));
		table = new MemoryExampleTable(attributes);

		if (drifts != null) {

			int driftID = 0;
			DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.');

			for (String point : drifts) {
				Object[] vals = new Object[2];
				vals[0] = driftID;
				vals[1] = Long.parseLong(point);

				DataRow dataRow = factory.create(vals, attributes.toArray(new Attribute[attributes.size()]));
				table.addDataRow(dataRow);
				driftID++;
			}

		}

		es = table.createExampleSet();
		output.deliver(es);
	}
}
