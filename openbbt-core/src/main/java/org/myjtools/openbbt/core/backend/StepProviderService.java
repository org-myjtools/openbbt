package org.myjtools.openbbt.core.backend;


import org.myjtools.openbbt.core.assertionsx.AssertionFactories;
import org.myjtools.openbbt.core.datatypes.DataTypes;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.expressionsx.ExpressionMatcher;
import org.myjtools.openbbt.core.expressionsx.ExpressionMatcherBuilder;
import org.myjtools.openbbt.core.expressionsx.Match;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.step.SetUp;
import org.myjtools.openbbt.core.step.Step;
import org.myjtools.openbbt.core.step.StepContributor;
import org.myjtools.openbbt.core.step.TearDown;
import org.myjtools.openbbt.core.util.Log;
import org.myjtools.openbbt.core.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class StepContributorBackend {

    private static final Log log = Log.of();

    private final DataTypes dataTypes;
    private final AssertionFactories assertionFactories;
    private final Messages messages;
    private final StepContributor stepContributor;
    private final Map<String, RunnableStep> runnableSteps = new LinkedHashMap<>();
    private final List<Method> setupMethods = new ArrayList<>();
    private final List<Method> teardownMethods = new ArrayList<>();
    private final ExpressionMatcherBuilder matcherBuilder;


    public StepContributorBackend(
        StepContributor stepContributor,
        DataTypes dataTypes,
        AssertionFactories assertionFactories,
        Messages messages
    ) {

        this.dataTypes = dataTypes;
        this.assertionFactories = assertionFactories;
        this.stepContributor = stepContributor;
        this.messages = messages;
        this.matcherBuilder = new ExpressionMatcherBuilder(dataTypes, assertionFactories);

        var methods = stepContributor.getClass().getMethods();
        for (var method : methods) {
            var step = method.getAnnotation(Step.class);
            addRunnableStep(dataTypes, method, step);
            addMethod(SetUp.class, method, setupMethods);
            addMethod(TearDown.class, method, teardownMethods);
        }
    }


    public Optional<Pair<RunnableStep,Match>> matchingStep(String step, Locale locale) {
        for (var entry : runnableSteps.entrySet()) {
            String stepKey = entry.getKey();
            RunnableStep runnableStep = entry.getValue();
            String keyExpression = messages.forLocale(locale).get(stepKey);
            ExpressionMatcher matcher = matcherBuilder.buildExpressionMatcher(keyExpression);
            Match match = matcher.matches(step, locale);
            if (match.matched()) {
                return Optional.of(Pair.of(runnableStep, match));
            }
        }
        return Optional.empty();
    }









    private void addRunnableStep(DataTypes dataTypes, Method method, Step step) {
        if (step != null) {
            try {
                checkMethodNotStatic(method);
                checkMethodPublic(method);
                runnableSteps.put(step.value(), new RunnableStep(stepContributor, method, dataTypes));
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
        if (Modifier.isStatic(method.getModifiers())) {
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

