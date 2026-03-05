package org.myjtools.openbbt.core.extensions;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.AssertionFactory;
import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@ExtensionPoint
public interface AssertionFactoryProvider {

	Stream<AssertionFactory<?>> assertionFactories();

}
