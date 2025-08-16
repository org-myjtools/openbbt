package org.myjtools.openbbt.core.assertions;

import java.util.stream.Stream;

public interface AssertionFactoryProvider {

    Stream<AssertionFactory<?>> assertionFactories();

}
