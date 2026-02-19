package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import java.io.IOException;

public class PlanNodeRepositoryWriter {

	public interface Appender {
		void append(String string) throws IOException;
	}

	private final PlanNodeRepository repository;

	public PlanNodeRepositoryWriter(PlanNodeRepository repository) {
		this.repository = repository;
	}

	public void write(PlanNodeID rootNodeID, Appender appender) throws IOException {
		write(rootNodeID,appender,0);
	}

	private void write(PlanNodeID nodeID, Appender appender, int indent) throws IOException {
		PlanNode node = repository.getNodeData(nodeID).orElseThrow();
		appender.append("  ".repeat(indent));
		appender.append("[");
		appender.append(String.valueOf(node.nodeType()));
		appender.append("] ");
		if (node.identifier() != null) {
			appender.append("(");
			appender.append(node.identifier());
			appender.append(") ");
		}
		appender.append(node.toString());
		appender.append("\n");
		for (PlanNodeID childID : repository.getNodeChildren(nodeID).toList()) {
			write(childID,appender,indent+1);
		}
	}

}
