package org.myjtools.openbbt.core.persistence.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.persistence.DataSourceProvider;
import org.myjtools.openbbt.core.persistence.JooqRepository;
import org.myjtools.openbbt.core.plan.*;
import javax.sql.DataSource;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractRepositoryTest {

	protected JooqRepository repo;
	private DataSource dataSource;

	protected abstract DataSourceProvider dataSourceProvider();

	@BeforeEach
	void setUp() {
		DataSourceProvider provider = dataSourceProvider();
		dataSource = provider.obtainDataSource();
		repo = new JooqRepository(dataSource, provider.dialect());
		repo.clearAllData();
	}

	@AfterEach
	void tearDown() {
		if (dataSource instanceof HikariDataSource hikari) {
			hikari.close();
		}
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
			.identifier("TC-001")
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
		assertThat(retrieved.identifier()).isEqualTo("TC-001");
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
	void updateAndRetrieveFields() {
		PlanNode node = new PlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("original name")
			.description("original description");

		PlanNodeID id = repo.persistNode(node);

		String retrievedName = repo.getNodeField(id, "name").orElseThrow().toString();
		assertThat(retrievedName).isEqualTo("original name");
		repo.updateNodeField(id, "name", "updated name");
		String updatedName = repo.getNodeField(id, "name").orElseThrow().toString();
		assertThat(updatedName).isEqualTo("updated name");
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

	@Test
	void deleteNodeWithChildren_cascadesDeletion() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		PlanNodeID child2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child2"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);

		repo.deleteNode(root);

		assertThat(repo.existsNode(root)).isFalse();
		assertThat(repo.existsNode(child1)).isFalse();
		assertThat(repo.existsNode(child2)).isFalse();
	}

	@Test
	void deleteNodeWithMultipleParents_doesNotDeleteSharedNode() {
		PlanNodeID root1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root1"));
		PlanNodeID root2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root2"));
		PlanNodeID sharedChild = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("sharedChild"));

		repo.attachChildNodeLast(root1, sharedChild);
		repo.attachChildNodeLast(root2, sharedChild);

		repo.deleteNode(root1);

		assertThat(repo.existsNode(root1)).isFalse();
		assertThat(repo.existsNode(sharedChild)).isTrue();
		assertThat(repo.getParentNode(sharedChild)).contains(root2);
	}

	@Test
	void countNodeChildren_returnsCorrectCount() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		assertThat(repo.countNodeChildren(root)).isZero();

		PlanNodeID child1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		PlanNodeID child2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);

		assertThat(repo.countNodeChildren(root)).isEqualTo(2);
	}

	@Test
	void getNodeDescendants_returnsAllDescendants() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child"));
		PlanNodeID grandchild = repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("grandchild"));

		repo.attachChildNodeLast(root, child);
		repo.attachChildNodeLast(child, grandchild);

		List<PlanNodeID> descendants = repo.getNodeDescendants(root).toList();
		assertThat(descendants).containsExactlyInAnyOrder(child, grandchild);
	}

	@Test
	void getNodeDescendants_emptyForLeafNode() {
		PlanNodeID leaf = repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("leaf"));
		assertThat(repo.getNodeDescendants(leaf).toList()).isEmpty();
	}

	@Test
	void countNodeDescendants_returnsCorrectCount() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child"));
		PlanNodeID grandchild = repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("grandchild"));

		repo.attachChildNodeLast(root, child);
		repo.attachChildNodeLast(child, grandchild);

		assertThat(repo.countNodeDescendants(root)).isEqualTo(2);
		assertThat(repo.countNodeDescendants(child)).isEqualTo(1);
		assertThat(repo.countNodeDescendants(grandchild)).isZero();
	}

	@Test
	void getNodeAncestors_returnsAllAncestors() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child"));
		PlanNodeID grandchild = repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("grandchild"));

		repo.attachChildNodeLast(root, child);
		repo.attachChildNodeLast(child, grandchild);

		List<PlanNodeID> ancestors = repo.getNodeAncestors(grandchild).toList();
		assertThat(ancestors).containsExactlyInAnyOrder(child, root);
	}

	@Test
	void getNodeAncestors_emptyForRootNode() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		assertThat(repo.getNodeAncestors(root).toList()).isEmpty();
	}

	@Test
	void existsTag_returnsTrueForExistingTag() {
		PlanNode node = new PlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("tagged")
			.tags(new HashSet<>(Set.of("smoke", "regression")));
		PlanNodeID id = repo.persistNode(node);

		assertThat(repo.existsTag(id, "smoke")).isTrue();
		assertThat(repo.existsTag(id, "regression")).isTrue();
		assertThat(repo.existsTag(id, "nonexistent")).isFalse();
	}

	@Test
	void existsProperty_returnsTrueForExistingProperty() {
		PlanNode node = new PlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("with props")
			.properties(new TreeMap<>(Map.of("priority", "high", "author", "tester")));
		PlanNodeID id = repo.persistNode(node);

		assertThat(repo.existsProperty(id, "priority", "high")).isTrue();
		assertThat(repo.existsProperty(id, "priority", "low")).isFalse();
		assertThat(repo.existsProperty(id, "priority", null)).isTrue();
		assertThat(repo.existsProperty(id, "missing", null)).isFalse();
	}

	@Test
	void getNodeProperty_returnsValue() {
		PlanNode node = new PlanNode()
			.nodeType(NodeType.TEST_CASE)
			.name("with props")
			.properties(new TreeMap<>(Map.of("priority", "high")));
		PlanNodeID id = repo.persistNode(node);

		assertThat(repo.getProperty(id, "priority")).contains("high");
		assertThat(repo.getProperty(id, "missing")).isEmpty();
	}

	@Test
	void searchNodes_byNodeType() {
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("plan"));
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("case1"));
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("case2"));
		repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("step"));

		List<PlanNodeID> results = repo.searchNodes(
			org.myjtools.openbbt.core.PlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
		).toList();
		assertThat(results).hasSize(2);
	}

	@Test
	void searchNodes_byTag() {
		PlanNode tagged = new PlanNode().nodeType(NodeType.TEST_CASE).name("tagged")
			.tags(new HashSet<>(Set.of("smoke")));
		PlanNode untagged = new PlanNode().nodeType(NodeType.TEST_CASE).name("untagged");
		repo.persistNode(tagged);
		repo.persistNode(untagged);

		List<PlanNodeID> results = repo.searchNodes(
			org.myjtools.openbbt.core.PlanNodeCriteria.withTag("smoke")
		).toList();
		assertThat(results).hasSize(1);
	}

	@Test
	void searchNodes_byProperty() {
		PlanNode withProp = new PlanNode().nodeType(NodeType.TEST_CASE).name("with")
			.properties(new TreeMap<>(Map.of("env", "prod")));
		PlanNode without = new PlanNode().nodeType(NodeType.TEST_CASE).name("without");
		repo.persistNode(withProp);
		repo.persistNode(without);

		var criteria = org.myjtools.openbbt.core.PlanNodeCriteria.class;
		assertThat(repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.withProperty("env", "prod")).toList()).hasSize(1);
		assertThat(repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.withProperty("env", null)).toList()).hasSize(1);
		assertThat(repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.withProperty("env", "dev")).toList()).isEmpty();
	}

	@Test
	void searchNodes_byField() {
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("with lang").language("en"));
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("no lang"));

		assertThat(repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.withField("language", "en")).toList()).hasSize(1);
		assertThat(repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.withField("language")).toList()).hasSize(1);
	}

	@Test
	void searchNodes_withAndOrNot() {
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("case")
			.tags(new HashSet<>(Set.of("smoke"))));
		repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("step")
			.tags(new HashSet<>(Set.of("smoke"))));
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("case2"));

		var C = org.myjtools.openbbt.core.PlanNodeCriteria.class;

		List<PlanNodeID> andResult = repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.and(
			org.myjtools.openbbt.core.PlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			org.myjtools.openbbt.core.PlanNodeCriteria.withTag("smoke")
		)).toList();
		assertThat(andResult).hasSize(1);

		List<PlanNodeID> orResult = repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.or(
			org.myjtools.openbbt.core.PlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			org.myjtools.openbbt.core.PlanNodeCriteria.withNodeType(NodeType.STEP)
		)).toList();
		assertThat(orResult).hasSize(3);

		List<PlanNodeID> notResult = repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.not(
			org.myjtools.openbbt.core.PlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
		)).toList();
		assertThat(notResult).hasSize(1);
	}

	@Test
	void searchNodes_all() {
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("a"));
		repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("b"));

		assertThat(repo.searchNodes(org.myjtools.openbbt.core.PlanNodeCriteria.all()).toList()).hasSize(2);
	}

	@Test
	void countNodes_returnsCorrectCount() {
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("case1"));
		repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("case2"));
		repo.persistNode(new PlanNode().nodeType(NodeType.STEP).name("step"));

		assertThat(repo.countNodes(org.myjtools.openbbt.core.PlanNodeCriteria.all())).isEqualTo(3);
		assertThat(repo.countNodes(org.myjtools.openbbt.core.PlanNodeCriteria.withNodeType(NodeType.TEST_CASE))).isEqualTo(2);
		assertThat(repo.countNodes(org.myjtools.openbbt.core.PlanNodeCriteria.withNodeType(NodeType.STEP))).isEqualTo(1);
	}

	@Test
	void reattachNodeToSameParent_maintainsOrder() {
		PlanNodeID root = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		PlanNodeID child1 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child1"));
		PlanNodeID child2 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child2"));
		PlanNodeID child3 = repo.persistNode(new PlanNode().nodeType(NodeType.TEST_CASE).name("child3"));

		repo.attachChildNodeLast(root, child1);
		repo.attachChildNodeLast(root, child2);
		repo.attachChildNodeLast(root, child3);

		repo.detachChildNode(root, child2);
		repo.attachChildNodeLast(root, child2);

		List<PlanNodeID> children = repo.getNodeChildren(root).toList();
		assertThat(children).containsExactly(child1, child3, child2);
	}

}