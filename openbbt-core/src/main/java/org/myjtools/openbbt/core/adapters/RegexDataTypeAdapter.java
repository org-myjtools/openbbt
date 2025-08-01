package org.myjtools.openbbt.core.adapters;


import org.myjtools.openbbt.core.DataType;
import org.myjtools.openbbt.core.util.Patterns;
import org.myjtools.openbbt.core.util.ThrowableFunction;

import java.util.regex.Pattern;


public class RegexDataTypeAdapter<T> implements DataType {

    private final String name;
    private final String regex;
    private final Class<T> javaType;
    private final ThrowableFunction<String,T> parser;
    private final String hint;


    public RegexDataTypeAdapter(
        String name,
        String regex,
        Class<T> javaType,
        ThrowableFunction<String, T> parser,
        String hint
    ) {
        this.name = name;
        this.regex = regex;
        this.javaType = javaType;
        this.parser = parser;
        this.hint = hint;
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public Class<T> javaType() {
        return javaType;
    }


    @Override
    public String hint() {
        return hint;
    }


    @Override
    public Pattern pattern() {
        return Patterns.of(regex);
    }


    @Override
    public Object parse(String value) {
        return parser.apply(value);
    }


    @Override
    public String toString() {
        return name+"["+javaType.getName()+"]";
    }
}
