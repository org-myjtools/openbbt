package org.myjtools.openbbt.core.adapters;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class AssertionFactory {

    private final String name;

    public AssertionFactory(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public List<Pattern> patterns(Locale locale) {
        return List.of();
    }
}
