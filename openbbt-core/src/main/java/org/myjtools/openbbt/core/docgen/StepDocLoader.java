package org.myjtools.openbbt.core.docgen;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StepDocLoader {

    @SuppressWarnings("unchecked")
    public static Map<String, StepDocEntry> load(Path path) throws IOException {
        return load(Files.newInputStream(path));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, StepDocEntry> load(InputStream inputStream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Map<String, Object>> raw;
        try (var reader = new BufferedInputStream(inputStream)) {
            raw = yaml.load(reader);
        }
        var result = new LinkedHashMap<String, StepDocEntry>();
        for (var entry : raw.entrySet()) {
            result.put(entry.getKey(), parseEntry(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static StepDocEntry parseEntry(Map<String, Object> map) {
        String description = trimTrailingNewline((String) map.get("description"));
        Map<String, String> expressions = (Map<String, String>) map.getOrDefault("expressions", Map.of());
        String additionalData = trimTrailingNewline((String) map.get("additional-data"));
        String example = trimTrailingNewline((String) map.get("example"));
        List<ParameterDoc> parameters = new ArrayList<>();
        var rawParams = (List<Map<String, Object>>) map.get("parameters");
        if (rawParams != null) {
            for (var p : rawParams) {
                parameters.add(new ParameterDoc(
                    (String) p.get("name"),
                    (String) p.get("type"),
                    (String) p.get("description")
                ));
            }
        }
        return new StepDocEntry(description, expressions, parameters, additionalData, example);
    }

    private static String trimTrailingNewline(String s) {
        if (s == null) return null;
        return s.stripTrailing();
    }
}