package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.OpenBBTException;

import java.io.Serial;

public class ExpressionException extends OpenBBTException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExpressionException(String text, int position, String message) {
        super("Error in expression {} at position {}: {}. {}", position, text, message);
    }


    public ExpressionException(String message, Object... args) {
        super(message, args);
    }
}
