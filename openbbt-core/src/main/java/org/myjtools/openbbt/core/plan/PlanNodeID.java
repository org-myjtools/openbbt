package org.myjtools.openbbt.core.plan;

import java.util.UUID;

public record PlanNodeID(UUID UUID) {

    @Override
    public String toString() {
        return UUID.toString();
    }

}
