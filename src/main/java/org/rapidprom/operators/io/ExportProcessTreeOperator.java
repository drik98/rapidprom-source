package org.rapidprom.operators.io;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;


import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.ptml.exporting.PtmlExportTree;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.ProcessTreeIOObject;
import org.rapidprom.operators.abstr.AbstractRapidProMExporterOperator;
import org.rapidprom.operators.io.ProcessTreeFileFormat;

import com.rapidminer.operator.OperatorDescription;


public class ExportProcessTreeOperator
        extends AbstractRapidProMExporterOperator<ProcessTreeIOObject, ProcessTree, ProcessTreeFileFormat>{
	
	public ExportProcessTreeOperator(OperatorDescription description) {
		super(description, ProcessTreeIOObject.class,
				EnumSet.allOf(ProcessTreeFileFormat.class)
						.toArray(new ProcessTreeFileFormat[EnumSet.allOf(ProcessTreeFileFormat.class).size()]),
						ProcessTreeFileFormat.PTML);
	}
	
	@Override
	protected void writeToFile(File file, ProcessTree object, ProcessTreeFileFormat format) throws IOException {
		switch (format) {
		case PTML:
		default:
			PtmlExportTree exporterPTML = new PtmlExportTree();
			exporterPTML.exportDefault(RapidProMGlobalContext.instance().getPluginContext(), object, file);
			break;
		}
	}
	

}
