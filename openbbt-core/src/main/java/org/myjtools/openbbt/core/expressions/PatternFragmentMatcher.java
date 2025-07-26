package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.util.Patterns;
import java.util.regex.Pattern;

public class PatternFragmentMatcher implements FragmentMatcher {

    private final Pattern pattern;
    private final String literal;

    public PatternFragmentMatcher(String regex, String literal) {
        this.pattern = Patterns.of(regex);
        this.literal = literal;
    }

    @Override
    public MatchResult matches(String input) {
        var matcher = pattern.matcher(input);
        if (matcher.find()) {
            return new MatchResult(true, matcher.end());
        } else {
            return new MatchResult(false, 0);
        }
    }


    public Pattern pattern() {
        return pattern;
    }

    public String literal() {
        return literal;
    }

    @Override
    public String toString() {
        return "Pattern["+literal+"]";
    }
}
