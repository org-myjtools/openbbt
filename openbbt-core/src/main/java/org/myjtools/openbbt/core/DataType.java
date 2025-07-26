package org.myjtools.openbbt.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a data type in the OpenBBT API.
 * Each data type has a name, a Java type, hints for usage, and a regex pattern for validation.
 * It also provides methods to parse a string value into the corresponding data type.
 */
public interface DataType {

    String name();

    Class<?> javaType();

    String hint();

    Pattern pattern();

    Object parse (String value);

    default Matcher matcher(String value) {
        return pattern().matcher(value);
    }


}
