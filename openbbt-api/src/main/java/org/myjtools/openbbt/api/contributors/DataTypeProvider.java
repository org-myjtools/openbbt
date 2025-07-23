package org.myjtools.openbbt.api.contributors;


import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.api.Contributor;
import org.myjtools.openbbt.api.DataType;

import java.util.stream.Stream;

@ExtensionPoint
public interface DataTypeProvider extends Contributor {

	Stream<DataType> dataTypes();

}
