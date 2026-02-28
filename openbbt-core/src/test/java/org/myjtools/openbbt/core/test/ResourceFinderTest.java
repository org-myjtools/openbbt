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
		var resourceSet = resourceFinder.findResources("**/*.txt");
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
		var resourceSet = resourceFinder.findResources("**/*.{txt,yml}");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/file_c.yml"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithRecursiveGlob() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*.txt");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithSubdirGlob() {
		// **/subdir/*.txt only matches files inside the subdir directory
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/subdir/*.txt");
		assertThat(resourceSet.resources()).extracting(Resource::relativePath)
			.containsExactly(
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testFindResourcesWithAllFilesGlob() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
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
		var resourceSet1 = resourceFinder.findResources("**/*.txt");
		var resourceSet2 = resourceFinder.findResources("**/*.txt");
		var resourceSet3 = resourceFinder.findResources("**/*.yml");
		assertThat(resourceSet1.hash()).isEqualTo(resourceSet2.hash());
		assertThat(resourceSet1.hash()).isNotEqualTo(resourceSet3.hash());
	}

	@Test
	void testResourceSetFilter_withRecursiveGlob_matchesAllTxtFiles() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
		assertThat(resourceSet.filter("**/*.txt").map(Resource::relativePath).toList())
			.containsExactly(
				Path.of("src/test/resources/files/file_a.txt"),
				Path.of("src/test/resources/files/file_b.txt"),
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testResourceSetFilter_withSubdirGlob_matchesOnlySubdirFiles() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
		assertThat(resourceSet.filter("**/subdir/*.txt").map(Resource::relativePath).toList())
			.containsExactly(
				Path.of("src/test/resources/files/subdir/file_d.txt")
			);
	}

	@Test
	void testResourceSetFilter_withPredicate_matchesByExtension() {
		var resourceFinder = new ResourceFinder(Path.of("src/test/resources/files"));
		var resourceSet = resourceFinder.findResources("**/*");
		assertThat(resourceSet.filter(r -> "yml".equals(r.extension())).map(Resource::relativePath).toList())
			.containsExactly(
				Path.of("src/test/resources/files/file_c.yml")
			);
	}

}
