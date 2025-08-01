package org.myjtools.openbbt.core.adapters;

import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.AssertionFactory;
import org.myjtools.openbbt.core.AssertionPattern;
import org.myjtools.openbbt.core.DataType;
import org.myjtools.openbbt.core.util.Patterns;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;

import static org.hamcrest.Matchers.*;

public class StringAssertionFactory extends AssertionFactoryAdapter<String> {


    public static final String ASSERTION_STRING_EQUALS = "assertion.string.equals";
    public static final String ASSERTION_STRING_NOT_EQUALS = "assertion.string.not.equals";
    public static final String ASSERTION_GENERIC_NULL = "assertion.generic.null";
    public static final String ASSERTION_GENERIC_NOT_NULL = "assertion.generic.not.null";
    public static final String ASSERTION_STRING_EQUALS_IGNORE_CASE = "assertion.string.equals.ignore.case";
    public static final String ASSERTION_STRING_EQUALS_IGNORE_WHITESPACE = "assertion.string.equals.ignore.whitespace";
    public static final String ASSERTION_STRING_STARTS_WITH = "assertion.string.starts.with";
    public static final String ASSERTION_STRING_STARTS_WITH_IGNORE_CASE = "assertion.string.starts.with.ignore.case";
    public static final String ASSERTION_STRING_ENDS_WITH = "assertion.string.ends.with";
    public static final String ASSERTION_STRING_ENDS_WITH_IGNORE_CASE = "assertion.string.ends.with.ignore.case";
    public static final String ASSERTION_STRING_CONTAINS = "assertion.string.contains";
    public static final String ASSERTION_STRING_CONTAINS_IGNORE_CASE = "assertion.string.contains.ignore.case";
    public static final String ASSERTION_STRING_NOT_EQUALS_IGNORE_CASE = "assertion.string.not.equals.ignore.case";
    public static final String ASSERTION_STRING_NOT_EQUALS_IGNORE_WHITESPACE = "assertion.string.not.equals.ignore.whitespace";
    public static final String ASSERTION_STRING_NOT_STARTS_WITH = "assertion.string.not.starts.with";
    public static final String ASSERTION_STRING_NOT_STARTS_WITH_IGNORE_CASE = "assertion.string.not.starts.with.ignore.case";
    public static final String ASSERTION_STRING_NOT_ENDS_WITH = "assertion.string.not.ends.with";
    public static final String ASSERTION_STRING_NOT_ENDS_WITH_IGNORE_CASE = "assertion.string.not.ends.with.ignore.case";
    public static final String ASSERTION_STRING_NOT_CONTAINS = "assertion.string.not.contains";
    public static final String ASSERTION_STRING_NOT_CONTAINS_IGNORE_CASE = "assertion.string.not.contains.ignore.case";


    public StringAssertionFactory(String name, DataType type, Messages messages) {
        super(name,StringAssertionFactory::parse,type,messages);
    }


    private static String parse(String value) {
        return value.substring(1,value.length()-1);
    }

    protected void fillSuppliers() {
        suppliers.put(
                ASSERTION_STRING_EQUALS,
                it -> new AssertionAdapter(name, comparesEqualTo(it)));
        suppliers.put(
                ASSERTION_STRING_NOT_EQUALS,
                it -> new AssertionAdapter(name, not(comparesEqualTo(it))));
        suppliers.put(
                ASSERTION_GENERIC_NULL,
                it -> new AssertionAdapter(name, nullValue())
        );
        suppliers.put(
                ASSERTION_GENERIC_NOT_NULL,
                it -> new AssertionAdapter(name, not(nullValue()))
        );
        suppliers.put(
                ASSERTION_STRING_EQUALS_IGNORE_CASE,
                it -> new AssertionAdapter(name, equalToIgnoringCase(it))
        );
        suppliers.put(
                ASSERTION_STRING_EQUALS_IGNORE_WHITESPACE,
                it -> new AssertionAdapter(name, equalToIgnoringCase(it))
        );
        suppliers.put(
                ASSERTION_STRING_STARTS_WITH,
                it -> new AssertionAdapter(name, startsWith(it))
        );
        suppliers.put(
                ASSERTION_STRING_STARTS_WITH_IGNORE_CASE,
                it -> new AssertionAdapter(name, startsWithIgnoringCase(it))
        );
        suppliers.put(
                ASSERTION_STRING_ENDS_WITH,
                it -> new AssertionAdapter(name, endsWith(it))
        );
        suppliers.put(
                ASSERTION_STRING_ENDS_WITH_IGNORE_CASE,
                it -> new AssertionAdapter(name, endsWithIgnoringCase(it))
        );
        suppliers.put(
                ASSERTION_STRING_CONTAINS,
                it -> new AssertionAdapter(name, containsString(it))
        );
        suppliers.put(
                ASSERTION_STRING_CONTAINS_IGNORE_CASE,
                it -> new AssertionAdapter(name, containsStringIgnoringCase(it))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_EQUALS_IGNORE_CASE,
                it -> new AssertionAdapter(name, not(equalToIgnoringCase(it)))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_EQUALS_IGNORE_WHITESPACE,
                it -> new AssertionAdapter(name, not(equalToIgnoringCase(it)))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_STARTS_WITH,
                it -> new AssertionAdapter(name, not(startsWith(it)))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_STARTS_WITH_IGNORE_CASE,
                it -> new AssertionAdapter(name, not(startsWithIgnoringCase(it)))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_ENDS_WITH,
                it -> new AssertionAdapter(name, not(endsWith(it)))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_ENDS_WITH_IGNORE_CASE,
                it -> new AssertionAdapter(name, not(endsWithIgnoringCase(it)))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_CONTAINS,
                it -> new AssertionAdapter(name, not(containsString(it)))
        );
        suppliers.put(
                ASSERTION_STRING_NOT_CONTAINS_IGNORE_CASE,
                it -> new AssertionAdapter(name, not(containsStringIgnoringCase(it)))
        );
    }



}
