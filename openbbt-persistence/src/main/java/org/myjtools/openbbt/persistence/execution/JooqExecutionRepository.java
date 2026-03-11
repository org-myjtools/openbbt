package org.myjtools.openbbt.persistence.execution;

import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.util.UUIDGenerator;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public class JooqExecutionRepository implements TestExecutionRepository, AutoCloseable {

	private static final Table<Record> TABLE_EXECUTION = DSL.table("execution");
	private static final Table<Record> TABLE_EXECUTION_NODE = DSL.table("execution_node");
	private static final Table<Record> TABLE_EXECUTION_ATTACHMENT = DSL.table("execution_attachment");

	private static final Field<UUID> FIELD_EXECUTION_ID = DSL.field("execution_id", UUID.class);
	private static final Field<UUID> FIELD_PLAN_ID = DSL.field("plan_id", UUID.class);
	private static final Field<LocalDateTime> FIELD_EXECUTED_AT = DSL.field("executed_at", LocalDateTime.class);

	private static final Field<UUID> FIELD_EXECUTION_NODE_ID = DSL.field("execution_node_id", UUID.class);
	private static final Field<UUID> FIELD_PLAN_NODE_ID = DSL.field("plan_node_id", UUID.class);
	private static final Field<LocalDateTime> FIELD_STARTED_AT = DSL.field("started_at", LocalDateTime.class);
	private static final Field<LocalDateTime> FIELD_FINISHED_AT = DSL.field("finished_at", LocalDateTime.class);
	private static final Field<Integer> FIELD_RESULT = DSL.field("result", Integer.class);
	private static final Field<String> FIELD_MESSAGE = DSL.field("message", String.class);

	private static final Field<UUID> FIELD_ATTACHMENT_ID = DSL.field("attachment_id", UUID.class);

	private final DSLContext dsl;
	private final Connection directConnection;


	public JooqExecutionRepository(DataSourceProvider dataSourceProvider) {
		this(dataSourceProvider.obtainDataSource(), dataSourceProvider.dialect());
	}

	public JooqExecutionRepository(DataSource dataSource, SQLDialect dialect) {
		this.dsl = DSL.using(new DataSourceConnectionProvider(dataSource), dialect);
		this.directConnection = null;
	}

	public JooqExecutionRepository(Connection connection, SQLDialect dialect) {
		this.dsl = DSL.using(connection, dialect);
		this.directConnection = connection;
	}

	@Override
	public void close() {
		if (directConnection != null) {
			try { directConnection.close(); } catch (SQLException ignored) {}
		}
	}


	@Override
	public TestExecution newExecution(UUID planID, Instant executedAt) {
		UUID id = UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_EXECUTION)
		   .set(FIELD_EXECUTION_ID, id)
		   .set(FIELD_PLAN_ID, planID)
		   .set(FIELD_EXECUTED_AT, LocalDateTime.ofInstant(executedAt, ZoneOffset.UTC))
		   .execute();
		TestExecution execution = new TestExecution();
		execution.executionID(id);
		execution.planID(planID);
		execution.executedAt(executedAt);
		return execution;
	}


	@Override
	public UUID newExecutionNode(UUID executionID, UUID testPlanNodeID) {
		UUID id = UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_EXECUTION_NODE)
		   .set(FIELD_EXECUTION_NODE_ID, id)
		   .set(FIELD_EXECUTION_ID, executionID)
		   .set(FIELD_PLAN_NODE_ID, testPlanNodeID)
		   .execute();
		return id;
	}


	@Override
	public Optional<UUID> getExecutionNodeByPlanNode(UUID executionID, UUID testPlanNodeID) {
		return dsl.select(FIELD_EXECUTION_NODE_ID)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_ID.eq(executionID))
			.and(FIELD_PLAN_NODE_ID.eq(testPlanNodeID))
			.fetchOptional(FIELD_EXECUTION_NODE_ID);
	}


	@Override
	public void updateExecutionNodeStart(UUID executionNodeID, Instant startedAt) {
		dsl.update(TABLE_EXECUTION_NODE)
		   .set(FIELD_STARTED_AT, LocalDateTime.ofInstant(startedAt, ZoneOffset.UTC))
		   .where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
		   .execute();
	}


	@Override
	public void updateExecutionNodeFinish(UUID executionNodeID, ExecutionResult result, Instant finishedAt) {
		dsl.update(TABLE_EXECUTION_NODE)
		   .set(FIELD_RESULT, result.value())
		   .set(FIELD_FINISHED_AT, LocalDateTime.ofInstant(finishedAt, ZoneOffset.UTC))
		   .where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
		   .execute();
	}


	@Override
	public void updateExecutionNodeMessage(UUID executionNodeID, String message) {
		dsl.update(TABLE_EXECUTION_NODE)
		   .set(FIELD_MESSAGE, message)
		   .where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
		   .execute();
	}


	public void clearAllData() {
		dsl.deleteFrom(TABLE_EXECUTION_ATTACHMENT).execute();
		dsl.deleteFrom(TABLE_EXECUTION_NODE).execute();
		dsl.deleteFrom(TABLE_EXECUTION).execute();
	}


	public Optional<Instant> getExecutionNodeStartedAt(UUID executionNodeID) {
		return dsl.select(FIELD_STARTED_AT)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_STARTED_AT)
			.map(ldt -> ldt.toInstant(ZoneOffset.UTC));
	}


	public Optional<Instant> getExecutionNodeFinishedAt(UUID executionNodeID) {
		return dsl.select(FIELD_FINISHED_AT)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_FINISHED_AT)
			.map(ldt -> ldt.toInstant(ZoneOffset.UTC));
	}


	public Optional<ExecutionResult> getExecutionNodeResult(UUID executionNodeID) {
		return dsl.select(FIELD_RESULT)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_RESULT)
			.map(v -> ExecutionResult.of(v));
	}


	public Optional<String> getExecutionNodeMessage(UUID executionNodeID) {
		return dsl.select(FIELD_MESSAGE)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_MESSAGE);
	}


	public boolean existsAttachment(UUID attachmentID) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_EXECUTION_ATTACHMENT)
			   .where(FIELD_ATTACHMENT_ID.eq(attachmentID))
		);
	}


	@Override
	public UUID newAttachment(UUID executionNodeID) {
		UUID executionID = dsl.select(FIELD_EXECUTION_ID)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOne(FIELD_EXECUTION_ID);
		UUID id = UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_EXECUTION_ATTACHMENT)
		   .set(FIELD_ATTACHMENT_ID, id)
		   .set(FIELD_EXECUTION_NODE_ID, executionNodeID)
		   .set(FIELD_EXECUTION_ID, executionID)
		   .execute();
		return id;
	}

}