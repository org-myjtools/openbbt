package org.myjtools.openbbt.api.expressions.parser;

public record ExpressionToken(ExpressionTokenType type, String value, int start, int end) {


	public ExpressionToken(ExpressionTokenType type, int start, int end) {
		this(type, null, start, end);
	}


	public ExpressionToken(ExpressionTokenType type, int position) {
		this(type, null, position, position);
	}


	@Override
	public String toString() {
		if (value != null) {
			return "Token[%s '%s' %s-%s]".formatted(type,value,start,end);
		} else {
			return "Token[%s %s-%s]".formatted(type,start,end);
		}
	}


	public boolean startsWithBlank() {
		return value.startsWith(" ");
	}


	public boolean endsWithBlank() {
		return value.endsWith(" ");
	}


	public boolean isSingleWord() {
		return value.strip().chars().filter(c -> c == ' ').count() == 0L;
	}


	public String firstWord() {
		return value.strip().split(" ")[0];
	}


	public ExpressionToken firstWordToken() {
		String firstWord = firstWord();
		return new ExpressionToken(type, firstWord, start, start+firstWord.length());
	}


	public String lastWord() {
		var words = value.strip().split(" ");
		return words[words.length-1];
	}


	public ExpressionToken removeLeadingChars(int length) {
		return new ExpressionToken(type, value.substring(length), start+length, end);
	}


	public ExpressionToken removeTrailingChars(int length) {
		return new ExpressionToken(type, value.substring(0,value.length()-length), start, end-length);
	}

}
