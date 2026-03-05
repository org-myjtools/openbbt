package org.myjtools.openbbt.core.persistence;

import java.util.UUID;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import java.io.IOException;

public class TestPlanRepositoryWriter {

	public interface Appender {
		void append(String string) throws IOException;
	}

	private final TestPlanRepository repository;

	public TestPlanRepositoryWriter(TestPlanRepository repository) {
		this.repository = repository;
	}

	public void write(UUID rootNodeID, Appender appender) throws IOException {
		write(rootNodeID,appender,0);
	}

	private void write(UUID nodeID, Appender appender, int indent) throws IOException {
		TestPlanNode node = repository.getNodeData(nodeID).orElseThrow();
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
		for (UUID childID : repository.getNodeChildren(nodeID).toList()) {
			write(childID,appender,indent+1);
		}
	}

}
