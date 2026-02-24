package org.myjtools.openbbt.persistence.test.project;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.plannode.NodeType;
import org.myjtools.openbbt.core.plannode.PlanNode;
import org.myjtools.openbbt.core.plannode.PlanNodeID;
import org.myjtools.openbbt.core.plannode.TagExpression;
import org.myjtools.openbbt.core.project.Plan;
import org.myjtools.openbbt.core.project.PlanID;
import org.myjtools.openbbt.core.project.Project;
import org.myjtools.openbbt.core.project.TestSuite;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import org.myjtools.openbbt.persistence.plannode.JooqPlanNodeRepository;
import org.myjtools.openbbt.persistence.project.JooqProjectRepository;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractProjectRepositoryTest {

	protected JooqProjectRepository repo;
	protected JooqPlanNodeRepository nodeRepo;
	private DataSource dataSource;

	protected abstract DataSourceProvider dataSourceProvider();

	@BeforeEach
	void setUp() {
		DataSourceProvider provider = dataSourceProvider();
		dataSource = provider.obtainDataSource();
		repo = new JooqProjectRepository(dataSource, provider.dialect());
		nodeRepo = new JooqPlanNodeRepository(dataSource, provider.dialect());
		repo.clearAllData();
		nodeRepo.clearAllData();
	}

	@AfterEach
	void tearDown() {
		if (dataSource instanceof HikariDataSource hikari) {
			hikari.close();
		}
	}

	// --- helpers ---

	private Project project(String org, String name) {
		return new Project(name, "description of " + name, org, List.of());
	}

	private Plan plan(PlanNodeID planNodeRoot) {
		return new Plan(
			new PlanID(UUID.randomUUID()),
			Instant.now(),
			"resource-hash-" + UUID.randomUUID(),
			"config-hash-" + UUID.randomUUID(),
			planNodeRoot
		);
	}

	private PlanNodeID newPlanNode() {
		return nodeRepo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("plan root"));
	}

	private TestSuite suite(String name, String tagExpression) {
		return new TestSuite(name, "description of " + name, TagExpression.parse(tagExpression));
	}

	// --- project tests ---

	@Test
	void persistProject_and_getProject_roundtrip() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);

		Project retrieved = repo.getProject("Acme", "my-project").orElseThrow();
		assertThat(retrieved.name()).isEqualTo("my-project");
		assertThat(retrieved.description()).isEqualTo("description of my-project");
		assertThat(retrieved.organization()).isEqualTo("Acme");
	}

	@Test
	void persistProject_updatesExistingProject() {
		repo.persistProject(project("Acme", "my-project"));

		Project updated = new Project("my-project", "updated description", "Acme", List.of());
		repo.persistProject(updated);

		Project retrieved = repo.getProject("Acme", "my-project").orElseThrow();
		assertThat(retrieved.description()).isEqualTo("updated description");
	}

	@Test
	void getProject_returnsEmpty_whenNotFound() {
		assertThat(repo.getProject("Acme", "nonexistent")).isEmpty();
	}

	@Test
	void deleteProject_removesProject() {
		repo.persistProject(project("Acme", "my-project"));
		assertThat(repo.getProject("Acme", "my-project")).isPresent();

		repo.deleteProject("Acme", "my-project");

		assertThat(repo.getProject("Acme", "my-project")).isEmpty();
	}

	@Test
	void deleteProject_cascadesToPlans() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		Plan plan = plan(newPlanNode());
		PlanID planID = repo.persistPlan(p, plan);
		assertThat(repo.getPlan(planID)).isPresent();

		repo.deleteProject("Acme", "my-project");

		assertThat(repo.getPlan(planID)).isEmpty();
	}

	@Test
	void searchProjects_byProjectName() {
		repo.persistProject(project("Acme", "alpha-project"));
		repo.persistProject(project("Acme", "beta-project"));
		repo.persistProject(project("Acme", "gamma-service"));

		List<Project> results = repo.searchProjects("project");
		assertThat(results).hasSize(2);
		assertThat(results).extracting(Project::name)
			.containsExactlyInAnyOrder("alpha-project", "beta-project");
	}

	@Test
	void searchProjects_byOrganizationName() {
		repo.persistProject(project("AcmeCorp", "project-a"));
		repo.persistProject(project("OtherOrg", "project-b"));

		List<Project> results = repo.searchProjects("Acme");
		assertThat(results).hasSize(1);
		assertThat(results.getFirst().name()).isEqualTo("project-a");
	}

	@Test
	void searchProjects_returnsEmpty_whenNoMatch() {
		repo.persistProject(project("Acme", "my-project"));

		assertThat(repo.searchProjects("nonexistent")).isEmpty();
	}

	// --- plan tests ---

	@Test
	void persistPlan_and_getPlan_roundtrip() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		Plan plan = plan(newPlanNode());

		PlanID planID = repo.persistPlan(p, plan);

		assertThat(planID).isNotNull();
		Plan retrieved = repo.getPlan(planID).orElseThrow();
		assertThat(retrieved.planID()).isEqualTo(planID);
		assertThat(retrieved.resourceSetHash()).isEqualTo(plan.resourceSetHash());
		assertThat(retrieved.configurationHash()).isEqualTo(plan.configurationHash());
		assertThat(retrieved.planNodeRoot()).isEqualTo(plan.planNodeRoot());
	}

	@Test
	void getPlan_returnsEmpty_whenNotFound() {
		assertThat(repo.getPlan(new PlanID(UUID.randomUUID()))).isEmpty();
	}

	@Test
	void getPlansForProject_returnsAllPlansForProject() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		repo.persistPlan(p, plan(newPlanNode()));
		repo.persistPlan(p, plan(newPlanNode()));

		List<Plan> plans = repo.getPlansForProject("Acme", "my-project");
		assertThat(plans).hasSize(2);
	}

	@Test
	void getPlansForProject_doesNotReturnPlansFromOtherProject() {
		Project p1 = project("Acme", "project-a");
		Project p2 = project("Acme", "project-b");
		repo.persistProject(p1);
		repo.persistProject(p2);
		repo.persistPlan(p1, plan(newPlanNode()));
		repo.persistPlan(p2, plan(newPlanNode()));

		assertThat(repo.getPlansForProject("Acme", "project-a")).hasSize(1);
		assertThat(repo.getPlansForProject("Acme", "project-b")).hasSize(1);
	}

	@Test
	void deletePlan_removesPlan() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		PlanID planID = repo.persistPlan(p, plan(newPlanNode()));
		assertThat(repo.getPlan(planID)).isPresent();

		repo.deletePlan(planID);

		assertThat(repo.getPlan(planID)).isEmpty();
	}

	@Test
	void deletePlan_cascadesToSuites() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		Plan plan = plan(newPlanNode());
		PlanID planID = repo.persistPlan(p, plan);
		repo.persistTestSuite(p, plan, suite("suiteA", "smoke"));
		assertThat(repo.getTestSuites(planID)).hasSize(1);

		repo.deletePlan(planID);

		assertThat(repo.getTestSuites(planID)).isEmpty();
	}

	// --- test suite tests ---

	@Test
	void persistTestSuite_and_getTestSuites_roundtrip() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		Plan plan = plan(newPlanNode());
		PlanID planID = repo.persistPlan(p, plan);

		repo.persistTestSuite(p, plan, suite("suiteA", "smoke or regression"));

		List<TestSuite> suites = repo.getTestSuites(planID);
		assertThat(suites).hasSize(1);
		assertThat(suites.getFirst().name()).isEqualTo("suiteA");
		assertThat(suites.getFirst().description()).isEqualTo("description of suiteA");
		assertThat(suites.getFirst().tagExpression()).hasToString("smoke or regression");
	}

	@Test
	void persistTestSuite_updatesExistingTestSuite() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		Plan plan = plan(newPlanNode());
		PlanID planID = repo.persistPlan(p, plan);
		repo.persistTestSuite(p, plan, suite("suiteA", "smoke"));

		TestSuite updated = new TestSuite("suiteA", "updated description", TagExpression.parse("regression"));
		repo.persistTestSuite(p, plan, updated);

		List<TestSuite> suites = repo.getTestSuites(planID);
		assertThat(suites).hasSize(1);
		assertThat(suites.getFirst().description()).isEqualTo("updated description");
		assertThat(suites.getFirst().tagExpression()).hasToString("regression");
	}

	@Test
	void getTestSuites_returnsOnlySuitesForGivenPlan() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		Plan plan1 = plan(newPlanNode());
		Plan plan2 = plan(newPlanNode());
		PlanID planID1 = repo.persistPlan(p, plan1);
		PlanID planID2 = repo.persistPlan(p, plan2);
		repo.persistTestSuite(p, plan1, suite("suiteA", "smoke"));
		repo.persistTestSuite(p, plan1, suite("suiteB", "regression"));
		repo.persistTestSuite(p, plan2, suite("suiteC", "e2e"));

		assertThat(repo.getTestSuites(planID1)).hasSize(2);
		assertThat(repo.getTestSuites(planID2)).hasSize(1);
	}

	@Test
	void deleteTestSuite_removesSuite() {
		Project p = project("Acme", "my-project");
		repo.persistProject(p);
		Plan plan = plan(newPlanNode());
		PlanID planID = repo.persistPlan(p, plan);
		repo.persistTestSuite(p, plan, suite("suiteA", "smoke"));
		repo.persistTestSuite(p, plan, suite("suiteB", "regression"));
		assertThat(repo.getTestSuites(planID)).hasSize(2);

		repo.deleteTestSuite(planID, "suiteA");

		List<TestSuite> remaining = repo.getTestSuites(planID);
		assertThat(remaining).hasSize(1);
		assertThat(remaining.getFirst().name()).isEqualTo("suiteB");
	}

}
