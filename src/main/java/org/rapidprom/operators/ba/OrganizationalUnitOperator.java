package org.rapidprom.operators.ba;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.rapidprom.ba.Prozess;
import org.rapidprom.ba.XLogUtil;
import org.rapidprom.ioobjects.XLogIOObject;

/**
 * This operator is used to add the organizational unit to the events in the
 * log. It identifies all unique activity names and makes you enter the
 * organizational unit that executes the activity
 * 
 * @author Hendrik
 */
public class OrganizationalUnitOperator extends Operator {

	public static final String PARAMETER_ORGANIZATIONALUNIT_KEY = "organizational unit key";
	public static final String PARAMETER_ORGANIZATIONALUNIT_DEFAULT = Prozess.ORGANIZATIONALUNIT_KEY;
	public static final String PARAMETER_ORGANIZATIONALUNIT_DESC = "This key will be used when searching in events for the attribute that contains information about the organizational unit exceuting the activity.";

	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);

	private OutputPort outputXLog = getOutputPorts().createPort("event log (ProM Event Log)");

	public OrganizationalUnitOperator(OperatorDescription descrption) {
		super(descrption);
		getTransformer().addRule(new GenerateNewMDRule(outputXLog, XLogIOObject.class));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_ORGANIZATIONALUNIT_KEY, PARAMETER_ORGANIZATIONALUNIT_DESC,
				PARAMETER_ORGANIZATIONALUNIT_DEFAULT, false));

		return types;
	}

	@Override
	public void doWork() throws OperatorException {

		Logger logger = LogService.getRoot();

		XLogIOObject input = ((XLogIOObject) inputXLog.getData(XLogIOObject.class));
		XLog eventLog = input.getArtifact();

		final String ORGANIZATIONALUNIT_KEY = getParameter(PARAMETER_ORGANIZATIONALUNIT_KEY);
		XLogUtil.createEventLogAttribute(logger, eventLog, ORGANIZATIONALUNIT_KEY, Prozess.ORGANIZATIONALUNIT_KEY,
				"the organizational unit.", XLogUtil.XType.LITERAL);

		MetaData md = inputXLog.getMetaData();

		XEventClassifier classifier = eventLog.getClassifiers().get(0);
		String classifierKey = classifier.name();
		List<XEvent> xevents = eventLog.stream().flatMap(List::stream).collect(Collectors.toList());
		Set<String> uniqueActivities = xevents.stream()
				.map((XEvent event) -> ((XAttributeLiteralImpl) event.getAttributes().get(classifierKey)).getValue())
				.collect(Collectors.toSet());
		Map<String, XAttribute> activityToAbteilung = new HashMap<String, XAttribute>();
		for (String uniqueActivity : uniqueActivities) {
			Boolean organizationalUnitAlreadyExists = xevents.stream().anyMatch((XEvent evt) -> {
				return uniqueActivity
						.equals(((XAttributeLiteralImpl) evt.getAttributes().get(classifierKey)).getValue())
						&& evt.getAttributes().get(ORGANIZATIONALUNIT_KEY) != null
						&& !evt.getAttributes().get(ORGANIZATIONALUNIT_KEY).toString().isEmpty();
			});
			if (!organizationalUnitAlreadyExists) {
				String abteilung = JOptionPane.showInputDialog(
						"Please enter the organizational unit for activity \"" + uniqueActivity + "\".");
				XAttributeLiteralImpl attribLit = new XAttributeLiteralImpl(ORGANIZATIONALUNIT_KEY, abteilung);
				activityToAbteilung.put(uniqueActivity, attribLit);
			} else {
				logger.log(Level.INFO, "Organizational unit from log was used for activity \"" + uniqueActivity + "\"");
			}

		}

		xevents.stream().filter((XEvent event) -> {
			return event.getAttributes().get(ORGANIZATIONALUNIT_KEY) == null;
		}).forEach((XEvent event) -> {
			event.getAttributes().put(ORGANIZATIONALUNIT_KEY,
					activityToAbteilung.get(event.getAttributes().get(classifierKey).toString()));
		});

		XLogIOObject xLogIOObject = new XLogIOObject(eventLog, input.getPluginContext());

		outputXLog.deliverMD(md);
		outputXLog.deliver(xLogIOObject);

	}
}
