package org.myjtools.openbbt.core.persistence;

import org.myjtools.openbbt.core.project.Plan;
import org.myjtools.openbbt.core.project.PlanID;
import org.myjtools.openbbt.core.project.Project;
import org.myjtools.openbbt.core.project.TestSuite;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends Repository{

	void persistProject(Project project) ;

	Optional<Project> getProject(String organizationName, String projectName);

	void deleteProject(String organizationName, String projectName);

	List<Project> searchProjects(String searchTerm);

	PlanID persistPlan(Project project, Plan plan);

	Optional<Plan> getPlan(PlanID planID);

	List<Plan> getPlansForProject(String organizationName, String projectName);

	void deletePlan(PlanID planID);

	void persistTestSuite(Project project, Plan plan, TestSuite testSuite);

	List<TestSuite> getTestSuites(PlanID planID);

	void deleteTestSuite(PlanID planID, String testSuiteName);

	void deleteTestSuites(PlanID planID);
}
