package org.myjtools.openbbt.core.assertions;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;


public class AssertionAdapter implements Assertion {

    private final Matcher<?> matcher;
    private final String name;

    public AssertionAdapter(String name, Matcher<?> matcher) {
        this.name = name;
        this.matcher = matcher;
    }

    @Override
    public boolean test(Object actualValue) {
        return matcher.matches(actualValue);
    }



    @Override
    public String describeFailure(Object actualValue) {
        Description description = new StringDescription();
        matcher.describeMismatch(actualValue, description);
        return description.toString();
    }


    @Override
    public String name() {
        return name;
    }


}
