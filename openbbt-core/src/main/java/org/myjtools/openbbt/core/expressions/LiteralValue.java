package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.datatypes.DataType;

public record LiteralValue(String name, String literal, DataType type) implements ArgumentValue {

    public Object value() {
        return type.parse(literal);
    }

}
