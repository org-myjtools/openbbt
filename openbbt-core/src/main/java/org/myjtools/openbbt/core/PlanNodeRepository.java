package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Repository interface for managing plan nodes. Necessary for working with large test plan models
 * that cannot be fully loaded into memory.
 * This interface defines methods for retrieving, persisting, and manipulating plan nodes
 * in a storage system. It supports operations such as attaching and detaching child nodes,
 * searching nodes based on criteria, and managing node relationships.
 * Implementations of this interface should handle the underlying storage and retrieval logic,
 * allowing for flexibility in how plan nodes are stored (e.g., in-memory, database, etc.).
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public interface PlanNodeRepository  {


	/**
	 * Retrieve the data of a plan node by its ID.
	 * @param id the node ID
	 * @return the plan node data, or empty if the node does not exist
	 */
	Optional<PlanNode> getNodeData(PlanNodeID id);

	/**
	 * Update a specific field of a plan node. The field name and value are determined by the caller,
	 * and the repository implementation should handle the storage and retrieval of these fields.
	 * @param id the node ID
	 * @param fieldName the name of the field to update
	 * @param fieldValue the new value for the field
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	<T> void updateNodeField(PlanNodeID id, String fieldName, T fieldValue);


	/**
	 * Retrieve the value of a specific field of a plan node.
	 * @param id the node ID
	 * @param fieldName the name of the field to retrieve
	 * @return the value of the field, or empty if the node or field does not exist
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	<T> Optional<T> getNodeField(PlanNodeID id, String fieldName);


	/**
	 * Check whether a plan node exists in the repository.
	 * @param id the node ID
	 * @return {@code true} if the node exists, {@code false} otherwise
	 */
	boolean existsNode(PlanNodeID id);

	/**
	 * Retrieve the parent node of a given node.
	 * @param id the node ID
	 * @return the parent node ID, or empty if the node is a root node
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	Optional<PlanNodeID> getParentNode(PlanNodeID id);

	/**
	 * Delete completely a plan node, including its child nodes.
	 * If the node was a child of another node, it will be detached.
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	void deleteNode(PlanNodeID id);


	/**
	 * Attach a plan node as child of another node, at the end of the existing child list.
	 * If the child node was already in the child list, this operation will have no effect.
	 * @throws OpenBBTException if either the parent UUID or the child UUID do not exist in the repository
	 */
	void attachChildNodeLast(PlanNodeID parent, PlanNodeID child);

	/**
	 * Attach a plan node as child of another node, at the beginning of the existing child list.
	 * If the child node was already in the child list, this operation will have no effect.
	 * @throws OpenBBTException if either the parent UUID or the child UUID do not exist in the repository
	 */
	void attachChildNodeFirst(PlanNodeID parent, PlanNodeID child);


	/**
	 * Detach a plan node as a child of another node, keeping it in the repository as an orphan node.
	 * If the child node was not already in the child list, this operation will have no effect.
	 * @throws OpenBBTException if either the parent UUID or the child UUID do not exist in the repository
	 */
	void detachChildNode(PlanNodeID parent, PlanNodeID child);



	/**
	 * Retrieve the direct children of a node, ordered by their position.
	 * @param id the parent node ID
	 * @return a stream of child node IDs in order
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	Stream<PlanNodeID> getNodeChildren(PlanNodeID id);

	/**
	 * Retrieve all descendants of a node (children, grandchildren, etc.) recursively.
	 * @param id the ancestor node ID
	 * @return a stream of descendant node IDs
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	Stream<PlanNodeID> getNodeDescendants(PlanNodeID id);

	/**
	 * Retrieve all ancestors of a node (parent, grandparent, etc.) up to the root.
	 * @param id the descendant node ID
	 * @return a stream of ancestor node IDs
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	Stream<PlanNodeID> getNodeAncestors(PlanNodeID id);

	/**
	 * Count the direct children of a node.
	 * @param id the parent node ID
	 * @return the number of direct children
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	int countNodeChildren(PlanNodeID id);

	/**
	 * Count all descendants of a node recursively.
	 * @param id the ancestor node ID
	 * @return the total number of descendants
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	int countNodeDescendants(PlanNodeID id);

	/**
	 * Count all ancestors of a node up to the root.
	 * @param id the descendant node ID
	 * @return the total number of ancestors
	 * @throws OpenBBTException if the node does not exist in the repository
	 */
	int countNodeAncestors(PlanNodeID id);

	/**
	 * Persist a plan node in the repository. If the node UUID did exist previously, it
	 * will update the node content; otherwise, it will create a new record and assign a
	 * unique UUID.
	 * @param node the plan node to persist
	 * @return the assigned node UUID
	 */
	PlanNodeID persistNode(PlanNode node);

	/**
	 * Search for nodes matching the given criteria.
	 * @param criteria the search criteria
	 * @return a stream of matching node IDs
	 */
	Stream<PlanNodeID> searchNodes(PlanNodeCriteria criteria);

	/**
	 * Count nodes matching the given criteria.
	 * @param criteria the search criteria
	 * @return the number of matching nodes
	 */
	int countNodes(PlanNodeCriteria criteria);

	/**
	 * Check whether a node has a specific tag.
	 * @param nodeID the node ID
	 * @param tag the tag to check
	 * @return {@code true} if the tag exists on the node, {@code false} otherwise
	 */
	boolean existsTag(PlanNodeID nodeID, String tag);

	/**
	 * Retrieve all tags of a node.
	 * @param nodeID the node ID
	 * @return a stream of tags associated with the node, or an empty stream if the node has no tags
	 */
	void addTag(PlanNodeID nodeID, String tag);

	/**
	 * Remove a specific tag from a node. If the tag does not exist on the node, this operation will have no effect.
	 * @param nodeID the node ID
	 * @param tag the tag to remove
	 */
	void removeTag(PlanNodeID nodeID, String tag);

	/**
	 * Retrieve all tags of a node as a list.
	 * @param nodeID the node ID
	 * @return a list of tags associated with the node, or an empty list if the node has no tags
	 */
	List<String> getTags(PlanNodeID nodeID);

	/**
	 * Check whether a node has a specific property. If {@code propertyValue} is {@code null},
	 * only the existence of the key is checked regardless of its value.
	 * @param nodeID the node ID
	 * @param propertyKey the property key
	 * @param propertyValue the expected value, or {@code null} to match any value
	 * @return {@code true} if the property exists (and matches the value if provided)
	 */
	boolean existsProperty(PlanNodeID nodeID, String propertyKey, String propertyValue);

	void addProperty(PlanNodeID nodeID, String propertyKey, String propertyValue);
	void removeProperty(PlanNodeID nodeID, String propertyKey);

	/**
	 * Retrieve the value of a specific property of a node.
	 * @param nodeID the node ID
	 * @param propertyKey the property key
	 * @return the property value, or empty if the property does not exist
	 */
	Optional<String> getProperty(PlanNodeID nodeID, String propertyKey);

	/**
	 * Retrieve all properties of a node as a map of key-value pairs.
	 * @param nodeID the node ID
	 * @return a map containing all properties of the node, or an empty map if the node has no properties
	 */
	Map<String, String> getProperties(PlanNodeID nodeID);

}
