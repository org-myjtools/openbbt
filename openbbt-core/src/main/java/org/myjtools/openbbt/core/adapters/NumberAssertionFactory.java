package org.myjtools.openbbt.core.adapters;

import org.hamcrest.Matchers;
import org.myjtools.openbbt.core.contributors.MessageProvider;
import org.myjtools.openbbt.core.util.Patterns;

import java.sql.Array;
import java.util.*;
import java.util.regex.Pattern;

public class NumberAssertionFactory extends AssertionFactory {


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


    private final Messages messages;
    private final Map<Locale, List<Pattern>> patternsByKey = new HashMap<>();


    public NumberAssertionFactory(String name, Messages messages) {
        super(name);
        this.messages = messages;
    }


    @Override
    public List<Pattern> patterns(Locale locale) {
        return patternsByKey.computeIfAbsent(locale, this::createPatternsForLocale);
    }


    private ArrayList<Pattern> createPatternsForLocale(Locale locale) {
        var localeMessages = messages.forLocale(locale);
        var patterns = new ArrayList<Pattern>();
        for (String assertion : List.of(
            ASSERTION_NUMBER_EQUALS,
            ASSERTION_NUMBER_GREATER,
            ASSERTION_NUMBER_LESS,
            ASSERTION_NUMBER_GREATER_EQUALS,
            ASSERTION_NUMBER_LESS_EQUALS,
            ASSERTION_NUMBER_NOT_EQUALS,
            ASSERTION_NUMBER_NOT_GREATER,
            ASSERTION_NUMBER_NOT_LESS,
            ASSERTION_NUMBER_NOT_GREATER_EQUALS,
            ASSERTION_NUMBER_NOT_LESS_EQUALS
        )) {
            String expression = localeMessages.get(assertion);
            expression = "\\s*" + expression + "\\s*";
            expression = expression.replaceAll("\\)", ")?");
            expression = expression.replaceAll("_", "(\\\\d+)");
            patterns.add(Patterns.of(expression));
        }
        return patterns;
    }

}
