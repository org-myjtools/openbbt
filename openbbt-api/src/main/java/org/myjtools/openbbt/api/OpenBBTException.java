package org.myjtools.openbbt.api;

import java.io.Serial;

/**
 * OpenBBTException is the base class for all exceptions thrown by the OpenBBT API.
 * It extends RuntimeException, allowing it to be used as an unchecked exception.
 * This class provides constructors for creating exceptions with a message, a cause,
 * or both, and supports formatted messages.
 */
public class OpenBBTException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public OpenBBTException(String message) {
        super(message);
    }

    public OpenBBTException(String message, Object... args) {
        super(format(message,args));
    }

    public OpenBBTException(Throwable cause, String message) {
        super(message, cause);
    }

    public OpenBBTException(Throwable cause, String message, Object... args) {
        super(format(message,args), cause);
    }

    public OpenBBTException(Throwable cause) {
        super(cause);
    }


    protected static String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        String formattedMessage = message;
        for (int i = 0; i < args.length; i++) {
            formattedMessage = formattedMessage.replace("{}", String.valueOf(args[i]));
        }
        return formattedMessage;
    }

}
