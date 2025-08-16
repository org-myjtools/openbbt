package org.myjtools.openbbt.core.backend;


import org.myjtools.openbbt.core.datatypes.DataTypes;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.step.SetUp;
import org.myjtools.openbbt.core.step.Step;
import org.myjtools.openbbt.core.step.StepContributor;
import org.myjtools.openbbt.core.step.TearDown;
import org.myjtools.openbbt.core.util.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StepContributorBackend {

    private static final Log log = Log.of("core");

    private DataTypes dataTypes;
    private Messages messages;
    private StepContributor stepContributor;
    private Map<String, RunnableStep> runnableSteps = new LinkedHashMap<>();
    private List<Method> setupMethods;
    private List<Method> teardownMethods;



    public void StepContributorBackend(
        StepContributor stepContributor,
        DataTypes dataTypes,
        Messages messages
    ) {

        this.dataTypes = dataTypes;
        this.stepContributor = stepContributor;
        this.messages = messages;

        var methods = stepContributor.getClass().getMethods();
        for (var method : methods) {
            var step = method.getAnnotation(Step.class);
            addRunnableStep(dataTypes, method, step);
            addMethod(SetUp.class, method, setupMethods);
            addMethod(TearDown.class, method, teardownMethods);
        }
    }



    private String localizedStepKey(String stepKey, Locale locale) {
        try {
            return messages.forLocale(locale).get(stepKey);
        } catch (Exception e) {
            log.error("Error while getting localized step key for '{}': {}", stepKey, e.getMessage());
            return stepKey;
        }
    }



    private void addRunnableStep(DataTypes dataTypes, Method method, Step step) {
        if (step != null) {
            try {
                checkMethodNotStatic(method);
                checkMethodPublic(method);
                runnableSteps.put(step.value(), new RunnableStep(step, method, dataTypes));
            } catch (OpenBBTException e) {
                log.error(e);
            }
        }
    }


    public void setUp()  {
        try  {
            for (Method setupMethod : setupMethods) {
                setupMethod.invoke(stepContributor, new Object[0]);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new OpenBBTException(e);
        }
    }

    public void tearDown() {
        try {
            for (Method tearDownMethod : teardownMethods) {
                tearDownMethod.invoke(stepContributor, new Object[0]);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new OpenBBTException(e);
        }
    }



    private void addMethod(Class<? extends Annotation> annotation, Method method, List<Method> methods) {
        if (method.isAnnotationPresent(annotation)) {
            try {
                checkMethodWithNoArguments(method);
                checkMethodNotStatic(method);
                checkMethodPublic(method);
                methods.add(method);
            } catch (OpenBBTException e) {
                log.error(e);
            }
        }
    }



    private void checkMethodPublic(Method method) {
        if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
            throw new OpenBBTException(
                "Setup method '{}.{}' must be public.",
                stepContributor.getClass().getSimpleName(),
                method.getName()
            );
        }
    }

    private void checkMethodNotStatic(Method method) {
        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            throw new OpenBBTException(
                "Setup method '{}.{}' must be static.",
                stepContributor.getClass().getSimpleName(),
                method.getName()
            );
        }
    }

    private void checkMethodWithNoArguments(Method method) {
        if (method.getParameterTypes().length > 0) {
            throw new OpenBBTException(
                    "Setup method '{}.{}' must not have any arguments.",
                    stepContributor.getClass().getSimpleName(),
                    method.getName()
            );
        }
    }


}

