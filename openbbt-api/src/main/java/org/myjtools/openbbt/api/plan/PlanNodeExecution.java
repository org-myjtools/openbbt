package org.myjtools.openbbt.api.plan;

import java.time.Instant;
import lombok.*;

@Getter @Setter @EqualsAndHashCode
public class PlanNodeExecution {

    private ExecutionResult result;
    private Instant startInstant;
    private Instant finishInstant;

}
