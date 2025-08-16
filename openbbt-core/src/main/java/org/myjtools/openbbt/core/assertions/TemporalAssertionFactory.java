package org.myjtools.openbbt.core.assertions;

import org.myjtools.openbbt.core.datatypes.DataType;
import org.myjtools.openbbt.core.messages.Messages;

import java.util.function.Function;

import static org.hamcrest.Matchers.*;

public class TemporalAssertionFactory<T extends Comparable<T>> extends ComparableAssertionFactory<T> {


    public static final String ASSERTION_TEMPORAL_EQUALS = "assertion.temporal.equals";
    public static final String ASSERTION_TEMPORAL_AFTER = "assertion.temporal.after";
    public static final String ASSERTION_TEMPORAL_LESS = "assertion.temporal.before";
    public static final String ASSERTION_TEMPORAL_AFTER_EQUALS = "assertion.temporal.after.equals";
    public static final String ASSERTION_TEMPORAL_LESS_EQUALS = "assertion.temporal.before.equals";
    public static final String ASSERTION_TEMPORAL_NOT_EQUALS = "assertion.temporal.not.equals";
    public static final String ASSERTION_TEMPORAL_NOT_AFTER = "assertion.temporal.not.after";
    public static final String ASSERTION_TEMPORAL_NOT_LESS = "assertion.temporal.not.before";
    public static final String ASSERTION_TEMPORAL_NOT_AFTER_EQUALS = "assertion.temporal.not.after.equals";
    public static final String ASSERTION_TEMPORAL_NOT_LESS_EQUALS = "assertion.temporal.not.before.equals";
    public static final String ASSERTION_GENERIC_NULL = "assertion.generic.null";
    public static final String ASSERTION_GENERIC_NOT_NULL = "assertion.generic.not.null";


    public TemporalAssertionFactory(String name, Function<String,T> parser, DataType type, Messages messages) {
        super(name,parser,type,messages);
    }


    protected void fillSuppliers() {
        suppliers.put(
                ASSERTION_TEMPORAL_EQUALS,
                it -> new AssertionAdapter(name, comparesEqualTo(it)));
        suppliers.put(
                ASSERTION_TEMPORAL_AFTER,
                it -> new AssertionAdapter(name, greaterThan(it))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_LESS,
                it -> new AssertionAdapter(name, lessThan(it))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_AFTER_EQUALS,
                it -> new AssertionAdapter(name, greaterThanOrEqualTo((it)))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_LESS_EQUALS,
                it -> new AssertionAdapter(name, lessThanOrEqualTo((it)))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_NOT_EQUALS,
                it -> new AssertionAdapter(name, not(equalTo(it)))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_NOT_AFTER,
                it -> new AssertionAdapter(name, not(greaterThan((it))))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_NOT_LESS,
                it -> new AssertionAdapter(name, not(lessThan((it))))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_NOT_AFTER_EQUALS,
                it -> new AssertionAdapter(name, not(greaterThanOrEqualTo((it))))
        );
        suppliers.put(
                ASSERTION_TEMPORAL_NOT_LESS_EQUALS,
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
