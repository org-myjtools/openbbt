package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.util.Hash;
import java.util.*;
import java.util.function.Consumer;

public class ResourceSet implements Iterable<Resource>{

	private final List<Resource> resources;
	private final String hash;

	public ResourceSet(Collection<Resource> resources) {
		this.resources = resources.stream().sorted().distinct().toList();
		this.hash = Hash.of(resources.stream().map(Resource::relativePath).toList());
	}

	public List<Resource> resources() {
		return resources;
	}

	public int size() {
		return resources.size();
	}

	public Resource get(int index) {
		return resources.get(index);
	}

	public boolean isEmpty() {
		return resources.isEmpty();
	}

	public String hash() {
		return hash;
	}

	@Override
	public Iterator<Resource> iterator() {
		return resources.iterator();
	}

	@Override
	public void forEach(Consumer<? super Resource> action) {
		resources.forEach(action);
	}

	@Override
	public Spliterator<Resource> spliterator() {
		return resources.spliterator();
	}

}
