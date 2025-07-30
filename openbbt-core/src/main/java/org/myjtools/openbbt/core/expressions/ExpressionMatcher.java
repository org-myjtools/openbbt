package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.Assertion;

import java.util.*;
import java.util.stream.Collectors;

public class ExpressionMatcher {



    List<FragmentMatcher> fragments;
    Map<String,ArgumentFragmentMatcher> arguments;

    public ExpressionMatcher(List<FragmentMatcher> fragments) {
        this.fragments = List.copyOf(fragments);
        this.arguments = new HashMap<>();
        for (FragmentMatcher fragment : fragments) {
            if (fragment instanceof ArgumentFragmentMatcher argumentFragmentMatcher)  {
                arguments.put(argumentFragmentMatcher.name(), argumentFragmentMatcher);
            }
        }
    }


    public List<FragmentMatcher> fragments() {
        return fragments;
    }

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
