package org.myjtools.openbbt.core.testplan;

public enum ExecutionResult {

	// in inverted order of severity

	/** The node or all of its children has been executed successfully
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
	PASSED,

	/** The node was not executed due to previous step did not pass the test */
	SKIPPED,

	/** The node or any of its children was not executed due to malformed definition */
	UNDEFINED,

	/** The node or any of its children has not passed the validation */
	FAILED,

	/** The node or any of its children has experienced a fatal error */
	ERROR;


	public boolean isPassed() {
		return this == PASSED;
	}

	public boolean isExecuted() {
		return this == PASSED || this == FAILED || this == ERROR;
	}


}
