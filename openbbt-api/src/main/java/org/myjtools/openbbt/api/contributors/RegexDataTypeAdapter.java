package org.myjtools.openbbt.api.contributors;


import org.myjtools.openbbt.api.DataType;
import org.myjtools.openbbt.api.OpenBBTException;
import org.myjtools.openbbt.api.util.Patterns;
import org.myjtools.openbbt.api.util.ThrowableFunction;

import java.util.*;
import java.util.regex.Pattern;


public class RegexDataTypeAdapter<T> implements DataType {

    private final String name;
    private final String regex;
    private final Class<T> javaType;
    private final ThrowableFunction<String,T> parser;
    private final List<String> hints;


    public RegexDataTypeAdapter(
        String name,
        String regex,
        Class<T> javaType,
        ThrowableFunction<String, T> parser,
        List<String> hints
    ) {
        this.name = name;
        this.regex = regex;
        this.javaType = javaType;
        this.parser = parser;
        this.hints = hints;
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
    public String regex(Locale locale) {
        return regex;
    }


    @Override
    public Pattern pattern(Locale locale) {
        return Patterns.of(regex(locale));
    }


    @Override
    public List<String> hints(Locale locale) {
        return hints;
    }


    @Override
    public T parse(Locale locale, String value) {
        try {
            return parser.apply(value);
        } catch (Exception e) {
            throw new OpenBBTException(e);
        }
    }



}
