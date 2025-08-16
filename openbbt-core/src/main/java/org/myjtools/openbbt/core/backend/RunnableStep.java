package org.myjtools.openbbt.core.backend;

import org.myjtools.openbbt.core.datatypes.DataType;
import org.myjtools.openbbt.core.datatypes.DataTypes;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.step.Step;
import org.myjtools.openbbt.core.util.Pair;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;

public class RunnableStep {

    private Object stepContributor;
    private String stepKey;
    private Method method;
    private List<Pair<String, DataType>> arguments;


    public RunnableStep(Object stepContributor, Method method, DataTypes dataTypes) {
        this.stepContributor = stepContributor;
        var annotation = method.getAnnotation(Step.class);
        this.stepKey = annotation.value();
        this.method = method;
        this.arguments = Pair.ofMap(checkStepArgs(dataTypes, annotation, method));
    }


    public void run(Object[] args) throws Exception {
        method.invoke(stepContributor, args);
    }


    private LinkedHashMap<String, DataType> checkStepArgs(DataTypes datatypes, Step step, Method method) {
        var result = new LinkedHashMap<String, DataType>();
        if (step.args().length == 0) {
            return checkImplicitStepArgs(datatypes, step, method);
        } else {
            return checkExplicitStepArgs(datatypes, step, method, step.args());
        }
    }


    private LinkedHashMap<String, DataType> checkImplicitStepArgs(DataTypes dataTypes, Step step, Method method) {
        var result = new LinkedHashMap<String, DataType>();
        for (int i = 0; i < method.getParameterTypes().length; i++) {
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

        if (step.args().length != method.getParameterCount()) {
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
