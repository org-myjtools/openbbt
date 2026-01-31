package org.myjtools.openbbt.core;

import org.myjtools.jexten.ExtensionPoint;
import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@ExtensionPoint
public interface AssertionFactoryProvider {

    Stream<AssertionFactory<?>> assertionFactories();

}
