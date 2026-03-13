package org.myjtools.openbbt.core.backend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionContext {

	private static final ThreadLocal<ExecutionContext> threadLocal = new ThreadLocal<>();

	public static ExecutionContext current() {
		return threadLocal.get();
	}





	private final Map<String,String> variables = new ConcurrentHashMap<>();

	static void setCurrent(ExecutionContext executionContext) {
		threadLocal.set(executionContext);
	}

	static void clearCurrent() {
		threadLocal.remove();
	}

	public void setVariable(String name, String value) {
		variables.put(name, value);
	}

	public String getVariable(String name) {
		return variables.get(name);
	}


	public String interpolateString(String input) {
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			String placeholder = "${" + entry.getKey() + "}";
			input = input.replace(placeholder, entry.getValue());
		}
		return input;
	}

}
