package org.myjtools.openbbt.core.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.PlanNodeCriteria;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.plan.*;
import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class JooqRepository implements PlanNodeRepository {

	private static final Table<Record> TABLE_PLAN_NODE = DSL.table("plan_node");
	private static final Table<Record> TABLE_PLAN_NODE_TAG = DSL.table("plan_node_tag");
	private static final Table<Record> TABLE_PLAN_NODE_PROPERTY = DSL.table("plan_node_property");

	private static final Field<UUID> FIELD_NODE_ID = DSL.field("node_id", UUID.class);
	private static final Field<UUID> FIELD_PARENT_NODE = DSL.field("parent_node", UUID.class);
	private static final Field<UUID> FIELD_ROOT_NODE = DSL.field("root_node", UUID.class);
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


	public JooqRepository(DataSourceProvider dataSourceProvider) {
		this(dataSourceProvider.obtainDataSource(), dataSourceProvider.dialect());
	}

	public JooqRepository(DataSource dataSource, SQLDialect dialect) {
		this.dsl = DSL.using(new DataSourceConnectionProvider(dataSource), dialect);
	}


	public Optional<PlanNode> getNodeData(PlanNodeID id) {
		return dsl.select(
				FIELD_NODE_ID, FIELD_ROOT_NODE, FIELD_PARENT_NODE, FIELD_NODE_POSITION,
				FIELD_TYPE, FIELD_NAME, FIELD_IDENTIFIER, FIELD_LANGUAGE, FIELD_SOURCE,
				FIELD_KEYWORD, FIELD_DESCRIPTION, FIELD_DISPLAY, FIELD_DATA_TABLE,
				FIELD_DOCUMENT, FIELD_DOCUMENT_MIME_TYPE
			)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_NODE_ID.eq(id.UUID()))
			.fetchOptional()
			.map(this::mapPlanNode);
	}


	public boolean existsNode(PlanNodeID id) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE)
			   .where(FIELD_NODE_ID.eq(id.UUID()))
		);
	}


	public Optional<PlanNodeID> getParentNode(PlanNodeID id) {
		assertExistsNode(id);
		return dsl.select(FIELD_PARENT_NODE)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_NODE_ID.eq(id.UUID()))
			.fetchOptional()
			.map(record1 -> record1.get(FIELD_PARENT_NODE))
			.filter(Objects::nonNull)
			.map(PlanNodeID::new);
	}


	public void deleteNode(PlanNodeID id) {
		// first detach from parent if exists
		getParentNode(id).ifPresent(parent -> detachChildNode(parent, id));
		// delete node (cascade should handle hierarchy, tags, properties)
		dsl.deleteFrom(TABLE_PLAN_NODE)
		   .where(FIELD_NODE_ID.eq(id.UUID()))
		   .execute();
	}


	public void attachChildNodeLast(PlanNodeID parent, PlanNodeID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_PARENT_NODE, parent.UUID())
		   .set(FIELD_NODE_POSITION, maxNodePosition(parent) + 1)
		   .set(FIELD_ROOT_NODE, getRootNode(parent).orElse(parent).UUID())
		   .where(FIELD_NODE_ID.eq(child.UUID()))
		   .execute();
	}


	public void attachChildNodeFirst(PlanNodeID parent, PlanNodeID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		// increment position of existing child nodes
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_NODE_POSITION, FIELD_NODE_POSITION.add(1))
			.where(FIELD_PARENT_NODE.eq(parent.UUID()))
			.execute();
		// set child node as first
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_PARENT_NODE, parent.UUID())
			.set(FIELD_ROOT_NODE, getRootNode(parent).orElse(parent).UUID())
			.set(FIELD_NODE_POSITION, 1)
			.where(FIELD_NODE_ID.eq(child.UUID()))
			.execute();
	}


	public void detachChildNode(PlanNodeID parent, PlanNodeID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_PARENT_NODE, (UUID) null)
			.set(FIELD_ROOT_NODE, child.UUID())
			.where(FIELD_NODE_ID.eq(child.UUID()))
			.execute();
	}


	public Optional<PlanNodeID> getRootNode(PlanNodeID id) {
		assertExistsNode(id);
		return dsl.select(FIELD_ROOT_NODE)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_NODE_ID.eq(id.UUID()))
			.fetchOptional()
			.map(record1 -> mapPlanNodeID(record1, FIELD_ROOT_NODE));
	}


	public Stream<PlanNodeID> getNodeChildren(PlanNodeID id) {
		assertExistsNode(id);
		return dsl.select(FIELD_NODE_ID)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(id.UUID()))
			.orderBy(FIELD_NODE_POSITION)
			.fetchStream()
			.map(record1 -> mapPlanNodeID(record1, FIELD_NODE_ID));
	}


	public Stream<PlanNodeID> getNodeDescendants(PlanNodeID id) {
		assertExistsNode(id);
		return dsl.withRecursive("descendants").as(
			   DSL.select(FIELD_NODE_ID)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_PARENT_NODE.eq(id.UUID()))
			   .unionAll(
				   DSL.select(TABLE_PLAN_NODE.field(FIELD_NODE_ID))
					   .from(TABLE_PLAN_NODE)
					  .join(DSL.table("descendants"))
					  .on(
						  TABLE_PLAN_NODE.field(FIELD_PARENT_NODE).eq(DSL.field("descendants.node_id", UUID.class))
						  .and(TABLE_PLAN_NODE.field(FIELD_ROOT_NODE).eq(DSL.field("ancestors.root_node", UUID.class)))
					  )
			   )
		   )
		   .select(FIELD_NODE_ID)
		   .from(DSL.table("descendants"))
		   .fetchStream()
		   .map(rec -> mapPlanNodeID(rec, FIELD_NODE_ID));
	}


	public Stream<PlanNodeID> getNodeAncestors(PlanNodeID id) {
		assertExistsNode(id);
		return dsl.withRecursive("ancestors").as(
               DSL.select(FIELD_PARENT_NODE)
                   .from(TABLE_PLAN_NODE)
                   .where(FIELD_NODE_ID.eq(id.UUID()))
               .unionAll(
                   DSL.select(TABLE_PLAN_NODE.field(FIELD_PARENT_NODE))
                       .from(TABLE_PLAN_NODE)
                      .join(DSL.table("ancestors"))
                      .on(
                          TABLE_PLAN_NODE.field(FIELD_NODE_ID).eq(DSL.field("ancestors.parent_node", UUID.class))
                          .and(TABLE_PLAN_NODE.field(FIELD_ROOT_NODE).eq(DSL.field("ancestors.root_node", UUID.class)))
                      )
               )
		   )
		   .select(FIELD_PARENT_NODE)
		   .from(DSL.table("ancestors"))
		   .where(FIELD_PARENT_NODE.isNotNull())
		   .fetchStream()
		   .map(rec -> mapPlanNodeID(rec, FIELD_PARENT_NODE));
	}


	public PlanNodeID persistNode(PlanNode node) {
		boolean isUpdate = node.nodeID() != null;
		PlanNodeID id;
		if (isUpdate) {
			id = node.nodeID();
			assertExistsNode(id);
			updateNode(node);
		} else {
			id = generatePlanNodeID();
			node.nodeID(id);
			insertNode(node);
		}
		syncTags(id, node.tags());
		syncProperties(id, node.properties());
		return id;
	}


	private void insertNode(PlanNode node) {
		dsl.insertInto(TABLE_PLAN_NODE)
		   .set(FIELD_NODE_ID, node.nodeID().UUID())
		   .set(FIELD_ROOT_NODE, node.nodeID().UUID())
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
		   .where(FIELD_NODE_ID.eq(node.nodeID().UUID()))
		   .execute();
	}


	private void syncTags(PlanNodeID id, Set<String> tags) {
		dsl.deleteFrom(TABLE_PLAN_NODE_TAG)
		   .where(FIELD_PLAN_NODE.eq(id.UUID()))
		   .execute();

		if (tags == null || tags.isEmpty()) {
			return;
		}
		var batch = dsl.batch(
			dsl.insertInto(TABLE_PLAN_NODE_TAG, FIELD_PLAN_NODE, FIELD_TAG).values((UUID) null, (String) null)
		);
		tags.forEach(tag -> batch.bind(id.UUID(), tag));
		batch.execute();
	}


	private void syncProperties(PlanNodeID id, SortedMap<String, String> properties) {
		dsl.deleteFrom(TABLE_PLAN_NODE_PROPERTY)
		   .where(FIELD_PLAN_NODE.eq(id.UUID()))
		   .execute();

		if (properties == null || properties.isEmpty()) {
			return;
		}
		var batch = dsl.batch(
			dsl.insertInto(TABLE_PLAN_NODE_PROPERTY, FIELD_PLAN_NODE, FIELD_KEY, FIELD_VALUE)
			   .values((UUID) null, (String) null, (String) null)
		);
		for (var entry : properties.entrySet()) {
			batch.bind(id.UUID(), entry.getKey(), entry.getValue());
		}
		batch.execute();
	}

	@Override
	public Stream<PlanNodeID> searchNodes(PlanNodeCriteria criteria) {
		Condition condition = buildCondition(criteria);
		return dsl.select(FIELD_NODE_ID).from(TABLE_PLAN_NODE)
			.where(condition)
			.fetchStream()
			.map(rec1 -> mapPlanNodeID(rec1, FIELD_NODE_ID));
	}


	@Override
	public boolean existsTag(PlanNodeID nodeID, String tag) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE_TAG)
			   .where(FIELD_PLAN_NODE.eq(nodeID.UUID()))
			   .and(FIELD_TAG.eq(tag))
		);
	}

	@Override
	public boolean existsProperty(PlanNodeID nodeID, String propertyKey, String propertyValue) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE_PROPERTY)
			   .where(FIELD_PLAN_NODE.eq(nodeID.UUID()))
			   .and(FIELD_KEY.eq(propertyKey))
			   .and(propertyValue != null ? FIELD_VALUE.eq(propertyValue) : DSL.trueCondition())
		);
	}

	@Override
	public Optional<String> getNodeProperty(PlanNodeID nodeID, String propertyKey) {
		return dsl.select(FIELD_VALUE)
			.from(TABLE_PLAN_NODE_PROPERTY)
			.where(FIELD_PLAN_NODE.eq(nodeID.UUID()))
			.and(FIELD_KEY.eq(propertyKey))
			.fetchOptional(FIELD_VALUE);
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
			return FIELD_NODE_ID.in(
				DSL.withRecursive("descendants", "node_id", "depth").as(
					DSL.select(FIELD_NODE_ID, DSL.inline(1))
						.from(TABLE_PLAN_NODE)
						.where(FIELD_PARENT_NODE.eq(parent))
					.unionAll(
						DSL.select(TABLE_PLAN_NODE.field(FIELD_NODE_ID), DSL.field("descendants.depth", Integer.class).add(1))
							.from(TABLE_PLAN_NODE)
							.join(DSL.table("descendants"))
							.on(TABLE_PLAN_NODE.field(FIELD_PARENT_NODE).eq(DSL.field("descendants.node_id", UUID.class)))
							.where(depth < 0 ? DSL.trueCondition() : DSL.field("descendants.depth", Integer.class).lt(depth))
					)
				)
				.select(DSL.field("node_id", UUID.class))
				.from(DSL.table("descendants"))
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


	private PlanNodeID mapPlanNodeID(Record1<UUID> record1, Field<UUID> field) {
		return new PlanNodeID(record1.get(field));
	}


	private PlanNode mapPlanNode(Record rec) {
		PlanNode node = new PlanNode();
		node.nodeID(new PlanNodeID(rec.get(FIELD_NODE_ID)));
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
            .where(FIELD_PLAN_NODE.eq(planNode.nodeID().UUID()))
            .fetch(FIELD_TAG));
		planNode.tags(tags);
		// fill properties
		SortedMap<String, String> props = new TreeMap<>();
		dsl.select(FIELD_KEY, FIELD_VALUE)
            .from(TABLE_PLAN_NODE_PROPERTY)
            .where(FIELD_PLAN_NODE.eq(planNode.nodeID().UUID()))
            .fetch()
            .forEach(rec -> props.put(rec.get(FIELD_KEY), rec.get(FIELD_VALUE)));
		planNode.properties(props);
	}


	private Integer maxNodePosition(PlanNodeID planNodeID) {
		Integer maxPosition = dsl.select(DSL.max(FIELD_NODE_POSITION))
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(planNodeID.UUID()))
			.fetchOne(DSL.max(FIELD_NODE_POSITION));
		return maxPosition != null ? maxPosition : 0;
	}


	private void assertExistsNode(PlanNodeID id) {
		if (id == null) {
			throw new OpenBBTException("Plan node ID is null!");
		}
		if (!existsNode(id)) {
			throw new OpenBBTException("Plan node {} not present in repository", id);
		}
	}


	private PlanNodeID generatePlanNodeID() {
		return new PlanNodeID(UlidCreator.getUlid().toUuid());
	}

}