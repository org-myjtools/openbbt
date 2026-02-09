package org.myjtools.openbbt.core.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.Resource;
import org.myjtools.openbbt.core.ResourceFinder;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceFinderTest {

	@Test
	void testFindResources() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("*.txt");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithOrPattern() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("*.{txt,yml}");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/file_c.yml"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}


	@Test
	void testResourceSetHash() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet1 = resourceFinder.findResources("*.txt");
		var resourceSet2 = resourceFinder.findResources("*.txt");
		var resourceSet3 = resourceFinder.findResources("*.yml");
		assertThat(resourceSet1.hash()).isEqualTo(resourceSet2.hash());
		assertThat(resourceSet1.hash()).isNotEqualTo(resourceSet3.hash());
	}


}
