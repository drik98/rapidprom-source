package org.rapidprom.operators.conformance;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.DeclareIOObject;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.operators.abstr.AbstractRapidProMEventLogBasedOperator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

import minerful.checking.integration.prom.ModelFitnessEvaluatorOpenXesInterface;
import minerful.checking.relevance.dao.ModelFitnessEvaluation;
import minerful.logparser.LogEventClassifier.ClassificationType;

public class BooleanDeclareConformanceCheckerOperator extends AbstractRapidProMEventLogBasedOperator {

	private static final String NAMECOL = "case id", VALUECOL = "fits model?";

	private InputPort inputDeclareModel = getInputPorts().createPort("model (Declare)", DeclareIOObject.class);

	private OutputPort output = getOutputPorts().createPort("example set (Data Table)");
	private OutputPort outputLog = getOutputPorts().createPort("aligned event log (ProM XLog)");

	private ExampleSetMetaData metaData = null;

	public BooleanDeclareConformanceCheckerOperator(OperatorDescription description) {
		super(description);

		this.metaData = new ExampleSetMetaData();
		AttributeMetaData amd1 = new AttributeMetaData(NAMECOL, Ontology.STRING);
		amd1.setRole(AttributeColumn.REGULAR);
		amd1.setNumberOfMissingValues(new MDInteger(0));
		metaData.addAttribute(amd1);
		AttributeMetaData amd2 = new AttributeMetaData(VALUECOL, Ontology.NUMERICAL);
		amd2.setRole(AttributeColumn.REGULAR);
		amd2.setNumberOfMissingValues(new MDInteger(0));
		metaData.addAttribute(amd2);
		metaData.setNumberOfExamples(1);
		getTransformer().addRule(new GenerateNewMDRule(output, this.metaData));
		getTransformer().addRule(new GenerateNewMDRule(outputLog, XLogIOObject.class));
	}

	@Override
	public void doWork() throws OperatorException {
		final Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: replay log on declare model for boolean conformance checking");
		long time = System.currentTimeMillis();

		XLog log = getXLog();
		DeclareIOObject model = inputDeclareModel.getData(DeclareIOObject.class);
		
		ModelFitnessEvaluatorOpenXesInterface xEvalor = new ModelFitnessEvaluatorOpenXesInterface(
				log,
				ClassificationType.NAME,
				model.getArtifact());

		ExampleSet es = null;
		MemoryExampleTable table = null;
		List<Attribute> attributes = new LinkedList<Attribute>();
		attributes.add(AttributeFactory.createAttribute(NAMECOL, Ontology.STRING));
		attributes
				.add(AttributeFactory.createAttribute(VALUECOL, Ontology.NUMERICAL));
		table = new MemoryExampleTable(attributes);

		Attribute[] attribArray = new Attribute[attributes.size()];
		for (int i = 0; i < attributes.size(); i++) {
			attribArray[i] = attributes.get(i);
		}

		DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.');
		Object[] vals = new Object[2];
		
		ModelFitnessEvaluation xEvalon;

		for (XTrace t : log) {

			vals[0] = XConceptExtension.instance().extractName(t);

			xEvalon = xEvalor.evaluateOnTrace(t);
			
			if (xEvalon.isFullyFitting())
				vals[1] = 1;
			else
				vals[1] = 0;
			DataRow dataRow = factory.create(vals, attribArray);
			table.addDataRow(dataRow);
		}
		es = table.createExampleSet();

		output.deliver(es);

			outputLog.deliver(new XLogIOObject(log, RapidProMGlobalContext.instance().getPluginContext()));

		logger.log(Level.INFO, "End: replay log on declare model for boolean conformance checking ("
				+ (System.currentTimeMillis() - time) / 1000 + " sec)");

	}

}
