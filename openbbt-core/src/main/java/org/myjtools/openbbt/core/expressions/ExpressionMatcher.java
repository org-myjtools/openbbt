package org.myjtools.openbbt.core.expressions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public Match matches(String value) {
        String remaining = value;
        boolean matching = false;

        Map<String,ArgumentValue> arguments = new HashMap<>();
        for (FragmentMatcher fragmentMatcher : fragments) {
            var result = fragmentMatcher.matches(remaining);
            if (result.startMatched()) {
                matching = true;
                remaining = remaining.substring(result.consumed());
                if (result.argument() != null) {
                    arguments.put(result.argument().name(), result.argument());
                }
            } else {
                return new Match(false);
            }
        }
        return new Match(true, arguments);
    }




}
