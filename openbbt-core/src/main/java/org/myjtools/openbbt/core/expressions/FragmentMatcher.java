package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.Assertion;

import java.util.Locale;

public interface FragmentMatcher {


    record MatchResult(
            boolean startMatched,
            int consumed,
            ArgumentValue argument,
            Assertion assertion
    ) {
        public MatchResult(boolean startMatched) {
            this(startMatched, 0, null, null);
        }

        public MatchResult(boolean startMatched, int consumed) {
            this(startMatched, consumed, null, null);
        }

        public MatchResult(boolean startMatched, int consumed, ArgumentValue argumentValue) {
            this(startMatched, consumed, argumentValue, null);
        }

        public MatchResult(boolean startMatched, int consumed, Assertion assertion) {
            this(startMatched, consumed, null, assertion);
        }


    }

    MatchResult matches(String input, Locale locale);

}
