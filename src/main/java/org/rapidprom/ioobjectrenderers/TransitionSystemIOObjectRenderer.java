package org.rapidprom.ioobjectrenderers;

import javax.swing.JComponent;

import org.processmining.plugins.transitionsystem.MinedTSVisualization;
import org.processmining.plugins.transitionsystem.Visualization;
import org.rapidprom.ioobjectrenderers.abstr.AbstractRapidProMIOObjectRenderer;
import org.rapidprom.ioobjects.TransitionSystemIOObject;

public class TransitionSystemIOObjectRenderer extends
		AbstractRapidProMIOObjectRenderer<TransitionSystemIOObject> {

	@Override
	public String getName() {
		return "TransitionSystemRenderer";
	}

	@Override
	protected JComponent runVisualization(TransitionSystemIOObject artifact) {
		
//		Visualization visualizator = new Visualization();
		MinedTSVisualization visualizator = new MinedTSVisualization();		
		return visualizator.visualize(artifact.getPluginContext(),
				artifact.getArtifact());
	}

}