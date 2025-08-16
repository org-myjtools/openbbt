package org.myjtools.openbbt.core.test.backend;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.assertions.Assertion;
import org.myjtools.openbbt.core.assertions.AssertionAdapter;
import org.myjtools.openbbt.core.assertions.AssertionFactory;
import org.myjtools.openbbt.core.assertions.ComparableAssertionFactory;
import org.myjtools.openbbt.core.backend.RunnableStep;
import org.myjtools.openbbt.core.datatypes.CoreDataTypes;
import org.myjtools.openbbt.core.datatypes.DataTypes;
import org.myjtools.openbbt.core.messages.AssertionMessageProvider;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.plan.DataTable;
import org.myjtools.openbbt.core.plan.Document;

import java.util.List;
import java.util.Map;

class TestRunnableStep {

    private static final DataTypes dataTypes = DataTypes.CORE;


    @Test
    void testStepWithoutParameters() throws Throwable {
        RunnableStep runnableStep = new RunnableStep(
            new TestStepContributor(),
            TestStepContributor.class.getMethod("stepWithoutParameters"),
            dataTypes
        );
        runnableStep.run(Map.of(), null);
    }

    @Test
    void testStepWithOneParameter() throws Throwable {
        RunnableStep runnableStep = new RunnableStep(
            new TestStepContributor(),
            TestStepContributor.class.getMethod("stepWithOneParameter", Integer.class),
            dataTypes
        );
        runnableStep.run(Map.of("number", 42), null);
    }

    @Test
    void testStepWithTwoParameters() throws Throwable {
        RunnableStep runnableStep = new RunnableStep(
            new TestStepContributor(),
            TestStepContributor.class.getMethod("stepWithTwoParameters", Integer.class, Integer.class),
            dataTypes
        );
        runnableStep.run(Map.of("number2", 42, "number1", 84), null);
    }

    @Test
    void testStepWithOneParameterAndAssertion() throws Throwable {
        RunnableStep runnableStep = new RunnableStep(
            new TestStepContributor(),
            TestStepContributor.class.getMethod("stepWithOneParameterAndAssertion", Integer.class, Assertion.class),
            dataTypes
        );
        runnableStep.run(
            Map.of("number", 42),
            new AssertionAdapter("number-assertion", Matchers.equalTo(42))
        );
    }

    @Test
    void testStepWithOneParameterAndAssertionWithWrongValue() throws Throwable {
        RunnableStep runnableStep = new RunnableStep(
            new TestStepContributor(),
            TestStepContributor.class.getMethod("stepWithOneParameterAndAssertion", Integer.class, Assertion.class),
            dataTypes
        );
        Assertions.assertThatCode(() -> {
            runnableStep.run(
                Map.of("number", 41),
                new AssertionAdapter("number-assertion", Matchers.equalTo(42))
            );
        }).isInstanceOf(AssertionError.class)
          .hasMessageContaining("was <41>");

    }


    @Test
    void testStepWithOnlyDataTable() throws Throwable {
        RunnableStep runnableStep = new RunnableStep(
            new TestStepContributor(),
            TestStepContributor.class.getMethod("stepWithOnlyDataTable", DataTable.class),
            dataTypes
        );
        runnableStep.run(Map.of(), new DataTable());
    }


    @Test
    void testStepWithOnlyDocument() throws Throwable {
        RunnableStep runnableStep = new RunnableStep(
            new TestStepContributor(),
            TestStepContributor.class.getMethod("stepWithOnlyDocument", Document.class),
            dataTypes
        );
        runnableStep.run(Map.of(), new Document());
    }


}
