package org.myjtools.openbbt.core.contributors;


import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.Contributor;
import org.myjtools.openbbt.core.DataType;

import java.util.stream.Stream;

@ExtensionPoint
public interface DataTypeProvider extends Contributor {

	Stream<DataType> dataTypes();

}
