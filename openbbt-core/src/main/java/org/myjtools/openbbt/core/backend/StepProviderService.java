package org.myjtools.openbbt.core.backend;


import org.myjtools.openbbt.core.AssertionFactories;
import org.myjtools.openbbt.core.DataTypes;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.contributors.SetUp;
import org.myjtools.openbbt.core.contributors.Step;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.contributors.TearDown;
import org.myjtools.openbbt.core.expressions.ExpressionMatcher;
import org.myjtools.openbbt.core.expressions.ExpressionMatcherBuilder;
import org.myjtools.openbbt.core.expressions.Match;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.util.Log;
import org.myjtools.openbbt.core.util.Pair;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class StepProviderService {

    private static final Log log = Log.of();

    private final DataTypes dataTypes;
    private final AssertionFactories assertionFactories;
    private final Messages messages;
    private final StepProvider stepProvider;
    private final Map<String, StepProviderMethod> runnableMethods = new LinkedHashMap<>();
    private final List<Method> setupMethods = new ArrayList<>();
    private final List<Method> teardownMethods = new ArrayList<>();
    private final ExpressionMatcherBuilder matcherBuilder;


    public StepProviderService(
        StepProvider stepProvider,
        DataTypes dataTypes,
        AssertionFactories assertionFactories,
        Messages messages
    ) {

        this.dataTypes = dataTypes;
        this.assertionFactories = assertionFactories;
        this.stepProvider = stepProvider;
        this.messages = messages;
        this.matcherBuilder = new ExpressionMatcherBuilder(dataTypes, assertionFactories);

        var methods = stepProvider.getClass().getMethods();
        for (var method : methods) {
            var step = method.getAnnotation(Step.class);
            addRunnableMethod(dataTypes, method, step);
            addMethod(SetUp.class, method, setupMethods);
            addMethod(TearDown.class, method, teardownMethods);
        }
    }


    public Optional<Pair<StepProviderMethod, Match>> matchingStep(String step, Locale locale) {
        for (var entry : runnableMethods.entrySet()) {
            String stepKey = entry.getKey();
            StepProviderMethod runnableStep = entry.getValue();
            String keyExpression = messages.forLocale(locale).get(stepKey);
            ExpressionMatcher matcher = matcherBuilder.buildExpressionMatcher(keyExpression);
            var matchingStep = matcher.matches(step,locale).map(match -> Pair.of(runnableStep,match));
            if (matchingStep.isPresent()) {
                return matchingStep;
            }
        }
        return Optional.empty();
    }


    private void addRunnableMethod(DataTypes dataTypes, Method method, Step step) {
        if (step != null) {
            try {
                checkMethodNotStatic(method);
                checkMethodPublic(method);
                runnableMethods.put(step.value(), new StepProviderMethod(stepProvider, method, dataTypes));
            } catch (OpenBBTException e) {
                log.error(e);
            }
        }
    }


    public void setUp()  {
        try  {
            for (Method setupMethod : setupMethods) {
                setupMethod.invoke(stepProvider, new Object[0]);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new OpenBBTException(e);
        }
    }

    public void tearDown() {
        try {
            for (Method tearDownMethod : teardownMethods) {
                tearDownMethod.invoke(stepProvider, new Object[0]);
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
                stepProvider.getClass().getSimpleName(),
                method.getName()
            );
        }
    }

    private void checkMethodNotStatic(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new OpenBBTException(
                "Setup method '{}.{}' must be static.",
                stepProvider.getClass().getSimpleName(),
                method.getName()
            );
        }
    }

    private void checkMethodWithNoArguments(Method method) {
        if (method.getParameterTypes().length > 0) {
            throw new OpenBBTException(
                "Setup method '{}.{}' must not have any arguments.",
                stepProvider.getClass().getSimpleName(),
                method.getName()
            );
        }
    }



}

