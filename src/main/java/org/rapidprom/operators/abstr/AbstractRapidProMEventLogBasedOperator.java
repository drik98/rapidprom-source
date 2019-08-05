package org.rapidprom.operators.abstr;

import java.util.List;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.classification.XEventResourceClassifier;
import org.deckfour.xes.model.XLog;
import org.rapidprom.ioobjects.XEventClassifierIOObject;
import org.rapidprom.ioobjects.XLogIOObject;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;

public class AbstractRapidProMEventLogBasedOperator extends Operator {

	private static final String PARAMETER_KEY_VARIATION = "Default Classifier",
			PARAMETER_DESCR__VARIATION ="Default Classifier allows one to choose the specfic default classifier one wants to use in the three classifier types: Event Name, Reource, Life Transition.";
	
	private static final String EVENT = "Event Name",
			RESOURCE = "Resource",
			LT = "Life Transition";
			
	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);
	private InputPort inputXClassifier = getInputPorts().createPort("classifier", XEventClassifierIOObject.class);

	private XEventClassifier[] DEFAULT_CLASSIFIERS = new XEventClassifier[]{new XEventNameClassifier(), new XEventResourceClassifier(), new XEventLifeTransClassifier()};
	


	public AbstractRapidProMEventLogBasedOperator(OperatorDescription description) {
		super(description);
		inputXClassifier.addPrecondition(new SimplePrecondition(inputXClassifier, new MetaData(XEventClassifierIOObject.class), false));
		// TODO: make the precondition give a more meaningful warning if the
		// metadata is null
		// inputXLog.addPrecondition(new
		// XLogContainsXEventClassifiersPreCondition(inputXLog));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();
		ParameterTypeCategory parameter = new ParameterTypeCategory(PARAMETER_KEY_VARIATION, PARAMETER_DESCR__VARIATION,
		        new String[] { EVENT, RESOURCE,LT }, 1);
		parameterTypes.add(parameter);
		return parameterTypes;
	}
	
	protected XLog getXLog() throws UserError {
		return ((XLogIOObject) inputXLog.getData(XLogIOObject.class)).getArtifact();
	}
	
	public XEventClassifier getXEventClassifier() {
		XEventClassifier cla= DEFAULT_CLASSIFIERS[0];
		try {	
		  if (inputXClassifier != null) {
				cla= inputXClassifier.getData(XEventClassifierIOObject.class).getArtifact();
		  }
		  else if ((inputXClassifier == null)&&(getParameterAsString(PARAMETER_KEY_VARIATION).equals(EVENT))){
			  cla= DEFAULT_CLASSIFIERS[0];
		}
		  else if ((inputXClassifier == null)&&(getParameterAsString(PARAMETER_KEY_VARIATION).equals(RESOURCE))) {
			  cla= DEFAULT_CLASSIFIERS[1];
		}
		  else if ((inputXClassifier == null)&&(getParameterAsString(PARAMETER_KEY_VARIATION).equals(LT))) {
			  cla= DEFAULT_CLASSIFIERS[2];
		}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return cla;
	}
	
}
