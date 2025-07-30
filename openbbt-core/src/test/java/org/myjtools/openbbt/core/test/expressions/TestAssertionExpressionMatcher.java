package org.myjtools.openbbt.core.test.expressions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.AssertionFactories;
import org.myjtools.openbbt.core.DataTypes;
import org.myjtools.openbbt.core.adapters.CoreAssertionFactories;
import org.myjtools.openbbt.core.adapters.CoreDataTypes;
import org.myjtools.openbbt.core.adapters.Messages;
import org.myjtools.openbbt.core.adapters.ComparableAssertionFactory;
import org.myjtools.openbbt.core.expressions.ExpressionMatcherBuilder;
import org.myjtools.openbbt.core.expressions.LiteralValue;
import org.myjtools.openbbt.core.expressions.Match;
import org.myjtools.openbbt.core.messages.AssertionMessageProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


class TestAssertionExpressionMatcher {

    static final Messages assertionMessages = new Messages(List.of(new AssertionMessageProvider()));
    static final DataTypes dataTypes = DataTypes.of(new CoreDataTypes().dataTypes().toList());
    static final AssertionFactories assertionFactories = AssertionFactories.of(
        new CoreAssertionFactories(assertionMessages).assertionFactories().toList()
    );

    static ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(dataTypes, assertionFactories);



    static List<Object[]> numberAssertionTestData() {
        return List.of(
            new Object[]{"the number 5 is greater than 3", true},
            new Object[]{"the number 2 is less than 4", true},
            new Object[]{"the number 10 is equal to 10", true},
            new Object[]{"the number 7 is not equal to 8", true},
            new Object[]{"the number 3 is greater than or equal to 3", true},
            new Object[]{"the number 6 is less than or equal to 5", false},
            new Object[]{"the number 3 is not null", true}
        );
    }

    @ParameterizedTest
    @MethodSource("numberAssertionTestData")
    void testNumberAssertionExpression(String input, Boolean assertResult) {
        var expression = "the number {number} {{number-assertion}}";
        var matcher = builder.buildExpressionMatcher(expression);
        Match match = matcher.matches(input, Locale.ENGLISH);
        assertThat(match.matched()).isTrue();
        Integer number = (Integer) ((LiteralValue) match.argument("number")).value();
        Assertion assertion = match.assertion("number-assertion");
        boolean result = assertion.test(number);
        assertThat(result).isEqualTo(assertResult);
    }


    static List<Object[]> decimalAssertionTestData() {
        return List.of(
                new Object[]{"the decimal 5.2 is greater than 3.4", true},
                new Object[]{"the decimal 2.1 is less than 4.22", true},
                new Object[]{"the decimal 10.5 is equal to 10.5", true},
                new Object[]{"the decimal 7.7 is not equal to 8.0", true},
                new Object[]{"the decimal 3.1 is greater than or equal to 3.0", true},
                new Object[]{"the decimal 6.3 is less than or equal to 5.8", false},
                new Object[]{"the decimal 3.4 is not null", true}
        );
    }

    @ParameterizedTest
    @MethodSource("decimalAssertionTestData")
    void testDecimalAssertionExpression(String input, Boolean assertResult) {
        var expression = "the decimal {decimal} {{decimal-assertion}}";
        var matcher = builder.buildExpressionMatcher(expression);
        Match match = matcher.matches(input, Locale.ENGLISH);
        assertThat(match.matched()).isTrue();
        BigDecimal decimal = (BigDecimal) ((LiteralValue) match.argument("decimal")).value();
        Assertion assertion = match.assertion("decimal-assertion");
        boolean result = assertion.test(decimal);
        assertThat(result).isEqualTo(assertResult);
    }


}
