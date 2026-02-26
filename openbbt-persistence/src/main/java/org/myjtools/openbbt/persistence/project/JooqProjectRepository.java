package org.myjtools.openbbt.persistence.project;

import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.myjtools.openbbt.core.persistence.ProjectRepository;
import org.myjtools.openbbt.core.plannode.PlanNodeID;
import org.myjtools.openbbt.core.plannode.TagExpression;
import org.myjtools.openbbt.core.project.Plan;
import org.myjtools.openbbt.core.project.PlanID;
import org.myjtools.openbbt.core.project.Project;
import org.myjtools.openbbt.core.project.TestSuite;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JooqProjectRepository implements ProjectRepository {

	private static final Table<Record> TABLE_ORGANIZATION = DSL.table("organization");
	private static final Table<Record> TABLE_PROJECT = DSL.table("project");
	private static final Table<Record> TABLE_PLAN = DSL.table("plan");
	private static final Table<Record> TABLE_SUITE = DSL.table("suite");

	private static final Field<String> FIELD_NAME = DSL.field("name", String.class);
	private static final Field<String> FIELD_ORGANIZATION_NAME = DSL.field("organization_name", String.class);
	private static final Field<String> FIELD_PROJECT_NAME = DSL.field("project_name", String.class);
	private static final Field<String> FIELD_DESCRIPTION = DSL.field("description", String.class);
	private static final Field<UUID> FIELD_PLAN_ID = DSL.field("plan_id", UUID.class);
	private static final Field<LocalDateTime> FIELD_CREATED_AT = DSL.field("created_at", LocalDateTime.class);
	private static final Field<String> FIELD_RESOURCE_SET_HASH = DSL.field("resource_set_hash", String.class);
	private static final Field<String> FIELD_CONFIGURATION_HASH = DSL.field("configuration_hash", String.class);
	private static final Field<UUID> FIELD_PLAN_NODE_ROOT = DSL.field("plan_node_root", UUID.class);
	private static final Field<String> FIELD_TAG_EXPRESSION = DSL.field("tag_expression", String.class);

	private final DSLContext dsl;

	public JooqProjectRepository(DataSource dataSource, SQLDialect dialect) {
		this.dsl = DSL.using(new DataSourceConnectionProvider(dataSource), dialect);
	}

	public JooqProjectRepository(DataSourceProvider dataSourceProvider) {
		this(dataSourceProvider.obtainDataSource(), dataSourceProvider.dialect());
	}


	public void clearAllData() {
		dsl.deleteFrom(TABLE_SUITE).execute();
		dsl.deleteFrom(TABLE_PLAN).execute();
		dsl.deleteFrom(TABLE_PROJECT).execute();
		dsl.deleteFrom(TABLE_ORGANIZATION).execute();
	}


	@Override
	public void persistProject(Project project) {
		dsl.mergeInto(TABLE_ORGANIZATION)
			.using(DSL.selectOne())
			.on(FIELD_NAME.eq(project.organization()))
			.whenNotMatchedThenInsert(FIELD_NAME)
			.values(project.organization())
			.execute();
		dsl.mergeInto(TABLE_PROJECT)
			.using(DSL.selectOne())
			.on(FIELD_ORGANIZATION_NAME.eq(project.organization()).and(FIELD_PROJECT_NAME.eq(project.name())))
			.whenMatchedThenUpdate()
			.set(FIELD_DESCRIPTION, project.description())
			.whenNotMatchedThenInsert(FIELD_ORGANIZATION_NAME, FIELD_PROJECT_NAME, FIELD_DESCRIPTION)
			.values(project.organization(), project.name(), project.description())
			.execute();
	}


	@Override
	public Optional<Project> getProject(String organizationName, String projectName) {
		return dsl.select(FIELD_ORGANIZATION_NAME, FIELD_PROJECT_NAME, FIELD_DESCRIPTION)
			.from(TABLE_PROJECT)
			.where(FIELD_ORGANIZATION_NAME.eq(organizationName).and(FIELD_PROJECT_NAME.eq(projectName)))
			.fetchOptional()
			.map(rec -> new Project(
				rec.get(FIELD_PROJECT_NAME),
				rec.get(FIELD_DESCRIPTION),
				rec.get(FIELD_ORGANIZATION_NAME),
				List.of()
			));
	}


	@Override
	public void deleteProject(String organizationName, String projectName) {
		dsl.deleteFrom(TABLE_PROJECT)
			.where(FIELD_ORGANIZATION_NAME.eq(organizationName).and(FIELD_PROJECT_NAME.eq(projectName)))
			.execute();
	}


	@Override
	public List<Project> searchProjects(String searchTerm) {
		return dsl.select(FIELD_ORGANIZATION_NAME, FIELD_PROJECT_NAME, FIELD_DESCRIPTION)
			.from(TABLE_PROJECT)
			.where(
				FIELD_PROJECT_NAME.likeIgnoreCase("%" + searchTerm + "%")
				.or(FIELD_ORGANIZATION_NAME.likeIgnoreCase("%" + searchTerm + "%"))
			)
			.fetch().stream()
			.map(rec -> new Project(
				rec.get(FIELD_PROJECT_NAME),
				rec.get(FIELD_DESCRIPTION),
				rec.get(FIELD_ORGANIZATION_NAME),
				List.of()
			))
			.toList();
	}


	@Override
	public PlanID persistPlan(Project project, Plan plan) {
		dsl.insertInto(TABLE_PLAN)
			.set(FIELD_PLAN_ID, plan.planID().UUID())
			.set(FIELD_ORGANIZATION_NAME, project.organization())
			.set(FIELD_PROJECT_NAME, project.name())
			.set(FIELD_CREATED_AT, plan.createdAt() != null
				? LocalDateTime.ofInstant(plan.createdAt(), ZoneOffset.UTC)
				: LocalDateTime.now(ZoneOffset.UTC))
			.set(FIELD_RESOURCE_SET_HASH, plan.resourceSetHash())
			.set(FIELD_CONFIGURATION_HASH, plan.configurationHash())
			.set(FIELD_PLAN_NODE_ROOT, plan.planNodeRoot() != null ? plan.planNodeRoot().UUID() : null)
			.execute();
		return plan.planID();
	}


	@Override
	public Optional<Plan> getPlan(PlanID planID) {
		return dsl.select(
				FIELD_PLAN_ID, FIELD_CREATED_AT, FIELD_RESOURCE_SET_HASH,
				FIELD_CONFIGURATION_HASH, FIELD_PLAN_NODE_ROOT
			)
			.from(TABLE_PLAN)
			.where(FIELD_PLAN_ID.eq(planID.UUID()))
			.fetchOptional()
			.map(this::mapPlan);
	}


	@Override
	public List<Plan> getPlansForProject(String organizationName, String projectName) {
		return dsl.select(
				FIELD_PLAN_ID, FIELD_CREATED_AT, FIELD_RESOURCE_SET_HASH,
				FIELD_CONFIGURATION_HASH, FIELD_PLAN_NODE_ROOT
			)
			.from(TABLE_PLAN)
			.where(FIELD_ORGANIZATION_NAME.eq(organizationName).and(FIELD_PROJECT_NAME.eq(projectName)))
			.fetch().stream()
			.map(this::mapPlan)
			.toList();
	}


	@Override
	public void deletePlan(PlanID planID) {
		dsl.deleteFrom(TABLE_PLAN)
			.where(FIELD_PLAN_ID.eq(planID.UUID()))
			.execute();
	}


	@Override
	public void persistTestSuite(Project project, Plan plan, TestSuite testSuite) {
		dsl.mergeInto(TABLE_SUITE)
			.using(DSL.selectOne())
			.on(FIELD_PLAN_ID.eq(plan.planID().UUID()).and(FIELD_NAME.eq(testSuite.name())))
			.whenMatchedThenUpdate()
			.set(FIELD_TAG_EXPRESSION, testSuite.tagExpression().toString())
			.set(FIELD_DESCRIPTION, testSuite.description())
			.whenNotMatchedThenInsert(FIELD_PLAN_ID, FIELD_NAME, FIELD_TAG_EXPRESSION, FIELD_DESCRIPTION)
			.values(plan.planID().UUID(), testSuite.name(), testSuite.tagExpression().toString(), testSuite.description())
			.execute();
	}


	@Override
	public List<TestSuite> getTestSuites(PlanID planID) {
		return dsl.select(FIELD_NAME, FIELD_TAG_EXPRESSION, FIELD_DESCRIPTION)
			.from(TABLE_SUITE)
			.where(FIELD_PLAN_ID.eq(planID.UUID()))
			.fetch().stream()
			.map(rec -> new TestSuite(
				rec.get(FIELD_NAME),
				rec.get(FIELD_DESCRIPTION),
				TagExpression.parse(rec.get(FIELD_TAG_EXPRESSION))
			))
			.toList();
	}


	@Override
	public void deleteTestSuite(PlanID planID, String testSuiteName) {
		dsl.deleteFrom(TABLE_SUITE)
			.where(FIELD_PLAN_ID.eq(planID.UUID()).and(FIELD_NAME.eq(testSuiteName)))
			.execute();
	}

	@Override
	public void deleteTestSuites(PlanID planID) {
		dsl.deleteFrom(TABLE_SUITE)
			.where(FIELD_PLAN_ID.eq(planID.UUID()))
			.execute();
	}


	private Plan mapPlan(Record rec) {
		UUID planNodeRootUUID = rec.get(FIELD_PLAN_NODE_ROOT);
		return new Plan(
			new PlanID(rec.get(FIELD_PLAN_ID)),
			rec.get(FIELD_CREATED_AT) != null
				? rec.get(FIELD_CREATED_AT).toInstant(ZoneOffset.UTC)
				: null,
			rec.get(FIELD_RESOURCE_SET_HASH),
			rec.get(FIELD_CONFIGURATION_HASH),
			planNodeRootUUID != null ? new PlanNodeID(planNodeRootUUID) : null
		);
	}

}
