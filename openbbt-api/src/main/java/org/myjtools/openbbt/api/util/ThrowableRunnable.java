package org.myjtools.openbbt.api.util;

@FunctionalInterface
public interface ThrowableRunnable {

    void run(Object... arguments) throws Exception;

}
