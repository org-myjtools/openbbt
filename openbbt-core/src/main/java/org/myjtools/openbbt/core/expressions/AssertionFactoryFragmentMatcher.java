package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.adapters.AssertionFactory;
import org.myjtools.openbbt.core.adapters.AssertionPattern;

import java.util.Locale;

public class AssertionFactoryFragmentMatcher<T> implements FragmentMatcher {

    private final AssertionFactory<T> assertionFactory;

    public AssertionFactoryFragmentMatcher(AssertionFactory<T> assertionFactory) {
        this.assertionFactory = assertionFactory;
    }

    @Override
    public MatchResult matches(String input, Locale locale) {
        for (AssertionPattern<T> pattern : assertionFactory.patterns(locale)) {
            var matcher = pattern.matcher(input);
            if (matcher.find()) {
                Assertion assertion = assertionFactory.assertion(pattern,input);
                return new MatchResult(true, matcher.end(), assertion);
            }
        }
        return new MatchResult(false);
    }

    @Override
    public String toString() {
        return "AssertionFactory["+assertionFactory.name()+"]";
    }
}
