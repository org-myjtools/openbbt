package org.myjtools.openbbt.core.adapters;

import org.myjtools.openbbt.core.Assertion;

import java.util.List;
import java.util.Locale;

public interface AssertionFactory<T> {


    String name();

    List<AssertionPattern<T>> patterns(Locale locale);

    Assertion assertion(AssertionPattern<T> pattern, String input);

}
