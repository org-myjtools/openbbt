package org.myjtools.openbbt.core.datatypes;

import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.util.Patterns;

import java.util.function.Function;
import java.util.regex.*;



public abstract class DataTypeAdapter<T> implements DataType {


    private final String name;
    private final Class<T> javaType;
    private final Pattern pattern;
    private final String hint;
    private final Function<String,T> parser;


    protected DataTypeAdapter(
        String name,
        Class<T> javaType,
        String regex,
        String hint,
        Function<String,T> parser
    ) {
        this.name = name;
        this.javaType = javaType;
        this.pattern = Patterns.of(regex);
        this.hint = hint;
        this.parser = parser;
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<?> javaType() {
        return javaType;
    }

    @Override
    public String hint() {
        return hint;
    }

    @Override
    public Pattern pattern() {
        return pattern;
    }

    @Override
    public T parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = matcher(value);
        if (!matcher.matches()) {
            throw new OpenBBTException("Invalid value for data type {}: {}\nExpected pattern: {}",name,value,hint);
        }
        return parser.apply(value);
    }
}
