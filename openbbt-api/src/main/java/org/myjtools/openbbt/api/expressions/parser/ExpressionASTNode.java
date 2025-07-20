package org.myjtools.openbbt.api.expressions.parser;

import org.myjtools.openbbt.api.expressions.ExpressionException;

import java.util.*;
import java.util.stream.Stream;


public class ExpressionASTNode {


	public enum Type {

		SEQUENCE,
		LITERAL,
		CHOICE,
		WILDCARD,
		ASSERTION,
		ARGUMENT,
		OPTIONAL,
		NEGATION;


		public ExpressionASTNode empty() {
			return new ExpressionASTNode(this,null);
		}

		public ExpressionASTNode of(ExpressionToken token, ExpressionASTNode... children) {
			return new ExpressionASTNode(this, token == null ? null : token.value() ,children);
		}

		public ExpressionASTNode of(String value, ExpressionASTNode... children) {
			return new ExpressionASTNode(this, value ,children);
		}

		public ExpressionASTNode of(ExpressionASTNode... children) {
			return new ExpressionASTNode(this,null,children);
		}

	}

	final Type type;
	String value;
	final List<ExpressionASTNode> children = new ArrayList<>();



	private ExpressionASTNode(Type type, String value, ExpressionASTNode... children) {
		this.type = type;
		this.value = value;
		Stream.of(children).map(ExpressionASTNode::reduced).forEach(this::add);
	}


	void add(ExpressionASTNode child) {
		children.add(child.reduced());
	}


	public ExpressionASTNode firstChild() {
		return children.isEmpty() ? null : children.getFirst();
	}


	public ExpressionASTNode lastChild() {
		return children.isEmpty() ? null : children.get(children.size()-1);
	}


	public List<ExpressionASTNode> children() {
		return Collections.unmodifiableList(children);
	}

	public Type type() {
		return type;
	}

	public String value() {
		return value;
	}


	public void remove(ExpressionASTNode child) {
		children.remove(child);
	}


	ExpressionASTNode reduced() {
		if (type == Type.SEQUENCE && children.size() == 1) {
			return firstChild();
		} else {
			return this;
		}
	}


	ExpressionASTNode assertType(Type expected) {
		if (this.type != expected)
			throw new ExpressionException("Unexpected node {}; expected {}", this.type, expected);
		return this;
	}



	@Override
	public String toString() {
		return toString(new StringBuilder(),0).toString();
	}


	protected StringBuilder toString(StringBuilder string, int level) {
		String margin = "  ".repeat(level);
		string.append(margin);
		string.append(type);
		if (value != null) {
			string.append("<").append(value).append(">");
		}
		if (!children.isEmpty()) {
			string.append(" [\n");
			children.forEach(child -> child.toString(string, level+1));
			string.append(margin).append("]\n");
		} else {
			string.append("\n");
		}
		return string;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExpressionASTNode astNode = (ExpressionASTNode) o;
		return type == astNode.type
			&& Objects.equals(value, astNode.value)
			&& children.equals(astNode.children);
	}


	@Override
	public int hashCode() {
		return Objects.hash(type, value, children);
	}

}
