package org.myjtools.openbbt.core.plan;

import java.util.Objects;
import java.util.UUID;

public record PlanID(UUID UUID) implements Comparable<PlanID> {

	public PlanID {
		Objects.requireNonNull(UUID, "UUID cannot be null!");
	}

	@Override
	public int compareTo(PlanID other) {
		return UUID.compareTo(other.UUID);
	}

	@Override
	public String toString() {
		return UUID.toString();
	}

}
