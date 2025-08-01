package org.myjtools.openbbt.core.adapters;

import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.AssertionFactory;
import org.myjtools.openbbt.core.AssertionPattern;
import org.myjtools.openbbt.core.DataType;
import org.myjtools.openbbt.core.util.Patterns;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;

public abstract class AssertionFactoryAdapter<T> implements AssertionFactory<T> {

    protected final Map<String, Function<T, Assertion>> suppliers = new HashMap<>();
    protected final Messages messages;
    protected final Map<Locale, List<AssertionPattern<T>>> patternsByKey = new HashMap<>();
    protected final String name;
    protected final Function<String,T> parser;
    protected final DataType type;


    protected AssertionFactoryAdapter(String name, Function<String,T> parser, DataType type, Messages messages) {
        this.name = name;
        this.parser = parser;
        this.messages = messages;
        this.type = type;
        fillSuppliers();
    }


    protected abstract void fillSuppliers();



    @Override
    public String name() {
        return name;
    }

    @Override
    public List<AssertionPattern<T>> patterns(Locale locale) {
        return patternsByKey.computeIfAbsent(locale, this::createPatternsForLocale);
    }

    @Override
    public Assertion assertion(AssertionPattern<T> pattern, String input) {
        Matcher matcher = pattern.pattern().matcher(input);
        if (!matcher.find()) {
            return null;
        }
        try {
            String parameter = matcher.group("param");
            T parameterValue = (parameter == null ? null : parser.apply(parameter));
            return pattern.supplier().apply(parameterValue);
        } catch (IllegalArgumentException e) {
            return pattern.supplier().apply(null);
        }
    }



    private ArrayList<AssertionPattern<T>> createPatternsForLocale(Locale locale) {
        var localeMessages = messages.forLocale(locale);
        var patterns = new ArrayList<AssertionPattern<T>>();
        String parameterPattern = "(?<param>"+type.pattern().pattern()+")";
        for (String key : suppliers.keySet()) {
            String expression = localeMessages.get(key);
            expression = "\\s*" + expression + "\\s*"; // accept blanks before and after
            expression = expression.replaceAll("\\)", ")?"); // make () optionals
            expression = expression.replace("_", parameterPattern);
            patterns.add(new AssertionPattern<>(key,Patterns.of(expression),suppliers.get(key)));
        }
        return patterns;
    }



}
