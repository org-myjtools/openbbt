package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import java.io.IOException;
import java.io.Writer;

public class PlanNodeRepositoryWriter {

	private final PlanNodeRepository repository;

	public PlanNodeRepositoryWriter(PlanNodeRepository repository) {
		this.repository = repository;
	}

	public void write(PlanNodeID rootNodeID, Writer writer) throws IOException {
		write(rootNodeID,writer,0);
	}

	private void write(PlanNodeID nodeID, Writer writer, int indent) throws IOException {
		PlanNode node = repository.getNodeData(nodeID).orElseThrow();
		writer.append("  ".repeat(indent)).append(node.toString()).append("\n");
		for (PlanNodeID childID : repository.getNodeChildren(nodeID).toList()) {
			write(childID,writer,indent+1);
		}
	}

}
