package org.myjtools.openbbt.core.backend;

import org.myjtools.openbbt.core.assertions.Assertion;
import org.myjtools.openbbt.core.datatypes.DataType;
import org.myjtools.openbbt.core.datatypes.DataTypes;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.plan.DataTable;
import org.myjtools.openbbt.core.plan.Document;
import org.myjtools.openbbt.core.step.Step;
import org.myjtools.openbbt.core.step.StepContributor;
import org.myjtools.openbbt.core.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class RunnableStep {

    private StepContributor stepContributor;
    private String stepKey;
    private Method method;
    private List<Pair<String, DataType>> expectedArguments;

    private enum LastParameterType {
        REGULAR, ASSERTION, DATA_TABLE, DOCUMENT
    }
    private LastParameterType lastParameterType;



    public RunnableStep(StepContributor stepContributor, Method method, DataTypes dataTypes) {
        this.stepContributor = stepContributor;
        var annotation = method.getAnnotation(Step.class);
        this.stepKey = annotation.value();
        this.method = method;
        this.lastParameterType = checkLastParameterType(method);
        this.expectedArguments = Pair.ofMap(checkStepArgs(dataTypes, annotation, method));
    }


    public String stepKey() {
        return stepKey;
    }


    private LastParameterType checkLastParameterType(Method method) {
        if (method.getParameterCount() == 0) {
            return LastParameterType.REGULAR;
        }
        Class<?> methodLastParameterType = method.getParameterTypes()[method.getParameterCount() - 1];
        if (methodLastParameterType == Assertion.class) {
            return LastParameterType.ASSERTION;
        } else if (methodLastParameterType == DataTable.class) {
            return LastParameterType.DATA_TABLE;
        } else if (methodLastParameterType == Document.class) {
            return LastParameterType.DOCUMENT;
        }
        return LastParameterType.REGULAR;
    }




    public void run(Map<String,Object> arguments, Object additionalData) throws Throwable {

        Object[] args = new Object[method.getParameterCount()];
        for (int i = 0; i < this.expectedArguments.size(); i++) {
            Pair<String, DataType> arg = this.expectedArguments.get(i);
            Object value = arguments.get(arg.left());
            args[i] = value;
        }
        if (lastParameterType != LastParameterType.REGULAR) {
            args[args.length - 1] = Objects.requireNonNull(
                additionalData,
                "Additional data must not be null for last parameter type: " + lastParameterType)
            ;
        }

        try {
            method.invoke(stepContributor, args);
        } catch (IllegalAccessException e) {
            throw new OpenBBTException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }








    private LinkedHashMap<String, DataType> checkStepArgs(DataTypes datatypes, Step step, Method method) {
       if (step.args().length == 0) {
            return checkImplicitStepArgs(datatypes, step, method);
        } else {
            return checkExplicitStepArgs(datatypes, step, method, step.args());
        }
    }


    private LinkedHashMap<String, DataType> checkImplicitStepArgs(DataTypes dataTypes, Step step, Method method) {

        var result = new LinkedHashMap<String, DataType>();
        if (method.getParameterCount() == 0) {
            return result;
        }

        int lastParameterIndex = method.getParameterCount();
        if (lastParameterType != LastParameterType.REGULAR) {
            lastParameterIndex--;
        }


        for (int i = 0; i < lastParameterIndex; i++) {
            Class<?> methodParameterType = method.getParameterTypes()[i];
            DataType type = dataTypes.byJavaType(method.getParameterTypes()[i]);
            if (result.containsKey(type.name())) {
                throwError(step, method, "Duplicate data type '{}'. Declare arguments explicitly.",type.name());
            }
            result.put(type.name(), type);
        }
        return result;
    }




    private LinkedHashMap<String, DataType> checkExplicitStepArgs(DataTypes dataTypes, Step step, Method method, String[] args) {

        var result = new LinkedHashMap<String, DataType>();

        int lastParameterIndex = method.getParameterCount();
        if (lastParameterType == LastParameterType.ASSERTION ||
                lastParameterType == LastParameterType.DATA_TABLE ||
                lastParameterType == LastParameterType.DOCUMENT
        ) {
            lastParameterIndex--;
        }

        if (step.args().length != lastParameterIndex) {
            throwError(
                step,
                method,
                "Step has {} arguments, but method expects {} parameters.",
                step.args().length,
                method.getParameterCount()
            );
        }

        for (int i = 0; i < args.length; i++) {
            String[] parts = args[i].split(":");
            if (parts.length != 2) {
                throwError(step, method, "Invalid argument format '{}'. Expected format is 'name:type'.", args[i]);
            }
            String name = parts[0];
            DataType type = dataTypes.byName(parts[1]);
            if (type.javaType() != method.getParameterTypes()[i]) {
                throwError(
                    step,
                    method,
                    "Type mismatch for argument '{}'. Expected type: {}, but got: {}",
                    name,
                    type.javaType().getSimpleName(),
                    method.getParameterTypes()[i].getSimpleName()
                );
            }
            result.put(name, type);
        }
        return result;
    }


    private void throwError(Step step, Method method, String message, Object... args) {
        Object[] messageArgs = new Object[args.length + 3];
        messageArgs[0] = step.value();
        messageArgs[1] = stepContributor.getClass().getSimpleName();
        messageArgs[2] = method.getName();
        System.arraycopy(args, 0, messageArgs, 3, args.length);
        throw new OpenBBTException("Error in step '{}' ({}.{}): " + message, messageArgs);
    }
}
