package org.myjtools.openbbt.core.test;


import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.plan.TagExpression;


class TagExpressionTest {

	@Test
	void singleTag_matches() {
		var expr = TagExpression.parse("MainTest");
		assertTrue(expr.evaluate(List.of("MainTest")));
	}

	@Test
	void singleTag_doesNotMatch() {
		var expr = TagExpression.parse("MainTest");
		assertFalse(expr.evaluate(List.of("Other")));
	}

	@Test
	void and_bothPresent() {
		var expr = TagExpression.parse("A and B");
		assertTrue(expr.evaluate(Set.of("A", "B")));
	}

	@Test
	void and_oneMissing() {
		var expr = TagExpression.parse("A and B");
		assertFalse(expr.evaluate(Set.of("A")));
	}

	@Test
	void or_onePresent() {
		var expr = TagExpression.parse("A or B");
		assertTrue(expr.evaluate(Set.of("A")));
	}

	@Test
	void or_nonePresent() {
		var expr = TagExpression.parse("A or B");
		assertFalse(expr.evaluate(Set.of("C")));
	}

	@Test
	void not_tagAbsent() {
		var expr = TagExpression.parse("not A");
		assertTrue(expr.evaluate(Set.of("B")));
	}

	@Test
	void not_tagPresent() {
		var expr = TagExpression.parse("not A");
		assertFalse(expr.evaluate(Set.of("A")));
	}

	@Test
	void andWithParenthesizedOr() {
		var expr = TagExpression.parse("A and (B or C)");
		assertTrue(expr.evaluate(Set.of("A", "C")));
		assertFalse(expr.evaluate(Set.of("A")));
	}

	@Test
	void complexExpression() {
		var expr = TagExpression.parse("MainTest and (Quick or not Heavy)");
		assertTrue(expr.evaluate(Set.of("MainTest", "Quick")));
		assertTrue(expr.evaluate(Set.of("MainTest", "Light")));
		assertFalse(expr.evaluate(Set.of("MainTest", "Heavy")));
		assertFalse(expr.evaluate(Set.of("Quick")));
	}

	@Test
	void operatorsAreCaseInsensitive() {
		var upper = TagExpression.parse("A AND B");
		var mixed = TagExpression.parse("A And B");
		var lower = TagExpression.parse("A and B");
		var tags = Set.of("A", "B");
		assertTrue(upper.evaluate(tags));
		assertTrue(mixed.evaluate(tags));
		assertTrue(lower.evaluate(tags));
	}

	@Test
	void nullExpression_throws() {
		assertThrows(IllegalArgumentException.class, () -> TagExpression.parse(null));
	}

	@Test
	void blankExpression_throws() {
		assertThrows(IllegalArgumentException.class, () -> TagExpression.parse("  "));
	}

	@Test
	void unmatchedParenthesis_throws() {
		var ex = assertThrows(IllegalArgumentException.class, () -> TagExpression.parse("A and (B or C"));
		assertTrue(ex.getMessage().contains("Expected ')'"));
	}

	@Test
	void unexpectedToken_throws() {
		assertThrows(IllegalArgumentException.class, () -> TagExpression.parse("A B"));
	}

	@Test
	void precedence_andBindsTighterThanOr() {
		// "A or B and C" should parse as "A or (B and C)"
		var expr = TagExpression.parse("A or B and C");
		assertTrue(expr.evaluate(Set.of("A")));
		assertFalse(expr.evaluate(Set.of("B")));
		assertTrue(expr.evaluate(Set.of("B", "C")));
	}

	@Test
	void doubleNot() {
		var expr = TagExpression.parse("not not A");
		assertTrue(expr.evaluate(Set.of("A")));
		assertFalse(expr.evaluate(Set.of("B")));
	}
}
