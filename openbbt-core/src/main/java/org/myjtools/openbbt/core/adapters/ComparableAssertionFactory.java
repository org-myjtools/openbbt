package org.myjtools.openbbt.core.adapters;

import static org.hamcrest.Matchers.*;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.util.Patterns;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;

public class ComparableAssertionFactory<T extends Comparable<T>> implements AssertionFactory<T> {


    public static final String ASSERTION_NUMBER_EQUALS = "assertion.number.equals";
    public static final String ASSERTION_NUMBER_GREATER = "assertion.number.greater";
    public static final String ASSERTION_NUMBER_LESS = "assertion.number.less";
    public static final String ASSERTION_NUMBER_GREATER_EQUALS = "assertion.number.greater.equals";
    public static final String ASSERTION_NUMBER_LESS_EQUALS = "assertion.number.less.equals";
    public static final String ASSERTION_NUMBER_NOT_EQUALS = "assertion.number.not.equals";
    public static final String ASSERTION_NUMBER_NOT_GREATER = "assertion.number.not.greater";
    public static final String ASSERTION_NUMBER_NOT_LESS = "assertion.number.not.less";
    public static final String ASSERTION_NUMBER_NOT_GREATER_EQUALS = "assertion.number.not.greater.equals";
    public static final String ASSERTION_NUMBER_NOT_LESS_EQUALS = "assertion.number.not.less.equals";
    public static final String ASSERTION_GENERIC_NULL = "assertion.generic.null";
    public static final String ASSERTION_GENERIC_NOT_NULL = "assertion.generic.not.null";


    private final Map<String, Function<T, Assertion>> suppliers = new HashMap<>();



    private final Messages messages;
    private final Map<Locale, List<AssertionPattern<T>>> patternsByKey = new HashMap<>();

    private final String name;
    private final Function<String,T> parser;
    private final String replacement;


    public ComparableAssertionFactory(String name, Function<String,T> parser, String replacement, Messages messages) {
        this.name = name;
        this.parser = parser;
        this.messages = messages;
        this.replacement = replacement;
        fillSuppliers();
    }


    private void fillSuppliers() {
        suppliers.put(
                ASSERTION_NUMBER_EQUALS,
                it -> new AssertionAdapter(name, comparesEqualTo(it)));
        suppliers.put(
                ASSERTION_NUMBER_GREATER,
                it -> new AssertionAdapter(name, greaterThan(it))
        );
        suppliers.put(
                ASSERTION_NUMBER_LESS,
                it -> new AssertionAdapter(name, lessThan(it))
        );
        suppliers.put(
                ASSERTION_NUMBER_GREATER_EQUALS,
                it -> new AssertionAdapter(name, greaterThanOrEqualTo((it)))
        );
        suppliers.put(
                ASSERTION_NUMBER_LESS_EQUALS,
                it -> new AssertionAdapter(name, lessThanOrEqualTo((it)))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_EQUALS,
                it -> new AssertionAdapter(name, not(equalTo(it)))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_GREATER,
                it -> new AssertionAdapter(name, not(greaterThan((it))))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_LESS,
                it -> new AssertionAdapter(name, not(lessThan((it))))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_GREATER_EQUALS,
                it -> new AssertionAdapter(name, not(greaterThanOrEqualTo((it))))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_LESS_EQUALS,
                it -> new AssertionAdapter(name, not(lessThanOrEqualTo((it))))
        );
        suppliers.put(
                ASSERTION_GENERIC_NULL,
                it -> new AssertionAdapter(name, nullValue())
        );
        suppliers.put(
                ASSERTION_GENERIC_NOT_NULL,
                it -> new AssertionAdapter(name, not(nullValue()))
        );
    }



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
        for (String key : suppliers.keySet()) {
            String expression = localeMessages.get(key);
            expression = "\\s*" + expression + "\\s*";
            expression = expression.replaceAll("\\)", ")?");
            expression = expression.replaceAll("_", replacement);
            patterns.add(new AssertionPattern<>(key,Patterns.of(expression),suppliers.get(key)));
        }
        return patterns;
    }



}
