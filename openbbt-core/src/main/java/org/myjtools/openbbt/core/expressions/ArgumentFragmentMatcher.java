package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.DataType;
import org.myjtools.openbbt.core.util.Patterns;

import java.util.Locale;
import java.util.regex.Pattern;

public class ArgumentFragmentMatcher implements FragmentMatcher {

    private static final Pattern variable = Patterns.of("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

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
    public MatchResult matches(String input, Locale locale) {
        var dataTypePattern = dataType.pattern().matcher(input);
        if (dataTypePattern.find()) {
            return new MatchResult(
                true,
                dataTypePattern.end(),
                new LiteralValue(name, dataTypePattern.group(), dataType)
            );
        } else {
            var variableMatcher = variable.matcher(input);
            if (variableMatcher.find()) {
                return new MatchResult(
                    true,
                    variableMatcher.end(),
                    new VariableValue(name, variableMatcher.group(1), dataType)
                );
            }
            return new MatchResult(false);
        }
    }


    @Override
    public String toString() {
        return "Argument["+name+":"+dataType.name()+"]";
    }

    public DataType type() {
        return dataType;
    }

    public String name() {
        return name;
    }
}
