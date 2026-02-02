package org.myjtools.openbbt.core.persistence.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.persistence.HsqldbPlanNodeRepository;
import org.myjtools.openbbt.core.plan.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class HsqldbPlanNodeRepositoryTest {

	private PlanNodeRepository repo;

	@BeforeEach
	void setUp() {
		repo = new HsqldbPlanNodeRepository();
	}

	@Test
	void insertPlanNodeWithAllFields() {
		DataTable dataTable = new DataTable(List.of(
			List.of("col1", "col2"),
			List.of("val1", "val2")
		));
		Document document = Document.of("text/plain", "document content");

		PlanNode node = new PlanNode()
			.nodeType(NodeType.TEST_PLAN)
			.name("test name")
			.language("en")
			.testCaseID("TC-001")
			.source("test.feature")
			.keyword("Scenario")
			.description("test description")
			.display("Test Display")
			.dataTable(dataTable)
			.document(document)
			.tags(new HashSet<>(Set.of("smoke", "regression")))
			.properties(new TreeMap<>(Map.of("priority", "high", "author", "tester")));

		PlanNodeID id = repo.persistNode(node);

		assertThat(id).isNotNull();
		assertThat(repo.existsNode(id)).isTrue();

		PlanNode retrieved = repo.getNode(id).orElseThrow();

		assertThat(retrieved.nodeType()).isEqualTo(NodeType.TEST_PLAN);
		assertThat(retrieved.name()).isEqualTo("test name");
		assertThat(retrieved.language()).isEqualTo("en");
		assertThat(retrieved.testCaseID()).isEqualTo("TC-001");
		assertThat(retrieved.source()).isEqualTo("test.feature");
		assertThat(retrieved.keyword()).isEqualTo("Scenario");
		assertThat(retrieved.description()).isEqualTo("test description");
		assertThat(retrieved.display()).isEqualTo("Test Display");
		assertThat(retrieved.dataTable()).isEqualTo(dataTable);
		assertThat(retrieved.document().content()).isEqualTo("document content");
		assertThat(retrieved.document().mimeType()).isEqualTo("text/plain");
		assertThat(retrieved.tags()).containsExactlyInAnyOrder("smoke", "regression");
		assertThat(retrieved.properties()).containsEntry("priority", "high");
		assertThat(retrieved.properties()).containsEntry("author", "tester");
	}

	@Test
	void insertPlanNodeWithMinimalFields() {
		PlanNode node = new PlanNode()
			.nodeType(NodeType.STEP)
			.name("minimal node");

		PlanNodeID id = repo.persistNode(node);

		assertThat(id).isNotNull();
		PlanNode retrieved = repo.getNode(id).orElseThrow();
		assertThat(retrieved.name()).isEqualTo("minimal node");
		assertThat(retrieved.nodeType()).isEqualTo(NodeType.STEP);
		assertThat(retrieved.description()).isNull();
		assertThat(retrieved.dataTable()).isNull();
		assertThat(retrieved.document()).isNull();
	}

	@Test
	void updatePlanNode() {
		PlanNode node = new PlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("original name")
			.description("original description");

		PlanNodeID id = repo.persistNode(node);

		PlanNode toUpdate = repo.getNode(id).orElseThrow();
		toUpdate.name("updated name");
		toUpdate.description("updated description");
		toUpdate.tags().add("new-tag");

		repo.persistNode(toUpdate);

		PlanNode updated = repo.getNode(id).orElseThrow();
		assertThat(updated.name()).isEqualTo("updated name");
		assertThat(updated.description()).isEqualTo("updated description");
		assertThat(updated.tags()).contains("new-tag");
	}

	@Test
	void deletePlanNode() {
		PlanNode node = new PlanNode()
			.nodeType(NodeType.STEP)
			.name("to delete");

		PlanNodeID id = repo.persistNode(node);
		assertThat(repo.existsNode(id)).isTrue();

		repo.deleteNode(id);

		assertThat(repo.existsNode(id)).isFalse();
		assertThat(repo.getNode(id)).isEmpty();
	}

}
