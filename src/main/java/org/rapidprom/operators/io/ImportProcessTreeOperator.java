package org.rapidprom.operators.io;

import java.io.File;
import java.util.List;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.ptml.importing.PtmlImportTree;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.ProcessTreeIOObject;
import org.rapidprom.operators.abstr.AbstractRapidProMImportOperator;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;

public class ImportProcessTreeOperator extends AbstractRapidProMImportOperator<ProcessTreeIOObject> {
	
	private final static String[] SUPPORTED_FILE_FORMATS = new String[] { "ptml" };
	
	public ImportProcessTreeOperator(OperatorDescription description) {
		super(description, ProcessTreeIOObject.class, SUPPORTED_FILE_FORMATS);
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_KEY_FILE, PARAMETER_DESC_FILE, false, SUPPORTED_FILE_FORMATS));	
		return types;
	}
	
	@Override
	protected ProcessTreeIOObject read(File file) throws Exception {
		PluginContext context = RapidProMGlobalContext.instance()
				.getFutureResultAwarePluginContext(PtmlImportTree.class);
		
		PtmlImportTree importer = new PtmlImportTree();
		ProcessTree pt = (ProcessTree) importer.importFile(context, getParameterAsFile(PARAMETER_KEY_FILE));		
		ProcessTreeIOObject ptResult = new ProcessTreeIOObject(pt, context);
		return ptResult;
	
	}
		

}
