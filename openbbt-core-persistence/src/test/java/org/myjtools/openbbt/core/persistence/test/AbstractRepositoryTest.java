package org.myjtools.openbbt.core.persistence.test;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.persistence.JooqRepository;
import org.myjtools.openbbt.core.plan.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractRepositoryTest {

	protected JooqRepository repo;

	protected abstract DSLContext createDSLContext();

	@BeforeEach
	void setUp() {
		repo = new JooqRepository(createDSLContext());
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

		PlanNode retrieved = repo.getNodeData(id).orElseThrow();

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
		PlanNode retrieved = repo.getNodeData(id).orElseThrow();
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

		PlanNode toUpdate = repo.getNodeData(id).orElseThrow();
		toUpdate.name("updated name");
		toUpdate.description("updated description");
		toUpdate.tags().add("new-tag");

		repo.persistNode(toUpdate);

		PlanNode updated = repo.getNodeData(id).orElseThrow();
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
		assertThat(repo.getNodeData(id)).isEmpty();
	}

	@Test
	void attachChildNodeLast_buildsTreeHierarchy() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		PlanNodeID child2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		PlanNodeID child3 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child3"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);
		repo.attachChildNodeLast(root, child3);

		List<PlanNodeID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child1, child2, child3);

		assertThat(repo.getParentNode(child1)).contains(root);
		assertThat(repo.getParentNode(child2)).contains(root);
		assertThat(repo.getParentNode(child3)).contains(root);

		assertThat(repo.getRootNode(child1)).contains(root);
		assertThat(repo.getRootNode(child2)).contains(root);
		assertThat(repo.getRootNode(child3)).contains(root);
	}

	@Test
	void attachChildNodeFirst_insertsAtBeginning() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		PlanNodeID child2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		PlanNodeID child3 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child3"));

		repo.attachChildNodeFirst(root, child1);
		repo.attachChildNodeFirst(root, child2);
		repo.attachChildNodeFirst(root, child3);

		List<PlanNodeID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child3, child2, child1);
	}

	@Test
	void detachChildNode_removesFromParent() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		PlanNodeID child2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child2"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);

		repo.detachChildNode(root, child1);

		List<PlanNodeID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child2);
		assertThat(repo.getParentNode(child1)).isEmpty();
		assertThat(repo.getRootNode(child1)).contains(child1);
	}

	@Test
	void deepTreeHierarchy_threeLevels() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID level1a = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("level1a"));
		PlanNodeID level1b = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("level1b"));
		PlanNodeID level2a = repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("level2a"));
		PlanNodeID level2b = repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("level2b"));

		repo.attachChildNodeLast(root, level1a);
		repo.attachChildNodeLast(root, level1b);
		repo.attachChildNodeLast(level1a, level2a);
		repo.attachChildNodeLast(level1a, level2b);

		assertThat(repo.getNodeChildren(root).toList()).containsExactly(level1a, level1b);
		assertThat(repo.getNodeChildren(level1a).toList()).containsExactly(level2a, level2b);
		assertThat(repo.getNodeChildren(level1b).toList()).isEmpty();

		assertThat(repo.getRootNode(level2a)).contains(root);
		assertThat(repo.getRootNode(level2b)).contains(root);
		assertThat(repo.getParentNode(level2a)).contains(level1a);
	}

	@Test
	void moveNodeBetweenParents() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID parent1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("parent1"));
		PlanNodeID parent2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("parent2"));
		PlanNodeID child = repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("child"));

		repo.attachChildNodeLast(root, parent1);
		repo.attachChildNodeLast(root, parent2);
		repo.attachChildNodeLast(parent1, child);

		assertThat(repo.getNodeChildren(parent1).toList()).containsExactly(child);
		assertThat(repo.getNodeChildren(parent2).toList()).isEmpty();

		repo.detachChildNode(parent1, child);
		repo.attachChildNodeLast(parent2, child);

		assertThat(repo.getNodeChildren(parent1).toList()).isEmpty();
		assertThat(repo.getNodeChildren(parent2).toList()).containsExactly(child);
		assertThat(repo.getParentNode(child)).contains(parent2);
	}

	@Test
	void mixedAttachOperations_maintainsCorrectOrder() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		PlanNodeID child2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		PlanNodeID child3 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child3"));
		PlanNodeID child4 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child4"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeFirst(root, child2);
		repo.attachChildNodeLast(root, child3);
		repo.attachChildNodeFirst(root, child4);

		List<PlanNodeID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child4, child2, child1, child3);
	}

}