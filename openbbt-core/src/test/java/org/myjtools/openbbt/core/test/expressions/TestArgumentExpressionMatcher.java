package org.myjtools.openbbt.core.test.expressions;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.AssertionFactories;
import org.myjtools.openbbt.core.DataTypes;
import org.myjtools.openbbt.core.adapters.BasicDataTypes;
import org.myjtools.openbbt.core.expressions.*;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


class TestArgumentExpressionMatcher {


    public static final Locale LOCALE = Locale.getDefault();

    static DataTypes dataTypes = DataTypes.of(new BasicDataTypes().dataTypes().toList());
    static ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(
        dataTypes,
        AssertionFactories.of()
    );


    @Test
    void testUnnamedArgumentExpression() {
        var expression = "this is a unnamed number: {number}";
        var matcher = builder.buildExpressionMatcher(expression);
        Match match = matcher.matches("this is a unnamed number: 42", LOCALE);
        assertThat(match.matched()).isTrue();
        assertThat(match.argument("number")).isInstanceOf(LiteralValue.class);
        assertThat(((LiteralValue) match.argument("number")).value()).isEqualTo(42);
    }

    @Test
    void testNamedArgumentsExpression() {
        var expression = "this is a named number: {number1:number} and another: {number2:number}";
        var matcher = builder.buildExpressionMatcher(expression);
        Match match = matcher.matches("this is a named number: 42 and another: 84", LOCALE);
        assertThat(match.matched()).isTrue();
        assertThat(match.argument("number1")).isInstanceOf(LiteralValue.class);
        assertThat(((LiteralValue) match.argument("number1")).value()).isEqualTo(42);
        assertThat(match.argument("number2")).isInstanceOf(LiteralValue.class);
        assertThat(((LiteralValue) match.argument("number2")).value()).isEqualTo(84);
    }


    @Test
    void testVariableArgumentExpression() {
        var expression = "this is a unnamed number: {number}";
        var matcher = builder.buildExpressionMatcher(expression);
        Match match = matcher.matches("this is a unnamed number: ${varnumber}", LOCALE);
        assertThat(match.matched()).isTrue();
        assertThat(match.argument("number")).isInstanceOf(VariableValue.class);
        assertThat(((VariableValue) match.argument("number")).variable()).isEqualTo("varnumber");
        assertThat(match.argument("number").type().name()).isEqualTo("number");
    }



}
