package org.myjtools.openbbt.core;

import java.util.List;
import java.util.Locale;


public interface AssertionFactory<T> {


    String name();

    List<AssertionPattern<T>> patterns(Locale locale);

    Assertion assertion(AssertionPattern<T> pattern, String input);

}
