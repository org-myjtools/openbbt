package org.myjtools.openbbt.core;

import java.util.List;
import java.util.Locale;


/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public interface AssertionFactory<T> {

	String name();

	List<AssertionPattern<T>> patterns(Locale locale);

	Assertion assertion(AssertionPattern<T> pattern, String input);

}
