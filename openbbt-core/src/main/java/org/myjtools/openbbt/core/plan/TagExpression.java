package org.myjtools.openbbt.core.plan;


import java.util.Collection;

/**
 * Represents a boolean expression of tags that can be evaluated against a set of tags.
 * <p>Example expressions:
 * <ul>
 *     <li>"tag1 and tag2"</li>
 *     <li>"tag1 or tag2"</li>
 *     <li>"not tag1"</li>
 *     <li>"(tag1 or tag2) and not tag3"</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert -
 */
 public sealed interface TagExpression {

	boolean evaluate(Collection<String> tags);

	static TagExpression parse(String expression) {
		return Parser.parse(expression);
	}


	record Tag(String name) implements TagExpression {
		@Override
		public boolean evaluate(Collection<String> tags) {
			return tags.contains(name);
		}
	}


	record And(TagExpression left, TagExpression right) implements TagExpression {
		@Override
		public boolean evaluate(Collection<String> tags) {
			return left.evaluate(tags) && right.evaluate(tags);
		}
	}


	record Or(TagExpression left, TagExpression right) implements TagExpression {
		@Override
		public boolean evaluate(Collection<String> tags) {
			return left.evaluate(tags) || right.evaluate(tags);
		}
	}


	record Not(TagExpression operand) implements TagExpression {
		@Override
		public boolean evaluate(Collection<String> tags) {
			return !operand.evaluate(tags);
		}
	}


	/**
	 * Recursive descent parser for boolean tag expressions.
	 * <p>Grammar:
	 * <pre>
	 * expression  → or_expr
	 * or_expr     → and_expr ('or' and_expr)*
	 * and_expr    → unary_expr ('and' unary_expr)*
	 * unary_expr  → 'not' unary_expr | primary
	 * primary     → TAG | '(' expression ')'
	 * </pre>
	 */
	final class Parser {

		private final String input;
		private int pos;

		private Parser(String input) {
			this.input = input;
			this.pos = 0;
		}

		static TagExpression parse(String expression) {
			if (expression == null || expression.isBlank()) {
				throw new IllegalArgumentException("Tag expression must not be null or blank");
			}
			var parser = new Parser(expression.trim());
			TagExpression result = parser.parseOrExpr();
			parser.skipWhitespace();
			if (parser.pos < parser.input.length()) {
				throw new IllegalArgumentException(
					"Unexpected token at position %d: '%s'".formatted(
						parser.pos, parser.input.substring(parser.pos)
					)
				);
			}
			return result;
		}

		private TagExpression parseOrExpr() {
			TagExpression left = parseAndExpr();
			while (matchKeyword("or")) {
				left = new Or(left, parseAndExpr());
			}
			return left;
		}

		private TagExpression parseAndExpr() {
			TagExpression left = parseUnaryExpr();
			while (matchKeyword("and")) {
				left = new And(left, parseUnaryExpr());
			}
			return left;
		}

		private TagExpression parseUnaryExpr() {
			if (matchKeyword("not")) {
				return new Not(parseUnaryExpr());
			}
			return parsePrimary();
		}

		private TagExpression parsePrimary() {
			skipWhitespace();
			if (pos < input.length() && input.charAt(pos) == '(') {
				pos++;
				TagExpression expr = parseOrExpr();
				skipWhitespace();
				if (pos >= input.length() || input.charAt(pos) != ')') {
					throw new IllegalArgumentException(
						"Expected ')' at position %d".formatted(pos)
					);
				}
				pos++;
				return expr;
			}
			return parseTag();
		}

		private TagExpression parseTag() {
			skipWhitespace();
			int start = pos;
			while (pos < input.length() && isTagChar(input.charAt(pos))) {
				pos++;
			}
			if (pos == start) {
				throw new IllegalArgumentException(
					"Expected tag name at position %d".formatted(pos)
				);
			}
			return new Tag(input.substring(start, pos));
		}

		private boolean matchKeyword(String keyword) {
			skipWhitespace();
			int end = pos + keyword.length();
			if (end > input.length()) {
				return false;
			}
			if (!input.substring(pos, end).equalsIgnoreCase(keyword)) {
				return false;
			}
			if (end < input.length() && isTagChar(input.charAt(end))) {
				return false;
			}
			pos = end;
			return true;
		}

		private void skipWhitespace() {
			while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
				pos++;
			}
		}

		private static boolean isTagChar(char c) {
			return c != '(' && c != ')' && !Character.isWhitespace(c);
		}
	}
}
