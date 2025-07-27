package org.myjtools.openbbt.core.adapters;


import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



public class UnaryStringAssertion {

    public static final String NULL = "assertion.generic.null";
    public static final String EMPTY = "assertion.generic.empty";
    public static final String NULL_EMPTY = "assertion.generic.null.empty";

    public static final String NOT_NULL = "assertion.generic.not.null";
    public static final String NOT_EMPTY = "assertion.generic.not.empty";
    public static final String NOT_NULL_EMPTY = "assertion.generic.not.null.empty";

    private static final Map<String, Supplier<Matcher<?>>> matchers = new LinkedHashMap<>();

    static {
        matchers.put(NULL, Matchers::nullValue);
        matchers.put(EMPTY, () -> Matchers.anyOf(
                matcher(Matchers.emptyString()),
                matcherCollection(Matchers.empty())));
        matchers.put(NULL_EMPTY, () -> Matchers.anyOf(
                matcher(Matchers.emptyOrNullString()),
                matcherCollection(Matchers.empty())));
        matchers.put(NOT_NULL, Matchers::notNullValue);
        matchers.put(NOT_EMPTY, () -> Matchers.not(matchers.get(EMPTY)));
        matchers.put(NOT_NULL_EMPTY, () -> Matchers.not(matchers.get(NULL_EMPTY)));
    }




    @SuppressWarnings("unchecked")
    private static <T> Matcher<? super Object> matcher(Matcher<? super T> matcher) {
        return (Matcher<? super Object>) Matchers.allOf(Matchers.instanceOf(String.class), matcher);
    }


    @SuppressWarnings("unchecked")
    private static Matcher<? super Object> matcherCollection(Matcher<? super Collection<Object>> matcher) {
        return (Matcher<Object>) Matchers.allOf(Matchers.instanceOf(Collection.class), matcher);
    }

}