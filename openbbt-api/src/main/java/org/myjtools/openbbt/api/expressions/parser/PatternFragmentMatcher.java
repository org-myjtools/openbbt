package org.myjtools.openbbt.api.expressions.parser;

import org.myjtools.openbbt.api.Patterns;

import java.util.Locale;
import java.util.regex.Pattern;

public class PatternFragmentMatcher implements FragmentMatcher {

    private final Pattern pattern;
    private final String literal;

    public PatternFragmentMatcher(String regex, String literal) {
        this.pattern = Patterns.of(regex);
        this.literal = literal;
    }

    public boolean matches(String input, Locale locale) {
        var matcher = pattern.matcher(input);
        return matcher.matches();
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
