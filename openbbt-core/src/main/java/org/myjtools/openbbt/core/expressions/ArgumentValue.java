package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.DataType;

public sealed interface ArgumentValue permits VariableValue, LiteralValue {

    DataType type();
    String name();

}
