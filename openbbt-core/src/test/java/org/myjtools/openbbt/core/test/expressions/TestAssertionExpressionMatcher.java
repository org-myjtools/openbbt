package org.myjtools.openbbt.core.test.expressions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.AssertionFactories;
import org.myjtools.openbbt.core.DataTypes;
import org.myjtools.openbbt.core.adapters.BasicDataTypes;
import org.myjtools.openbbt.core.adapters.Messages;
import org.myjtools.openbbt.core.adapters.NumberAssertionFactory;
import org.myjtools.openbbt.core.expressions.ExpressionMatcherBuilder;
import org.myjtools.openbbt.core.expressions.LiteralValue;
import org.myjtools.openbbt.core.expressions.Match;
import org.myjtools.openbbt.core.messages.AssertionMessageProvider;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;


class TestAssertionExpressionMatcher {

    static Messages assertionMessages = new Messages(List.of(new AssertionMessageProvider()));
    static DataTypes dataTypes = DataTypes.of(new BasicDataTypes().dataTypes().toList());
    static final AssertionFactories assertionFactories = AssertionFactories.of(
            new NumberAssertionFactory("number-assertion", assertionMessages)
    );

    static ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(dataTypes, assertionFactories);



    static List<Object[]> numberAssertionTestData() {
        return List.of(
            new Object[]{"the number 5 is greater than 3", true},
            new Object[]{"the number 2 is less than 4", true},
            new Object[]{"the number 10 is equal to 10", true},
            new Object[]{"the number 7 is not equal to 8", true},
            new Object[]{"the number 3 is greater than or equal to 3", true},
            new Object[]{"the number 6 is less than or equal to 5", false}
        );
    }

    @ParameterizedTest
    @MethodSource("numberAssertionTestData")
    void testNumberAssertionExpression(String input, Boolean assertResult) {
        var expression = "the number {number} {{number-assertion}}";
        var matcher = builder.buildExpressionMatcher(expression);
        Match match = matcher.matches(input, Locale.ENGLISH);
        Integer number = (Integer) ((LiteralValue) match.argument("number")).value();
        Assertion assertion = match.assertion("number-assertion");
        boolean result = assertion.test(number);
        assertThat(match.matched()).isTrue();
        assertThat(result).isEqualTo(assertResult);
    }



}
