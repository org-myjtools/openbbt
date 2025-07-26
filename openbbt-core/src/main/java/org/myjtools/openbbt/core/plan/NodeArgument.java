package org.myjtools.openbbt.core.plan;


import java.util.function.UnaryOperator;

public sealed interface NodeArgument permits DataTable, Document {

    NodeArgument copy(UnaryOperator<String> replacingVariablesMethod);

}
