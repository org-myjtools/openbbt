package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.adapters.AssertionFactory;

import java.util.Locale;
import java.util.regex.Pattern;

public class AssertionFactoryFragmentMatcher implements FragmentMatcher {

    private final AssertionFactory assertionFactory;

    public AssertionFactoryFragmentMatcher(AssertionFactory assertionFactory) {
        this.assertionFactory = assertionFactory;
    }

    @Override
    public MatchResult matches(String input, Locale locale) {
        for (Pattern pattern : assertionFactory.patterns(locale)) {
            var matcher = pattern.matcher(input);
            if (matcher.find()) {
                return new MatchResult(
                    true,
                    matcher.end(),
                    null
                );
            }
        }
        return new MatchResult(false, 0);
    }

    @Override
    public String toString() {
        return "AssertionFactory["+assertionFactory.name()+"]";
    }
}
