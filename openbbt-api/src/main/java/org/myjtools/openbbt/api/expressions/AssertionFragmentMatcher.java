package org.myjtools.openbbt.api.expressions;

import org.myjtools.openbbt.api.Assertion;

import java.util.Locale;

public class AssertionFragmentMatcher implements FragmentMatcher {

    private final Assertion assertion;

    public AssertionFragmentMatcher(Assertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public boolean matches(String input, Locale locale) {
        return true;
    }

    @Override
    public String toString() {
        return "Assertion["+assertion.name()+"]";
    }
}
