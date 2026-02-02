package org.myjtools.openbbt.core.plan;

import java.util.UUID;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public record PlanNodeID(UUID UUID) implements Comparable<PlanNodeID> {

	@Override
	public int compareTo(PlanNodeID other) {
		return UUID.compareTo(other.UUID);
	}

	@Override
	public String toString() {
		return UUID.toString();
	}

}
