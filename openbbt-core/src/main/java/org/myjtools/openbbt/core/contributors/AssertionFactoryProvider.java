package org.myjtools.openbbt.core.contributors;

import org.myjtools.openbbt.core.AssertionFactory;

import java.util.stream.Stream;

public interface AssertionFactoryProvider {

    Stream<AssertionFactory<?>> assertionFactories();

}
