package org.rapidprom.ioobjects;

import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.framework.plugin.PluginContext;
import org.rapidprom.ioobjects.abstr.AbstractRapidProMIOObject;

public class XEventClassifierIOObject extends AbstractRapidProMIOObject<XEventClassifier> {

	private static final long serialVersionUID = 1L;

	public XEventClassifierIOObject(XEventClassifier t, PluginContext context) {
		super(t, context);
	}

}
