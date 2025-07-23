package org.myjtools.openbbt.api.plan;


import java.util.function.UnaryOperator;

public sealed interface NodeArgument permits DataTable, Document {

    NodeArgument copy(UnaryOperator<String> replacingVariablesMethod);

}
