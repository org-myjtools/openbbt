package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
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


	Optional<PlanNode> getNodeData(PlanNodeID id);
	boolean existsNode(PlanNodeID id);
	Optional<PlanNodeID> getParentNode(PlanNodeID id);


	/**
	 * Delete completely a plan node, including its child nodes.
	 * If the node was a child of another node, it will be detached.
	 * @throws OpenBBTException if the UUID does not exist in the repository
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


	Optional<PlanNodeID> getRootNode(PlanNodeID id);

	Stream<PlanNodeID> getNodeChildren(PlanNodeID id);
	Stream<PlanNodeID> getNodeDescendants(PlanNodeID id);
	Stream<PlanNodeID> getNodeAncestors(PlanNodeID id);

	int countNodeChildren(PlanNodeID id);
	int countNodeDescendants(PlanNodeID id);
	int countNodeAncestors(PlanNodeID id);


	/**
	 * Persist a plan node in the repository. If the node UUID did exist previously, it
	 * will update the node content; otherwise, it will create a new record and assign a
	 * unique UUID.
	 * @return The assigned node UUID
	 */
	PlanNodeID persistNode(PlanNode node);

	Stream<PlanNodeID> searchNodes(PlanNodeCriteria criteria);

	int countNodes(PlanNodeCriteria criteria);

	boolean existsTag(PlanNodeID nodeID, String tag);

	boolean existsProperty(PlanNodeID nodeID, String propertyKey, String propertyValue);

	Optional<String> getNodeProperty(PlanNodeID nodeID, String propertyKey);

}
