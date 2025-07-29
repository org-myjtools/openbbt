package org.myjtools.openbbt.core.adapters;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.myjtools.openbbt.core.Assertion;



public class AssertionAdapter<T> implements Assertion<T> {

    private final String name;
    private final Matcher<T> matcher;

    public AssertionAdapter(String name, Matcher<T> matcher) {
        this.name = name;
        this.matcher = matcher;
    }

    @Override
    public boolean test(Object actualValue) {
        return matcher.matches(actualValue);
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public String describeFailure(Object actualValue) {
        Description description = Description.NONE;
        matcher.describeMismatch(actualValue, description);
        return description.toString();
    }



}
