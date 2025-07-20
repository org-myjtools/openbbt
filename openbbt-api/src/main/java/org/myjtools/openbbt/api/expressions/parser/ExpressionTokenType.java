package org.myjtools.openbbt.api.expressions.parser;

public enum ExpressionTokenType {
	NEGATION,
	START_OPTIONAL,
	END_OPTIONAL,
	CHOICE_SEPARATOR,
	START_GROUP,
	END_GROUP,
	START_ARGUMENT,
	END_ARGUMENT,
	START_ASSERTION,
	END_ASSERTION,
	TEXT,
	WILDCARD
}
