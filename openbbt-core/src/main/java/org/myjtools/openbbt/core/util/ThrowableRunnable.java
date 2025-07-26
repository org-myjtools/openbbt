package org.myjtools.openbbt.core.util;

@FunctionalInterface
public interface ThrowableRunnable {

    void run(Object... arguments) throws Exception;

}
