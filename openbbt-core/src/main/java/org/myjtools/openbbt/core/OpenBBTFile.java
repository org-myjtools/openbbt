package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.core.project.Project;
import org.myjtools.openbbt.core.project.TestSuite;
import org.yaml.snakeyaml.Yaml;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenBBTFile {

	public static OpenBBTFile read(Reader reader) {
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Object> raw = yaml.load(reader);

		OpenBBTFile file = new OpenBBTFile();

		@SuppressWarnings("unchecked")
		Map<String, Object> projectMap = (Map<String, Object>) raw.get("project");
		if (projectMap != null) {
			file.project = parseProject(projectMap);
		}
		@SuppressWarnings("unchecked")
		List<String> plugins = (List<String>) raw.get("plugins");
		file.plugins = plugins;
		@SuppressWarnings("unchecked")
		Map<String, Object> configuration = (Map<String, Object>) raw.get("configuration");
		file.configuration = configuration;
		@SuppressWarnings("unchecked")
		Map<String, Map<String,String>> profiles = (Map<String, Map<String,String>>) raw.get("profiles");
		file.profiles = profiles;

		return file;
	}

	private static Project parseProject(Map<String, Object> map) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> suitesRaw = (List<Map<String, Object>>) map.getOrDefault("test-suites", List.of());
		List<TestSuite> testSuites = suitesRaw.stream().map(OpenBBTFile::parseTestSuite).toList();
		return new Project((String) map.get("name"), (String) map.get("description"), (String) map.get("organization"), testSuites);
	}

	private static TestSuite parseTestSuite(Map<String, Object> map) {
		TagExpression tagExpression = TagExpression.parse((String) map.get("tag-expression"));
		return new TestSuite((String) map.get("name"), (String) map.get("description"), tagExpression);
	}

	private Project project;
	private List<String> plugins;
	private Map<String, Object> configuration;
	private Map<String, Map<String,String>> profiles;


	public Project project() {
		return project;
	}

	public List<String> plugins() {
		return List.copyOf(plugins);
	}

	public Map<String, Object> configuration() {
		return Map.copyOf(configuration);
	}

	public Map<String, Map<String,String>> profiles() {
		return Map.copyOf(profiles);
	}


	public OpenBBTContext createContext(List<String> testSuites, String profile, Config env, Config parameters) {
		var contextProject = new Project(
			project.name(),
			project.description(),
			project.organization(),
			List.copyOf(project.testSuites())
		);
		var contextTestSuites = new ArrayList<String>();
		for (String testSuiteName : testSuites) {
			var testSuite = project.testSuites().stream().filter(suite -> suite.name().equals(testSuiteName)).findFirst();
			if (testSuite.isEmpty()) {
				throw new OpenBBTException("Test suite '" + testSuiteName + "' not found in project file");
			}
			contextTestSuites.add(testSuite.map(TestSuite::name).orElseThrow());
		}
		Map<String,Object> profiledConfiguration = this.configuration;
		if (!profile.isBlank()) {
			if (!this.profiles.containsKey(profile)) {
				throw new OpenBBTException("Profile '" + profile + "' not found in project file");
			}
			profiledConfiguration = applyProfile(this.configuration, this.profiles.get(profile));
		}
		profiledConfiguration = applyProfile(profiledConfiguration, env.asMap());
		profiledConfiguration = applyProfile(profiledConfiguration, parameters.asMap());
		List<String> plugins = List.of();
		if (this.plugins != null) {
			plugins = this.plugins.stream().map(this::normalizePluginName).toList();
		}
		return new OpenBBTContext(contextProject, Config.ofMap(flattenMap(profiledConfiguration, "")), contextTestSuites, profile, plugins);
	}


	private Map<String, Object> flattenMap(Map<String, Object> map, String prefix) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			if (entry.getValue() instanceof Map<?, ?> nested) {
				result.putAll(flattenMap((Map<String, Object>) nested, key));
			} else {
				result.put(key, entry.getValue());
			}
		}
		return result;
	}


	private String normalizePluginName(String name) {
		int groupNameIndex = name.indexOf(":");
		if (groupNameIndex == -1) {
			return "org.myjtools.openbbt.plugins:" + name;
		}
		return name;
	}


	private Map<String, Object> applyProfile(Map<String, Object> configuration, Map<String,String> profile) {
		if (profile == null || profile.isEmpty()) {
			return configuration;
		}
		Map<String,Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : configuration.entrySet()) {
			if (entry.getValue() instanceof String value) {
				for (Map.Entry<String, String> profileEntry : profile.entrySet()) {
					value = value.replace("{{" + profileEntry.getKey() + "}}", profileEntry.getValue());
				}
				result.put(entry.getKey(), value);
			} else if (entry.getValue() instanceof Map<?,?> map) {
				result.put(entry.getKey(), applyProfile((Map<String, Object>) map, profile));
			} else {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}


}


