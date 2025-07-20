package org.myjtools.openbbt.api;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public interface DataType {

    String name();
    Class<?> javaType();
    Pattern pattern(Locale locale);
    List<String> hints(Locale locale);
    Object parse (Locale locale, String value);

}
