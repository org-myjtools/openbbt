package org.myjtools.openbbt.core.datatypes;


import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.Contributor;

import java.util.stream.Stream;

@ExtensionPoint
public interface DataTypeProvider extends Contributor {

	Stream<DataType> dataTypes();

}
