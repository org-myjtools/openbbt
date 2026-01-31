package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.Assertion;

import java.util.*;

/**
 * Matches input strings against a compiled expression pattern.
 *
 * <p>An ExpressionMatcher is built by {@link ExpressionMatcherBuilder} and consists
 * of a sequence of {@link FragmentMatcher}s. When matching, each fragment is tested
 * in order, and the match succeeds only if all fragments match consecutively.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(dataTypes, assertions);
 * ExpressionMatcher matcher = builder.buildExpressionMatcher(
 *     "the user {name:text} has {count:number} items"
 * );
 *
 * Match match = matcher.matches("the user John has 5 items", Locale.ENGLISH);
 * if (match.matched()) {
 *     ArgumentValue name = match.argument("name");
 *     ArgumentValue count = match.argument("count");
 * }
 * }</pre>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionMatcherBuilder
 * @see Match
 * @see FragmentMatcher
 */
public class ExpressionMatcher {

    List<FragmentMatcher> fragments;
    Map<String,ArgumentFragmentMatcher> arguments;

    /**
     * Creates a new expression matcher with the given fragment matchers.
     *
     * @param fragments the list of fragment matchers
     */
    public ExpressionMatcher(List<FragmentMatcher> fragments) {
        this.fragments = List.copyOf(fragments);
        this.arguments = new HashMap<>();
        for (FragmentMatcher fragment : fragments) {
            if (fragment instanceof ArgumentFragmentMatcher argumentFragmentMatcher)  {
                arguments.put(argumentFragmentMatcher.name(), argumentFragmentMatcher);
            }
        }
    }

    /**
     * Returns the list of fragment matchers.
     *
     * @return immutable list of fragment matchers
     */
    public List<FragmentMatcher> fragments() {
        return fragments;
    }

    /**
     * Matches the input string against this expression pattern.
     *
     * @param value  the input string to match
     * @param locale the locale for assertion pattern matching
     * @return a {@link Match} result containing extracted arguments and assertions
     */
    public Match matches(String value, Locale locale) {
        String remaining = value;
        boolean matching = false;

        Map<String,ArgumentValue> arguments = new HashMap<>();
        Map<String, Assertion> assertions = new HashMap<>();

        for (FragmentMatcher fragmentMatcher : fragments) {
            var result = fragmentMatcher.matches(remaining, locale);
            if (result.startMatched()) {
                matching = true;
                remaining = remaining.substring(result.consumed());
                if (result.argument() != null) {
                    arguments.put(result.argument().name(), result.argument());
                }
                if (result.assertion() != null) {
                    assertions.put(result.assertion().name(),result.assertion());
                }
            } else {
                return new Match(false);
            }
        }
        return new Match(true, arguments, assertions);
    }




}
