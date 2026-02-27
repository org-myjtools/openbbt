package org.myjtools.openbbt.persistence.plan;

import java.util.UUID;
import com.github.f4b6a3.ulid.UlidCreator;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.persistence.PlanNodeCriteria;
import org.myjtools.openbbt.core.persistence.PlanRepository;
import org.myjtools.openbbt.core.plan.*;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class JooqPlanRepository implements PlanRepository {

	private static final Table<Record> TABLE_PLAN_NODE = DSL.table("plan_node");
	private static final Table<Record> TABLE_PLAN_NODE_TAG = DSL.table("plan_node_tag");
	private static final Table<Record> TABLE_PLAN_NODE_PROPERTY = DSL.table("plan_node_property");

	private static final Field<UUID> FIELD_NODE_ID = DSL.field("node_id", UUID.class);
	private static final Field<UUID> FIELD_PARENT_NODE = DSL.field("parent_node", UUID.class);
	private static final Field<UUID> FIELD_PLAN_NODE = DSL.field("plan_node", UUID.class);
	private static final Field<String> FIELD_TAG = DSL.field("tag", String.class);
	private static final Field<String> FIELD_KEY = DSL.field("key", String.class);
	private static final Field<String> FIELD_VALUE = DSL.field("value", String.class);
	private static final Field<Integer> FIELD_NODE_POSITION = DSL.field("node_position", Integer.class);
	private static final Field<Integer> FIELD_TYPE = DSL.field("type", Integer.class);
	private static final Field<String> FIELD_NAME = DSL.field("name", String.class);
	private static final Field<String> FIELD_IDENTIFIER = DSL.field("identifier", String.class);
	private static final Field<String> FIELD_LANGUAGE = DSL.field("language", String.class);
	private static final Field<String> FIELD_SOURCE = DSL.field("source", String.class);
	private static final Field<String> FIELD_DISPLAY = DSL.field("display", String.class);
	private static final Field<String> FIELD_KEYWORD = DSL.field("keyword", String.class);
	private static final Field<String> FIELD_DESCRIPTION = DSL.field("description", String.class);
	private static final Field<String> FIELD_DATA_TABLE = DSL.field("data_table", String.class);
	private static final Field<String> FIELD_DOCUMENT = DSL.field("document", String.class);
	private static final Field<String> FIELD_DOCUMENT_MIME_TYPE = DSL.field("document_mime_type", String.class);

	private final DSLContext dsl;


	public JooqPlanRepository(DataSourceProvider dataSourceProvider) {
		this(dataSourceProvider.obtainDataSource(), dataSourceProvider.dialect());
	}

	public JooqPlanRepository(DataSource dataSource, SQLDialect dialect) {
		this.dsl = DSL.using(new DataSourceConnectionProvider(dataSource), dialect);
	}

	public void clearAllData() {
		dsl.deleteFrom(TABLE_PLAN_NODE_TAG).execute();
		dsl.deleteFrom(TABLE_PLAN_NODE_PROPERTY).execute();
		dsl.deleteFrom(TABLE_PLAN_NODE).execute();
	}


	public Optional<PlanNode> getNodeData(UUID id) {
		return dsl.select(
				FIELD_NODE_ID, FIELD_PARENT_NODE, FIELD_NODE_POSITION,
				FIELD_TYPE, FIELD_NAME, FIELD_IDENTIFIER, FIELD_LANGUAGE, FIELD_SOURCE,
				FIELD_KEYWORD, FIELD_DESCRIPTION, FIELD_DISPLAY, FIELD_DATA_TABLE,
				FIELD_DOCUMENT, FIELD_DOCUMENT_MIME_TYPE
			)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_NODE_ID.eq(id))
			.fetchOptional()
			.map(this::mapPlanNode);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void updateNodeField(UUID id, String fieldName, T fieldValue) {
		Field<T> field = (Field<T>) resolveField(fieldName);
		dsl.update(TABLE_PLAN_NODE)
		   .set(field, fieldValue)
		   .where(FIELD_NODE_ID.eq(id))
		   .execute();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getNodeField(UUID id, String fieldName) {
		Field<T> field = (Field<T>) resolveField(fieldName);
		return dsl.select(field)
		   .from(TABLE_PLAN_NODE)
		   .where(FIELD_NODE_ID.eq(id))
		   .fetchOptional(field);
	}



	public boolean existsNode(UUID id) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE)
			   .where(FIELD_NODE_ID.eq(id))
		);
	}


	public Optional<UUID> getParentNode(UUID id) {
		assertExistsNode(id);
		return dsl.select(FIELD_PARENT_NODE)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_NODE_ID.eq(id))
			.fetchOptional()
			.map(record1 -> record1.get(FIELD_PARENT_NODE))
			;
	}


	public void deleteNode(UUID id) {
		// first detach from parent if exists
		getParentNode(id).ifPresent(parent -> detachChildNode(parent, id));
		// delete node (cascade should handle hierarchy, tags, properties)
		dsl.deleteFrom(TABLE_PLAN_NODE)
		   .where(FIELD_NODE_ID.eq(id))
		   .execute();
	}


	public void attachChildNodeLast(UUID parent, UUID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_PARENT_NODE, parent)
		   .set(FIELD_NODE_POSITION, maxNodePosition(parent) + 1)
		   .where(FIELD_NODE_ID.eq(child))
		   .execute();
	}


	public void attachChildNodeFirst(UUID parent, UUID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		// increment position of existing child nodes
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_NODE_POSITION, FIELD_NODE_POSITION.add(1))
			.where(FIELD_PARENT_NODE.eq(parent))
			.execute();
		// set child node as first
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_PARENT_NODE, parent)
			.set(FIELD_NODE_POSITION, 1)
			.where(FIELD_NODE_ID.eq(child))
			.execute();
	}


	public void detachChildNode(UUID parent, UUID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_PARENT_NODE, (UUID) null)
			.where(FIELD_NODE_ID.eq(child))
			.execute();
	}




	public Stream<UUID> getNodeChildren(UUID id) {
		assertExistsNode(id);
		return dsl.select(FIELD_NODE_ID)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(id))
			.orderBy(FIELD_NODE_POSITION)
			.fetch().stream()
			.map(record1 -> mapUUID(record1, FIELD_NODE_ID));
	}


	public int countNodeChildren(UUID id) {
		assertExistsNode(id);
		Integer count = dsl.selectCount()
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(id))
			.fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	private static final Table<?> CTE_DESC = DSL.table(DSL.unquotedName("descendants"));
	private static final Table<?> CTE_ANCS = DSL.table(DSL.unquotedName("ancestors"));
	private static final Field<UUID> CTE_NID = DSL.field(DSL.unquotedName("nid"), UUID.class);
	private static final Field<UUID> CTE_PID = DSL.field(DSL.unquotedName("pid"), UUID.class);

	public Stream<UUID> getNodeDescendants(UUID id) {
		assertExistsNode(id);
		return dsl.withRecursive(DSL.unquotedName("descendants"), DSL.unquotedName("nid")).as(
			   DSL.select(FIELD_NODE_ID)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_PARENT_NODE.eq(id))
			   .unionAll(
				   DSL.select(FIELD_NODE_ID)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_DESC)
					   .on(FIELD_PARENT_NODE.eq(CTE_NID))
			   )
		   )
		   .select(CTE_NID)
		   .from(CTE_DESC)
		   .fetch().stream()
		   .map(rec -> rec.get(CTE_NID));
	}


	public int countNodeDescendants(UUID id) {
		assertExistsNode(id);
		Integer count = dsl.withRecursive(DSL.unquotedName("descendants"), DSL.unquotedName("nid")).as(
			   DSL.select(FIELD_NODE_ID)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_PARENT_NODE.eq(id))
			   .unionAll(
				   DSL.select(FIELD_NODE_ID)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_DESC)
					   .on(FIELD_PARENT_NODE.eq(CTE_NID))
			   )
		   )
		   .selectCount()
		   .from(CTE_DESC)
		   .fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	public int countNodeAncestors(UUID id) {
		assertExistsNode(id);
		Integer count = dsl.withRecursive(DSL.unquotedName("ancestors"), DSL.unquotedName("pid")).as(
			   DSL.select(FIELD_PARENT_NODE)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_NODE_ID.eq(id))
			   .unionAll(
				   DSL.select(FIELD_PARENT_NODE)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_ANCS)
					   .on(FIELD_NODE_ID.eq(CTE_PID))
			   )
		   )
		   .selectCount()
		   .from(CTE_ANCS)
		   .where(CTE_PID.isNotNull())
		   .fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	public Stream<UUID> getNodeAncestors(UUID id) {
		assertExistsNode(id);
		return dsl.withRecursive(DSL.unquotedName("ancestors"), DSL.unquotedName("pid")).as(
			   DSL.select(FIELD_PARENT_NODE)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_NODE_ID.eq(id))
			   .unionAll(
				   DSL.select(FIELD_PARENT_NODE)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_ANCS)
					   .on(FIELD_NODE_ID.eq(CTE_PID))
			   )
		   )
		   .select(CTE_PID)
		   .from(CTE_ANCS)
		   .where(CTE_PID.isNotNull())
		   .fetch().stream()
		   .map(rec -> rec.get(CTE_PID));
	}


	public UUID persistNode(PlanNode node) {
		boolean isUpdate = node.nodeID() != null;
		UUID id;
		if (isUpdate) {
			id = node.nodeID();
			assertExistsNode(id);
			updateNode(node);
		} else {
			id = generateUUID();
			node.nodeID(id);
			insertNode(node);
		}
		syncTags(id, node.tags());
		syncProperties(id, node.properties());
		return id;
	}


	private void insertNode(PlanNode node) {
		dsl.insertInto(TABLE_PLAN_NODE)
		   .set(FIELD_NODE_ID, node.nodeID())
		   .set(FIELD_PARENT_NODE, (UUID) null)
		   .set(FIELD_NODE_POSITION, 1)
		   .set(FIELD_TYPE, node.nodeType() != null ? node.nodeType().value : null)
		   .set(FIELD_NAME, node.name())
		   .set(FIELD_IDENTIFIER, node.identifier())
		   .set(FIELD_LANGUAGE, node.language())
		   .set(FIELD_SOURCE, node.source())
		   .set(FIELD_KEYWORD, node.keyword())
		   .set(FIELD_DESCRIPTION, node.description())
		   .set(FIELD_DISPLAY, node.display())
		   .set(FIELD_DATA_TABLE, node.dataTable() != null ? node.dataTable().toString() : null)
		   .set(FIELD_DOCUMENT, node.document() != null ? node.document().content() : null)
		   .set(FIELD_DOCUMENT_MIME_TYPE, node.document() != null ? node.document().mimeType() : null)
		   .execute();
	}


	private void updateNode(PlanNode node) {
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_TYPE, node.nodeType() != null ? node.nodeType().value : null)
		   .set(FIELD_NAME, node.name())
		   .set(FIELD_IDENTIFIER, node.identifier())
		   .set(FIELD_LANGUAGE, node.language())
		   .set(FIELD_SOURCE, node.source())
		   .set(FIELD_KEYWORD, node.keyword())
		   .set(FIELD_DESCRIPTION, node.description())
		   .set(FIELD_DISPLAY, node.display())
		   .set(FIELD_DATA_TABLE, node.dataTable() != null ? node.dataTable().toString() : null)
		   .set(FIELD_DOCUMENT, node.document() != null ? node.document().content() : null)
		   .set(FIELD_DOCUMENT_MIME_TYPE, node.document() != null ? node.document().mimeType() : null)
		   .where(FIELD_NODE_ID.eq(node.nodeID()))
		   .execute();
	}


	private void syncTags(UUID id, Set<String> tags) {
		dsl.deleteFrom(TABLE_PLAN_NODE_TAG)
		   .where(FIELD_PLAN_NODE.eq(id))
		   .execute();

		if (tags == null || tags.isEmpty()) {
			return;
		}
		var batch = dsl.batch(
			dsl.insertInto(TABLE_PLAN_NODE_TAG, FIELD_PLAN_NODE, FIELD_TAG).values((UUID) null, (String) null)
		);
		tags.forEach(tag -> batch.bind(id, tag));
		batch.execute();
	}


	private void syncProperties(UUID id, SortedMap<String, String> properties) {
		dsl.deleteFrom(TABLE_PLAN_NODE_PROPERTY)
		   .where(FIELD_PLAN_NODE.eq(id))
		   .execute();
		if (properties == null || properties.isEmpty()) {
			return;
		}
		var batch = dsl.batch(
			dsl.insertInto(TABLE_PLAN_NODE_PROPERTY, FIELD_PLAN_NODE, FIELD_KEY, FIELD_VALUE)
			   .values((UUID) null, (String) null, (String) null)
		);
		for (var entry : properties.entrySet()) {
			batch.bind(id, entry.getKey(), entry.getValue());
		}
		batch.execute();
	}


	@Override
	public Stream<UUID> searchNodes(PlanNodeCriteria criteria) {
		Condition condition = buildCondition(criteria);
		return dsl.select(FIELD_NODE_ID).from(TABLE_PLAN_NODE)
			.where(condition)
			.orderBy(FIELD_NODE_ID)
			.fetch().stream()
			.map(rec1 -> mapUUID(rec1, FIELD_NODE_ID));
	}



	public int countNodes(PlanNodeCriteria criteria) {
		Condition condition = buildCondition(criteria);
		Integer count = dsl.selectCount().from(TABLE_PLAN_NODE)
				.where(condition)
				.fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	@Override
	public boolean existsNodeTag(UUID nodeID, String tag) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE_TAG)
			   .where(FIELD_PLAN_NODE.eq(nodeID))
			   .and(FIELD_TAG.eq(tag))
		);
	}

	@Override
	public void addNodeTag(UUID nodeID, String tag) {
		dsl.insertInto(TABLE_PLAN_NODE_TAG)
		   .set(FIELD_PLAN_NODE, nodeID)
		   .set(FIELD_TAG, tag)
		   .execute();
	}

	@Override
	public void removeNodeTag(UUID nodeID, String tag) {
		dsl.deleteFrom(TABLE_PLAN_NODE_TAG)
		   .where(FIELD_PLAN_NODE.eq(nodeID))
		   .and(FIELD_TAG.eq(tag))
		   .execute();
	}

	public List<String> getNodeTags(UUID nodeID) {
		return dsl.select(FIELD_TAG)
		   .from(TABLE_PLAN_NODE_TAG)
		   .where(FIELD_PLAN_NODE.eq(nodeID))
		   .fetch().stream()
		   .map(rec -> rec.get(FIELD_TAG)).toList();
	}

	@Override
	public boolean existsNodeProperty(UUID nodeID, String propertyKey, String propertyValue) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE_PROPERTY)
			   .where(FIELD_PLAN_NODE.eq(nodeID))
			   .and(FIELD_KEY.eq(propertyKey))
			   .and(propertyValue != null ? FIELD_VALUE.eq(propertyValue) : DSL.trueCondition())
		);
	}


	@Override
	public void addNodeProperty(UUID nodeID, String propertyKey, String propertyValue) {
		dsl.insertInto(TABLE_PLAN_NODE_PROPERTY)
		   .set(FIELD_PLAN_NODE, nodeID)
		   .set(FIELD_KEY, propertyKey)
		   .set(FIELD_VALUE, propertyValue)
		   .execute();
	}

	@Override
	public void removeNodeProperty(UUID nodeID, String propertyKey) {
		dsl.deleteFrom(TABLE_PLAN_NODE_PROPERTY)
		   .where(FIELD_PLAN_NODE.eq(nodeID))
		   .and(FIELD_KEY.eq(propertyKey))
		   .execute();
	}

	@Override
	public Optional<String> getNodeProperty(UUID nodeID, String propertyKey) {
		return dsl.select(FIELD_VALUE)
			.from(TABLE_PLAN_NODE_PROPERTY)
			.where(FIELD_PLAN_NODE.eq(nodeID))
			.and(FIELD_KEY.eq(propertyKey))
			.fetchOptional(FIELD_VALUE);
	}


	public Map<String, String> getNodeProperties(UUID nodeID) {
		return dsl.select(FIELD_KEY, FIELD_VALUE)
			.from(TABLE_PLAN_NODE_PROPERTY)
			.where(FIELD_PLAN_NODE.eq(nodeID))
			.fetch().stream()
			.collect(Collectors.toMap(
				rec -> rec.get(FIELD_KEY),
				rec -> rec.get(FIELD_VALUE)
			));
	}


	private Condition buildCondition(PlanNodeCriteria criteria) {
		return switch (criteria) {
			case PlanNodeCriteria.AllCriteria() -> DSL.trueCondition();

			case PlanNodeCriteria.HasTagCriteria(String tag) -> DSL.exists(
				DSL.selectOne()
					.from(TABLE_PLAN_NODE_TAG)
					.where(FIELD_PLAN_NODE.eq(FIELD_NODE_ID))
					.and(FIELD_TAG.eq(tag))
			);

			case PlanNodeCriteria.HasPropertyCriteria(String property, String value) -> DSL.exists(
				DSL.selectOne()
					.from(TABLE_PLAN_NODE_PROPERTY)
					.where(FIELD_PLAN_NODE.eq(FIELD_NODE_ID))
					.and(FIELD_KEY.eq(property))
					.and(value != null ? FIELD_VALUE.eq(value) : DSL.trueCondition())
			);

			case PlanNodeCriteria.HasNodeTypeCriteria(NodeType nodeType) ->
				FIELD_TYPE.eq(nodeType.value);

			case PlanNodeCriteria.HasFieldCriteria(String field, Object value) ->
				buildFieldCondition(field, value);

			case PlanNodeCriteria.HasValuedFieldCriteria(String field) ->
				resolveField(field).isNotNull();

			case PlanNodeCriteria.IsDescendantCriteria(UUID parent, int depth) ->
				buildDescendantCondition(parent, depth);

			case PlanNodeCriteria.IsAscendantCriteria(UUID child, int depth) ->
				buildAscendantCondition(child, depth);

			case PlanNodeCriteria.AndCriteria(PlanNodeCriteria[] conditions) -> {
				Condition result = DSL.trueCondition();
				for (PlanNodeCriteria c : conditions) {
					result = result.and(buildCondition(c));
				}
				yield result;
			}

			case PlanNodeCriteria.OrCriteria(PlanNodeCriteria[] conditions) -> {
				Condition result = DSL.falseCondition();
				for (PlanNodeCriteria c : conditions) {
					result = result.or(buildCondition(c));
				}
				yield result;
			}

			case PlanNodeCriteria.NotCriteria(PlanNodeCriteria condition) ->
				DSL.not(buildCondition(condition));
		};
	}


	private Field<?> resolveField(String fieldName) {
		return switch (fieldName.toLowerCase()) {
			case "name" -> FIELD_NAME;
			case "type", "nodetype" -> FIELD_TYPE;
			case "language" -> FIELD_LANGUAGE;
			case "source" -> FIELD_SOURCE;
			case "keyword" -> FIELD_KEYWORD;
			case "description" -> FIELD_DESCRIPTION;
			case "display" -> FIELD_DISPLAY;
			case "identifier" -> FIELD_IDENTIFIER;
			default -> throw new OpenBBTException("Unknown field: {}", fieldName);
		};
	}


	@SuppressWarnings("unchecked")
	private <T> Condition buildFieldCondition(String fieldName, T value) {
		Field<T> field = (Field<T>) resolveField(fieldName);
		return field.eq(value);
	}


	private Condition buildDescendantCondition(UUID parent, int depth) {
		if (depth == 1) {
			// Direct children only
			return FIELD_PARENT_NODE.eq(parent);
		} else {
			// All descendants (depth == -1) or up to certain depth
			var descendants = DSL.unquotedName("descendants");
			var descendantsTable = DSL.table(descendants);
			var dNodeId = DSL.field(DSL.unquotedName("descendants", "node_id"), UUID.class);
			var dDepth = DSL.field(DSL.unquotedName("descendants", "depth"), Integer.class);
			return FIELD_NODE_ID.in(
				DSL.withRecursive(descendants, DSL.unquotedName("node_id"), DSL.unquotedName("depth")).as(
					DSL.select(FIELD_NODE_ID, DSL.inline(1))
						.from(TABLE_PLAN_NODE)
						.where(FIELD_PARENT_NODE.eq(parent))
					.unionAll(
						DSL.select(FIELD_NODE_ID, dDepth.add(1))
							.from(TABLE_PLAN_NODE)
							.join(descendantsTable)
							.on(FIELD_PARENT_NODE.eq(dNodeId))
							.where(depth < 0 ? DSL.trueCondition() : dDepth.lt(depth))
					)
				)
				.select(dNodeId)
				.from(descendantsTable)
			);
		}
	}


	private Condition buildAscendantCondition(UUID child, int depth) {
		if (depth == 1) {
			// Direct parent only
			return FIELD_NODE_ID.in(
				DSL.select(FIELD_PARENT_NODE)
					.from(TABLE_PLAN_NODE)
					.where(FIELD_NODE_ID.eq(child))
			);
		} else {
			// All ancestors (depth == -1) or up to certain depth
			return FIELD_NODE_ID.in(
				DSL.withRecursive("ancestors", "node_id", "depth").as(
					DSL.select(FIELD_PARENT_NODE, DSL.inline(1))
						.from(TABLE_PLAN_NODE)
						.where(FIELD_NODE_ID.eq(child))
						.and(FIELD_PARENT_NODE.isNotNull())
					.unionAll(
						DSL.select(TABLE_PLAN_NODE.field(FIELD_PARENT_NODE), DSL.field("ancestors.depth", Integer.class).add(1))
							.from(TABLE_PLAN_NODE)
							.join(DSL.table("ancestors"))
							.on(TABLE_PLAN_NODE.field(FIELD_NODE_ID).eq(DSL.field("ancestors.node_id", UUID.class)))
							.where(TABLE_PLAN_NODE.field(FIELD_PARENT_NODE).isNotNull())
							.and(depth < 0 ? DSL.trueCondition() : DSL.field("ancestors.depth", Integer.class).lt(depth))
					)
				)
				.select(DSL.field("node_id", UUID.class))
				.from(DSL.table("ancestors"))
			);
		}
	}


	private UUID mapUUID(Record1<UUID> record1, Field<UUID> field) {
		return record1.get(field);
	}


	private PlanNode mapPlanNode(Record rec) {
		PlanNode node = new PlanNode();
		node.nodeID(rec.get(FIELD_NODE_ID));
		Integer typeValue = rec.get(FIELD_TYPE);
		if (typeValue != null) {
			node.nodeType(NodeType.of(typeValue));
		}
		node.name(rec.get(FIELD_NAME));
		node.language(rec.get(FIELD_LANGUAGE));
		node.identifier(rec.get(FIELD_IDENTIFIER));
		node.source(rec.get(FIELD_SOURCE));
		node.keyword(rec.get(FIELD_KEYWORD));
		node.description(rec.get(FIELD_DESCRIPTION));
		node.display(rec.get(FIELD_DISPLAY));
		String dataTableStr = rec.get(FIELD_DATA_TABLE);
		if (dataTableStr != null) {
			node.dataTable(DataTable.fromString(dataTableStr));
		}
		String documentContent = rec.get(FIELD_DOCUMENT);
		String documentMimeType = rec.get(FIELD_DOCUMENT_MIME_TYPE);
		if (documentContent != null) {
			node.document(Document.of(documentMimeType, documentContent));
		}
		fillTagsAndProperties(node);
		return node;
	}

	private void fillTagsAndProperties(PlanNode planNode) {
		// fill tags
		Set<String> tags = new HashSet<>(dsl.select(FIELD_TAG)
            .from(TABLE_PLAN_NODE_TAG)
            .where(FIELD_PLAN_NODE.eq(planNode.nodeID()))
            .fetch(FIELD_TAG));
		planNode.tags(tags);
		// fill properties
		SortedMap<String, String> props = new TreeMap<>();
		dsl.select(FIELD_KEY, FIELD_VALUE)
            .from(TABLE_PLAN_NODE_PROPERTY)
            .where(FIELD_PLAN_NODE.eq(planNode.nodeID()))
            .fetch()
            .forEach(rec -> props.put(rec.get(FIELD_KEY), rec.get(FIELD_VALUE)));
		planNode.properties(props);
	}




	private Integer maxNodePosition(UUID planNodeID) {
		Integer maxPosition = dsl.select(DSL.max(FIELD_NODE_POSITION))
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(planNodeID))
			.fetchOne(DSL.max(FIELD_NODE_POSITION));
		return maxPosition != null ? maxPosition : 0;
	}


	private void assertExistsNode(UUID id) {
		if (id == null) {
			throw new OpenBBTException("Plan node ID is null!");
		}
		if (!existsNode(id)) {
			throw new OpenBBTException("Plan node {} not present in repository", id);
		}
	}


	private UUID generateUUID() {
		return UlidCreator.getUlid().toUuid();
	}

}