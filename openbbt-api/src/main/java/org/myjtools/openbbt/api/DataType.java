package org.myjtools.openbbt.api;

import org.myjtools.openbbt.api.expressions.FragmentMatcher;
import org.myjtools.openbbt.api.util.Patterns;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface DataType {

    String name();

    Class<?> javaType();

    String regex(Locale locale);

    List<String> hints(Locale locale);

    Object parse (Locale locale, String value);

    default Matcher matcher(Locale locale, String value) {
        return pattern(locale).matcher(value);
    }

    default Pattern pattern(Locale locale) {
        return Patterns.of(regex(locale));
    }
}
