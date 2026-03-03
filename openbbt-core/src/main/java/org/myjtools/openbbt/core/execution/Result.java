package org.myjtools.openbbt.core.execution;

public enum Result {


	PASSED(1),
	FAILED(2),
	SKIPPED(3),
	ERROR(4),
	UNDEFINED(5);

	final int value;

	Result(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

}
