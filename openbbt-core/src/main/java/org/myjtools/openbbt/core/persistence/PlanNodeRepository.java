package org.myjtools.openbbt.core.persistence;

import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.OpenBBTException;
import java.util.*;
import java.util.stream.Stream;


/**
 * Repository interface for managing plan nodes. Necessary for working with large test plan models
 * that cannot be fully loaded into memory.
 * This interface defines methods for retrieving, persisting, and manipulating plan nodes
 * in a storage system. It supports operations such as attaching and detaching child nodes,
 * searching nodes based on criteria, and managing node relationships.
 * Implementations of this interface should handle the underlying storage and retrieval logic,
 * allowing for flexibility in how plan nodes are stored (e.g., in-memory, database, etc.).
 * */
public interface PlanNodeRepository  {


    Optional<PlanNode> getNode(PlanNodeID id);
    boolean existsNode(PlanNodeID id);
    Optional<PlanNodeID> getParentNodeID(PlanNodeID id);
    Optional<PlanNode> getParentNode(PlanNodeID id);


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
    void attachChildNode(PlanNodeID parent, PlanNodeID child);

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


    Optional<PlanNodeID> getRootNodeID(PlanNodeID id);
    Optional<PlanNode> getRootNode(PlanNodeID id);
    List<PlanNodeID> getNodeChildrenID(PlanNodeID id);
    List<PlanNode> getNodeChildren(PlanNodeID id);
    Stream<PlanNodeID> getNodeDescendantsID(PlanNodeID id);
    Stream<PlanNode> getNodeDescendants(PlanNodeID id);
    Stream<PlanNodeID> getNodeAncestorsID(PlanNodeID id);
    Stream<PlanNode> getNodeAncestors(PlanNodeID id);
    /**
     * Persist a plan node in the repository. If the node UUID did exist previously, it
     * will update the node content; otherwise, it will create a new record and assign a
     * unique UUID.
     * @return The assigned node UUID
     */
    PlanNodeID persistNode(PlanNode node);

    Stream<PlanNode> searchNodes(PlanNodeCriteria criteria);

    

    void commit();

}
