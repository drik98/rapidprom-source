package org.rapidprom.operators.ba;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;
import java.util.List;

/**
 *
 * @author Hendrik
 */
public class MyOwnOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts()
            .createPort("example set", ExampleSet.class);
    private OutputPort exampleSetOutput = getOutputPorts()
            .createPort("example set");
    
    public static final String PARAMETER_TEXT = "log text";

    public MyOwnOperator(OperatorDescription descrption) {
        super(descrption);

    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeString(
                PARAMETER_TEXT,
                "This parameter defines which text is logged to the console when this operator is executed.",
                "This is a default text",
                false));
        return types;
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
        // get attributes from example set
        Attributes attributes = exampleSet.getAttributes();
// create a new attribute
        String newName = "newAttribute";
// define the name and the type of the new attribute
// valid types are
// - nominal (sub types: binominal, polynominal, string, file_path)
// - date_time (sub types: date, time)
// - numerical (sub types: integer, real)
        Attribute targetAttribute = AttributeFactory
                .createAttribute(newName, Ontology.REAL);

        targetAttribute.setTableIndex(attributes.size());
        exampleSet.getExampleTable().addAttribute(targetAttribute);
        attributes.addRegular(targetAttribute);
        for (Example example : exampleSet) {
            example.setValue(targetAttribute, Math.round(Math.random() * 10 + 0.5));
        }

        exampleSetOutput.deliver(exampleSet);

    }
}
