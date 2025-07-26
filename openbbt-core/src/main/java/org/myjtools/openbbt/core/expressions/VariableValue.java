package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.DataType;

public record VariableValue(String name, String variable, DataType type) implements ArgumentValue {
}
