package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.plan.NodeType;

import java.util.*;



/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public sealed interface PlanNodeCriteria {


	record AllCriteria() implements PlanNodeCriteria {}

	record HasTagCriteria(String tag) implements PlanNodeCriteria {}

	record HasPropertyCriteria(String property, String value) implements PlanNodeCriteria {}

	record HasNodeTypeCriteria(NodeType nodeType) implements PlanNodeCriteria {}

	record HasFieldCriteria(String field, Object value) implements PlanNodeCriteria {}

	record HasValuedFieldCriteria(String field) implements PlanNodeCriteria {}

	record IsDescendantCriteria(UUID parent, int depth) implements PlanNodeCriteria {}

	record IsAscendantCriteria(UUID parent, int depth) implements PlanNodeCriteria {}

	record AndCriteria(PlanNodeCriteria... conditions) implements PlanNodeCriteria {}

	record OrCriteria(PlanNodeCriteria... conditions) implements PlanNodeCriteria {}

	record NotCriteria(PlanNodeCriteria condition) implements PlanNodeCriteria {}


	static PlanNodeCriteria all() {
		return new AllCriteria();
	}

	static PlanNodeCriteria withTag(String tag) {
		return new HasTagCriteria(Objects.requireNonNull(tag));
	}

	static PlanNodeCriteria withProperty(String property, String value) {
		return new HasPropertyCriteria(Objects.requireNonNull(property), value);
	}

	static PlanNodeCriteria withNodeType(NodeType nodeType) {
		return new HasNodeTypeCriteria(nodeType);
	}

	static PlanNodeCriteria withField(String field, Object value) {
		return new HasFieldCriteria(field,value);
	}

	static PlanNodeCriteria withField(String field) {
		return new HasValuedFieldCriteria(field);
	}

	static PlanNodeCriteria childOf(UUID parent) {
		return new IsDescendantCriteria(parent,1);
	}

	static PlanNodeCriteria descendantOf(UUID parent) {
		return new IsDescendantCriteria(parent,-1);
	}

	static PlanNodeCriteria descendantOf(UUID parent, int depth) {
		return new IsDescendantCriteria(parent,depth);
	}

	static PlanNodeCriteria parentOf(UUID child) {
		return new IsAscendantCriteria(child,1);
	}

	static PlanNodeCriteria ascendantOf(UUID child) {
		return new IsAscendantCriteria(child,-1);
	}

	static PlanNodeCriteria ascendantOf(UUID child, int depth) {
		return new IsAscendantCriteria(child,depth);
	}

	static PlanNodeCriteria and(PlanNodeCriteria... conditions) {
		return new AndCriteria(conditions);
	}

	static PlanNodeCriteria or(PlanNodeCriteria... conditions) {
		return new OrCriteria(conditions);
	}

	static PlanNodeCriteria not(PlanNodeCriteria condition) {
		return new NotCriteria(condition);
	}

}