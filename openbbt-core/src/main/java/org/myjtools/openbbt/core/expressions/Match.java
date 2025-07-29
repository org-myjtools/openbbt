package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.Assertion;

import java.util.Map;

public record Match (boolean matched, Map<String,ArgumentValue> argument) {

    public Match(boolean matched) {
        this(matched, Map.of());
    }

    public ArgumentValue argument(String name) {
        return argument.get(name);
    }


    public Assertion assertion(String name) {
        return null;
    }
}
