package org.myjtools.openbbt.api.expressions;

import org.myjtools.openbbt.api.DataType;

import java.util.Locale;

public class ArgumentFragmentMatcher implements FragmentMatcher {

    private final DataType dataType;
    private final String name;

    public ArgumentFragmentMatcher(DataType dataType) {
        this(dataType.name(), dataType);
    }

    public ArgumentFragmentMatcher(String name, DataType dataType) {
        this.dataType = dataType;
        this.name = name;
    }

    @Override
    public boolean matches(String input, Locale locale) {
        return dataType.pattern(locale).matcher(input).matches();
    }

    @Override
    public String toString() {
        return "Argument["+name+":"+dataType.name()+"]";
    }

}
