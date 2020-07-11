package org.rapidprom.operators.ba;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.rapidprom.ba.Prozess;
import org.rapidprom.ba.XLogUtil;
import org.rapidprom.ioobjects.XLogIOObject;

/**
 * This operator calculates the processing time and transition time for all
 * activites based on the ending timestamp
 * 
 * @author Hendrik
 */
public class ProcessingTimeOperator extends Operator {

	public static final String PARAMETER_END_TIME_KEY = "end timestamp key";
	public static final String PARAMETER_END_TIME_DEFAULT = Prozess.END_TIMESTAMP_KEY;
	public static final String PARAMETER_END_TIME_DESC = "This key will be used when searching in events for the attribute that contains information about the end timestamp of the activity";

	public static final String PARAMETER_PROCESSING_TIME_KEY = "processing time key";
	public static final String PARAMETER_PROCESSING_TIME_DEFAULT = Prozess.PROCESSING_TIME_KEY;
	public static final String PARAMETER_PROCESSING_TIME_DESC = "This key will be used when searching in events for the attribute that contains information about the processing time of the activity";

	public static final String PARAMETER_TRANSITION_TIME_KEY = "transition time key";
	public static final String PARAMETER_TRANSITION_TIME_DEFAULT = Prozess.TRANSITION_TIME_KEY;
	public static final String PARAMETER_TRANSITION_TIME_DESC = "This key will be used when searching in events for the attribute that contains information about the transition time between this and the previous activity";

	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);

	private OutputPort outputXLog = getOutputPorts().createPort("event log (ProM Event Log)");

	public ProcessingTimeOperator(OperatorDescription descrption) {
		super(descrption);
		getTransformer().addRule(new GenerateNewMDRule(outputXLog, XLogIOObject.class));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_END_TIME_KEY, PARAMETER_END_TIME_DESC, PARAMETER_END_TIME_DEFAULT,
				false));

		types.add(new ParameterTypeString(PARAMETER_PROCESSING_TIME_KEY, PARAMETER_PROCESSING_TIME_DESC,
				PARAMETER_PROCESSING_TIME_DEFAULT, false));

		types.add(new ParameterTypeString(PARAMETER_TRANSITION_TIME_KEY, PARAMETER_TRANSITION_TIME_DESC,
				PARAMETER_TRANSITION_TIME_DEFAULT, false));

		return types;
	}

	@Override
	public void doWork() throws OperatorException {

		Logger logger = LogService.getRoot();

		MetaData md = inputXLog.getMetaData();

		XLogIOObject input = ((XLogIOObject) inputXLog.getData(XLogIOObject.class));
		XLog eventLog = input.getArtifact();

		final String START_KEY = Prozess.START_TIMESTAMP_KEY;
		final String ENDE_KEY = getParameter(PARAMETER_END_TIME_KEY);
		final String PROCESSING_TIME_KEY = getParameter(PARAMETER_PROCESSING_TIME_KEY);
		final String TRANSITION_TIME_KEY = getParameter(PARAMETER_TRANSITION_TIME_KEY);

		XLogUtil.createEventLogAttribute(logger, eventLog, ENDE_KEY, Prozess.END_TIMESTAMP_KEY,
				"the timestamp of the end.", XLogUtil.XType.DATE);
		XLogUtil.createEventLogAttribute(logger, eventLog, PROCESSING_TIME_KEY, Prozess.PROCESSING_TIME_KEY,
				"processing time.", XLogUtil.XType.DISCRETE);
		XLogUtil.createEventLogAttribute(logger, eventLog, TRANSITION_TIME_KEY, Prozess.TRANSITION_TIME_KEY,
				"transition time between an event and the previous one.", XLogUtil.XType.DISCRETE);

		for (XTrace trace : eventLog) {
			for (int i = 0; i < trace.size(); i++) {
				XEvent event = trace.get(i);
				XAttributeTimestampImpl startXAttribute = (XAttributeTimestampImpl) event.getAttributes()
						.get(START_KEY);
				XAttributeTimestampImpl endeXAttribute = (XAttributeTimestampImpl) event.getAttributes().get(ENDE_KEY);
				XAttributeDiscreteImpl zeitXAttribute = (XAttributeDiscreteImpl) event.getAttributes()
						.get(PROCESSING_TIME_KEY);
				XAttributeDiscreteImpl uebergangXAttribute = (XAttributeDiscreteImpl) event.getAttributes()
						.get(TRANSITION_TIME_KEY);

				if (startXAttribute == null) {
					continue;
				}

				// calculate end-timestamp by duration or duration by end-timestamp
				if (endeXAttribute == null && zeitXAttribute != null) {
					endeXAttribute = new XAttributeTimestampImpl(ENDE_KEY,
							startXAttribute.getValueMillis() + zeitXAttribute.getValue());
					event.getAttributes().put(ENDE_KEY, endeXAttribute);

				} else if (endeXAttribute != null && zeitXAttribute == null) {
					zeitXAttribute = new XAttributeDiscreteImpl(PROCESSING_TIME_KEY,
							endeXAttribute.getValueMillis() - startXAttribute.getValueMillis());
					event.getAttributes().put(PROCESSING_TIME_KEY, zeitXAttribute);

				} else {
					zeitXAttribute = new XAttributeDiscreteImpl(PROCESSING_TIME_KEY, 0);
					event.getAttributes().put(PROCESSING_TIME_KEY, zeitXAttribute);
					// AUCH NOCH ENDE HINZUFUEGEN TODO
					endeXAttribute = new XAttributeTimestampImpl(ENDE_KEY,
							startXAttribute.getValueMillis() + zeitXAttribute.getValue());
					event.getAttributes().put(ENDE_KEY, endeXAttribute);
				}

				// calculate duration between steps
				if (i == 0 || uebergangXAttribute != null) {
					continue;
				}

				// get the previous event from trace and check if it has a an ending time set
				XEvent previousEvent = trace.get(i - 1);
				XAttributeTimestampImpl previousEndXAttribute = (XAttributeTimestampImpl) previousEvent.getAttributes()
						.get(ENDE_KEY);
				if (previousEndXAttribute == null) {
					continue;
				}

				// get differnce from start of current and ending of previous acitivity in
				// miliseconds
				uebergangXAttribute = new XAttributeDiscreteImpl(TRANSITION_TIME_KEY,
						startXAttribute.getValueMillis() - previousEndXAttribute.getValueMillis());
				event.getAttributes().put(TRANSITION_TIME_KEY, uebergangXAttribute);

			}
		}

		XLogIOObject xLogIOObject = new XLogIOObject(eventLog, input.getPluginContext());

		outputXLog.deliverMD(md);
		outputXLog.deliver(xLogIOObject);

	}
}
