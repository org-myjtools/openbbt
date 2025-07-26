package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.Assertion;

public class AssertionFragmentMatcher implements FragmentMatcher {

    private final Assertion assertion;

    public AssertionFragmentMatcher(Assertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public MatchResult matches(String input) {
        return new MatchResult(true,0);
    }

    @Override
    public String toString() {
        return "Assertion["+assertion.name()+"]";
    }
}
