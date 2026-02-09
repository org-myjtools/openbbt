package org.myjtools.openbbt.core.contributors;


import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.DataType;

import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@ExtensionPoint
public interface DataTypeProvider extends Contributor {

	Stream<DataType> dataTypes();

}
