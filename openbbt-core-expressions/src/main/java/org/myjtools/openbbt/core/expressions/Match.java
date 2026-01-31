package org.myjtools.openbbt.core.expressions;

import org.myjtools.openbbt.core.Assertion;

import java.util.Map;

/**
 * Result of matching an input string against an {@link ExpressionMatcher}.
 *
 * <p>A Match contains:</p>
 * <ul>
 *   <li>Whether the input matched the expression pattern</li>
 *   <li>Extracted argument values (from {@code {arg}} patterns)</li>
 *   <li>Created assertions (from {@code {{assertion}}} patterns)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Match match = matcher.matches("the user John has 5 items", Locale.ENGLISH);
 * if (match.matched()) {
 *     ArgumentValue name = match.argument("name");
 *     Assertion countAssertion = match.assertion("number-assertion");
 * }
 * }</pre>
 *
 * @param matched    whether the input matched the expression
 * @param argument   map of argument names to their extracted values
 * @param assertions map of assertion names to their created assertions
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionMatcher
 * @see ArgumentValue
 * @see Assertion
 */
public record Match (
        boolean matched,
        Map<String,ArgumentValue> argument,
        Map<String,Assertion> assertions
) {

    /**
     * Creates a match result with no arguments or assertions.
     *
     * @param matched whether the input matched
     */
    public Match(boolean matched) {
        this(matched, Map.of(), Map.of());
    }

    /**
     * Retrieves an argument value by name.
     *
     * @param name the argument name
     * @return the argument value, or {@code null} if not found
     */
    public ArgumentValue argument(String name) {
        return argument.get(name);
    }

    /**
     * Retrieves an assertion by name.
     *
     * @param name the assertion name
     * @return the assertion, or {@code null} if not found
     */
    public Assertion assertion(String name) {
        return assertions.get(name);
    }

}
