package org.myjtools.openbbt.core.backend;

import org.myjtools.openbbt.core.execution.ExecutionNodeStats;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Benchmark {

	private final Integer totalExecutions;
	private final Integer numThreads;
	private final AtomicInteger currentExecutions = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, Integer> executionTimes = new ConcurrentHashMap<>();
	private final AtomicInteger errors = new AtomicInteger(0);

	public Benchmark(Integer totalExecutions, Integer numThreads) {
		this.totalExecutions = totalExecutions;
		this.numThreads = numThreads;
	}

	int totalExecutions() {
		return totalExecutions;
	}

	int currentExecutions() {
		return currentExecutions.get();
	}

	int numThreads() {
		return numThreads;
	}

	public int markStarted() {
		return currentExecutions.incrementAndGet();
	}

	public void markFinished(int executionId, int timeTakenMillis, boolean error) {
		executionTimes.put(executionId, timeTakenMillis);
		if (error) {
			errors.incrementAndGet();
		}
	}

	public ExecutionNodeStats statistics() {
		int totalTime = executionTimes.values().stream().mapToInt(Integer::intValue).sum();
		int minTime = executionTimes.values().stream().mapToInt(Integer::intValue).min().orElse(0);
		int maxTime = executionTimes.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		int averageTime = totalExecutions > 0 ? totalTime / totalExecutions : 0;
		int p50 = executionTimes.values().stream().mapToInt(Integer::intValue).sorted().skip(totalExecutions / 2).findFirst().orElse(0);
		int p95 = executionTimes.values().stream().mapToInt(Integer::intValue).sorted().skip((long) (totalExecutions * 0.95)).findFirst().orElse(0);
		int p99 = executionTimes.values().stream().mapToInt(Integer::intValue).sorted().skip((long) (totalExecutions * 0.99)).findFirst().orElse(0);
		double throughput = totalExecutions / (totalTime / 1000.0);
		double errorRate = totalExecutions > 0 ? (double) errors.get() / totalExecutions : 0.0;
		return new ExecutionNodeStats(
			totalExecutions,
			numThreads,
			minTime,
			maxTime,
			averageTime,
			p50,
			p95,
			p99,
			throughput,
			errorRate
		);
	}
}
