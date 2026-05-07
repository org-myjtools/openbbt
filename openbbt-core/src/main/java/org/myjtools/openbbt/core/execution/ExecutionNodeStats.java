package org.myjtools.openbbt.core.execution;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExecutionNodeStats {
    private int numExecutions;
    private int numThreads;
    private int min;
    private int max;
    private int mean;
    private int p50;
    private int p95;
    private int p99;
    private double throughput;
    private double errorRate;
}