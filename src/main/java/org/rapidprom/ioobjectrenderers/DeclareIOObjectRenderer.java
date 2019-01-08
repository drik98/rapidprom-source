package org.rapidprom.ioobjectrenderers;

import javax.swing.JComponent;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.rapidprom.ioobjectrenderers.abstr.AbstractRapidProMIOObjectRenderer;
import org.rapidprom.ioobjects.DeclareIOObject;
import org.rapidprom.ioobjects.DotIOObject;
import org.rapidprom.ioobjects.abstr.AbstractRapidProMIOObject;

import minerful.concept.ProcessModel;
import minerful.io.encdec.declaremap.DeclareMapEncoderDecoder;

public class DeclareIOObjectRenderer extends AbstractRapidProMIOObjectRenderer<DeclareIOObject> {

	@Override
	public String getName() {
		return "Declare Model renderer";
	}

	@Override
	protected JComponent runVisualization(DeclareIOObject artifact) {
		DeclareMap declareMap = new DeclareMapEncoderDecoder(artifact.getArtifact()).createDeclareMap();
		return declareMap.getView().getGraph();
	}

}
