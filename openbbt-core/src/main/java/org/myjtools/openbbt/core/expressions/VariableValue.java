package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.datatypes.DataType;

public record VariableValue(String name, String variable, DataType type) implements ArgumentValue {
}
