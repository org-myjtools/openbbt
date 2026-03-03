package org.myjtools.openbbt.core.execution;

import org.myjtools.openbbt.core.OpenBBTException;

public class NoMatchingStepException extends OpenBBTException {

	public NoMatchingStepException(String message, Object... args) {
		super(message, args);
	}
}
