package org.myjtools.openbbt.api.expressions.parser;

import java.util.List;

public class ExpressionMatcher {


    List<FragmentMatcher> fragments;

    public ExpressionMatcher(List<FragmentMatcher> fragments) {
        this.fragments = List.copyOf(fragments);
    }

    public List<FragmentMatcher> fragments() {
        return fragments;
    }
}
