package org.myjtools.openbbt.core.plannode;


import java.util.function.UnaryOperator;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public sealed interface NodeArgument permits DataTable, Document {

	NodeArgument copy(UnaryOperator<String> replacingVariablesMethod);

}
