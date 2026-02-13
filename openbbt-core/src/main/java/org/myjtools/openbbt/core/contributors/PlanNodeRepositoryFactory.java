package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.PlanNodeRepository;

@ExtensionPoint
public interface PlanNodeRepositoryFactory {

	enum Mode {
		IN_MEMORY, FILE, REMOTE
	}

	PlanNodeRepository create(Mode mode, Object...args);

}