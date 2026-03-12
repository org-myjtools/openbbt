package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import java.util.UUID;

@ExtensionPoint
public interface ReportBuilder {

	void buildReport(UUID executionID);

}
