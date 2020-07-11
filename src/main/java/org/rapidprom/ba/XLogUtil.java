package org.rapidprom.ba;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;

public class XLogUtil {

	public enum XType {
		DATE, LITERAL, DISCRETE
	}

	/**
	 * creates and log the used key for an attribute
	 * 
	 * @param logger
	 * @param eventLog
	 * @param userParameter
	 * @param internalParameter
	 */
	public static void createEventLogAttribute(Logger logger, XLog eventLog, String userParameter,
			String internalParameter, String label, XType type) {
		logger.log(Level.INFO,
				"Key \"" + userParameter + "\" will be used to identify the attributes containing " + label);

		XAttribute parameterXAttribute = new XAttributeLiteralImpl(internalParameter, userParameter);
		eventLog.getAttributes().put(internalParameter, parameterXAttribute);

		// check if global event attribute for end time stamp exists
		Optional<XAttribute> optionalAttribute = eventLog.getGlobalEventAttributes().stream()
				.filter((XAttribute attr) -> userParameter.equals(attr.getKey())).findFirst();

		// and change/create it

		if (optionalAttribute.isPresent()) {
			switch (type) {
			case DATE:
				XAttributeTimestampImpl globalAttributeTimeStamp = (XAttributeTimestampImpl) optionalAttribute.get();
				globalAttributeTimeStamp.setValueMillis(0L);
				break;
			case LITERAL:
				XAttributeLiteralImpl globalAttributeLiteral = (XAttributeLiteralImpl) optionalAttribute.get();
				globalAttributeLiteral.setValue("string");
				break;
			case DISCRETE:
				XAttributeDiscreteImpl globalAttributeDiscrete = (XAttributeDiscreteImpl) optionalAttribute.get();
				globalAttributeDiscrete.setValue(0L);
				break;
			}

		} else {
			switch (type) {
			case DATE:
				XAttributeTimestampImpl globalAttributeTimeStamp = new XAttributeTimestampImpl(userParameter, 0L);
				eventLog.getGlobalEventAttributes().add(globalAttributeTimeStamp);
				break;
			case LITERAL:
				XAttributeLiteralImpl globalAttributeLiteral = new XAttributeLiteralImpl(userParameter, "string");
				eventLog.getGlobalEventAttributes().add(globalAttributeLiteral);
				break;
			case DISCRETE:
				XAttributeDiscreteImpl globalAttributeDiscrete = new XAttributeDiscreteImpl(userParameter, 0L);
				eventLog.getGlobalEventAttributes().add(globalAttributeDiscrete);
				break;
			}

		}
	}

	/**
	 * extracts the user set key for a attribute in the events. if not set it uses
	 * the internal key
	 * 
	 * @param eventLog
	 * @param internalKey
	 * @return
	 */
	public static String getAttributeKey(XLog eventLog, String internalKey) {
		XAttributeLiteralImpl attr = (XAttributeLiteralImpl) eventLog.getAttributes().get(internalKey);
		if (attr != null) {
			return attr.getValue();
		} else {
			return internalKey;
		}
	}

}
