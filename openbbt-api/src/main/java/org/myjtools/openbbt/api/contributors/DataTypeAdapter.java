package org.myjtools.openbbt.api.contributors;

import org.myjtools.openbbt.api.DataType;
import org.myjtools.openbbt.api.OpenBBTException;
import org.myjtools.openbbt.api.util.Patterns;

import java.util.*;
import java.util.regex.*;



public abstract class DataTypeAdapter<T> implements DataType {


    protected DataTypeAdapter(String name, Class<T> javaType) {
        this.name = name;
        this.javaType = javaType;
    }


    protected DataTypeAdapter(
        String name,
        Class<T> javaType,
        LocaleRegexProvider regexProvider,
        LocaleHintProvider hintProvider,
        LocaleTypeParser<T> parserProvider
    ) {
        this.name = name;
        this.javaType = javaType;
        this.regexProvider = regexProvider;
        this.hintProvider = hintProvider;
        this.parserProvider = parserProvider;
    }


    public interface TypeParser<T> {
        T parse(String value);
    }

    public interface LocaleTypeParser<T> {
        TypeParser<T> parser(Locale locale);
    }

    public interface LocaleRegexProvider {
        String regex(Locale locale);
    }

    public interface LocaleHintProvider {
        List<String> hints(Locale locale);
    }

    private final String name;
    private final Class<T> javaType;

    private LocaleRegexProvider regexProvider;
    private LocaleHintProvider hintProvider;
    private LocaleTypeParser<T> parserProvider;

    private final Map<Locale, String> regexByLocale = new HashMap<>();
    private final Map<Locale, List<String>> hintsByLocale = new HashMap<>();
    private final Map<Locale, TypeParser<T>> parserByLocale = new HashMap<>();



    public DataTypeAdapter<T> regexProvider(LocaleRegexProvider regexProvider) {
        this.regexProvider = regexProvider;
        return this;
    }


    public DataTypeAdapter<T> hintProvider(LocaleHintProvider hintProvider) {
        this.hintProvider = hintProvider;
        return this;
    }


    public DataTypeAdapter<T> parserProvider(LocaleTypeParser<T> parserProvider) {
        this.parserProvider = parserProvider;
        return this;
    }


    @Override
    public T parse(Locale locale, String value) {
        try {
            return parserForLocale(locale).parse(value);
        } catch (final Exception e) {
            throw new OpenBBTException(
                e,
                "Error parsing type {} using language {}: '{}'\n\tExpected {}",
                name,
                locale,
                value,
                hints(locale)
            );
        }
    }



    protected TypeParser<T> parserForLocale(Locale locale) {
        return parserByLocale.computeIfAbsent(locale, parserProvider::parser);
    }


    @Override
    public String regex(Locale locale) {
        return regexByLocale.computeIfAbsent(locale, regexProvider::regex);
    }


    @Override
    public List<String> hints(Locale locale) {
        return hintsByLocale.computeIfAbsent(locale, hintProvider::hints);
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public Class<T> javaType() {
        return javaType;
    }

}
