package org.myjtools.openbbt.core.backend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestCaseExecution {

	private static final ThreadLocal<TestCaseExecution> threadLocal = new ThreadLocal<>();

	public static TestCaseExecution current() {
		return threadLocal.get();
	}


	private final Map<String,String> variables = new ConcurrentHashMap<>();

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
