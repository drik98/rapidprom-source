package org.rapidprom.operators.discovery;


import java.util.List;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;

import org.processmining.framework.plugin.PluginContext;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.XEventClassifierIOObject;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.parameter.ParameterTypeXEventClassifierCategory;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;

public class ExtractionClassifierOperator extends Operator {

	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);
	private OutputPort xeventclassifieroutput = getOutputPorts().createPort("eventclassifier Outport");
	
	private static final String PARAMETER_KEY_EVENT_CLASSIFIER = "Event Classifier";
	private static final String PARAMETER_DESC_EVENT_CLASSIFIER = "Specifies how to identify events within the event log, as defined in http://www.xes-standard.org/";
	private static XEventClassifier[] PARAMETER_DEFAULT_CLASSIFIERS = new XEventClassifier[] {
			new XEventNameClassifier()};

	public ExtractionClassifierOperator(OperatorDescription description) {
		super(description);
	}

	public void doWork() throws OperatorException {
		PluginContext context = RapidProMGlobalContext.instance().getPluginContext();
		XLog log = getXLog();
		XEventClassifier classifier = getXEventClassifier();
		XEventClassifierIOObject xeventclassifierioobject = new XEventClassifierIOObject(classifier, context);
		
		xeventclassifieroutput.deliver(xeventclassifierioobject);
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> params = super.getParameterTypes();
		params.add(new ParameterTypeXEventClassifierCategory(PARAMETER_KEY_EVENT_CLASSIFIER,
				PARAMETER_DESC_EVENT_CLASSIFIER, new String[] { PARAMETER_DEFAULT_CLASSIFIERS[0].toString() },
				PARAMETER_DEFAULT_CLASSIFIERS, 0, false, inputXLog));
		return params;
	}

	protected XEventClassifier getXEventClassifier() throws UndefinedParameterError {
		ParameterTypeXEventClassifierCategory eClassParam = (ParameterTypeXEventClassifierCategory) getParameterType(
				PARAMETER_KEY_EVENT_CLASSIFIER);
		try {
			return eClassParam.valueOf(getParameterAsInt(PARAMETER_KEY_EVENT_CLASSIFIER));
		} catch (IndexOutOfBoundsException e) {
			throw new UndefinedParameterError("The index chosen is no longer available");
		}
	}
	
	protected XLog getXLog() throws UserError {
		return ((XLogIOObject) inputXLog.getData(XLogIOObject.class)).getArtifact();
	}
}