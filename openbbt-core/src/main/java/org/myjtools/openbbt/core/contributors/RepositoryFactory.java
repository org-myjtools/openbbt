package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.persistence.PlanNodeRepository;
import org.myjtools.openbbt.core.persistence.ProjectRepository;

@ExtensionPoint
public interface RepositoryFactory {

	PlanNodeRepository createPlanNodeRepository();
	ProjectRepository createProjectRepository();

}