package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.contributors.ContentComparator;
import java.util.Collection;
import java.util.List;

public class Comparators {

	private final List<ContentComparator> contentComparators;

	private Comparators(List<ContentComparator> contentComparators) {
		this.contentComparators = contentComparators;
	}

	public Comparators of(Collection<ContentComparator> contentComparators) {
		return new Comparators(List.copyOf(contentComparators));
	}
}
