package org.myjtools.openbbt.core.expressions;

import java.util.Locale;

public interface FragmentMatcher {


    record MatchResult(boolean startMatched, int consumed, ArgumentValue argument) {
        public MatchResult(boolean startMatched, int consumed) {
            this(startMatched, consumed, null);
        }
    }

    MatchResult matches(String input, Locale locale);

}
