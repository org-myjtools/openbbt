package org.myjtools.openbbt.core.assertions;

import static org.hamcrest.Matchers.*;
import org.myjtools.openbbt.core.datatypes.DataType;
import org.myjtools.openbbt.core.messages.Messages;

import java.util.function.Function;


public class ComparableAssertionFactory<T extends Comparable<T>> extends AssertionFactoryAdapter<T> {


    public static final String ASSERTION_NUMBER_EQUALS = "assertion.number.equals";
    public static final String ASSERTION_NUMBER_GREATER = "assertion.number.greater";
    public static final String ASSERTION_NUMBER_LESS = "assertion.number.less";
    public static final String ASSERTION_NUMBER_GREATER_EQUALS = "assertion.number.greater.equals";
    public static final String ASSERTION_NUMBER_LESS_EQUALS = "assertion.number.less.equals";
    public static final String ASSERTION_NUMBER_NOT_EQUALS = "assertion.number.not.equals";
    public static final String ASSERTION_NUMBER_NOT_GREATER = "assertion.number.not.greater";
    public static final String ASSERTION_NUMBER_NOT_LESS = "assertion.number.not.less";
    public static final String ASSERTION_NUMBER_NOT_GREATER_EQUALS = "assertion.number.not.greater.equals";
    public static final String ASSERTION_NUMBER_NOT_LESS_EQUALS = "assertion.number.not.less.equals";
    public static final String ASSERTION_GENERIC_NULL = "assertion.generic.null";
    public static final String ASSERTION_GENERIC_NOT_NULL = "assertion.generic.not.null";



    public ComparableAssertionFactory(String name, Function<String,T> parser, DataType type, Messages messages) {
        super(name,parser,type,messages);
    }


    protected void fillSuppliers() {
        suppliers.put(
                ASSERTION_NUMBER_EQUALS,
                it -> new AssertionAdapter(name, comparesEqualTo(it)));
        suppliers.put(
                ASSERTION_NUMBER_GREATER,
                it -> new AssertionAdapter(name, greaterThan(it))
        );
        suppliers.put(
                ASSERTION_NUMBER_LESS,
                it -> new AssertionAdapter(name, lessThan(it))
        );
        suppliers.put(
                ASSERTION_NUMBER_GREATER_EQUALS,
                it -> new AssertionAdapter(name, greaterThanOrEqualTo((it)))
        );
        suppliers.put(
                ASSERTION_NUMBER_LESS_EQUALS,
                it -> new AssertionAdapter(name, lessThanOrEqualTo((it)))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_EQUALS,
                it -> new AssertionAdapter(name, not(equalTo(it)))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_GREATER,
                it -> new AssertionAdapter(name, not(greaterThan((it))))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_LESS,
                it -> new AssertionAdapter(name, not(lessThan((it))))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_GREATER_EQUALS,
                it -> new AssertionAdapter(name, not(greaterThanOrEqualTo((it))))
        );
        suppliers.put(
                ASSERTION_NUMBER_NOT_LESS_EQUALS,
                it -> new AssertionAdapter(name, not(lessThanOrEqualTo((it))))
        );
        suppliers.put(
                ASSERTION_GENERIC_NULL,
                it -> new AssertionAdapter(name, nullValue())
        );
        suppliers.put(
                ASSERTION_GENERIC_NOT_NULL,
                it -> new AssertionAdapter(name, not(nullValue()))
        );
    }



}
