package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.contributors.ContentComparator;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Comparators {

	private final List<ContentComparator> contentComparators;

	private Comparators(List<ContentComparator> contentComparators) {
		this.contentComparators = contentComparators;
	}

	public static Comparators of(Collection<ContentComparator> contentComparators) {
		return new Comparators(List.copyOf(contentComparators));
	}

	public Optional<ContentComparator> comparatorFor(String contentType) {
		return contentComparators.stream()
				.filter(comparator -> comparator.accepts(contentType))
				.findFirst();
	}

}
