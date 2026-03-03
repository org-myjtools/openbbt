package org.myjtools.openbbt.core.test.docgen;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.docgen.ParameterDoc;
import org.myjtools.openbbt.core.docgen.StepDocLoader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class StepDocLoaderTest {

    private Path testResource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void load_parsesAllEntries() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps).hasSize(13);
    }

    @Test
    void load_preservesInsertionOrder() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps.keySet()).startsWith(
            "rest.request.GET",
            "rest.request.POST",
            "rest.request.POST.file"
        );
    }

    @Test
    void load_parsesDescription() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps.get("rest.request.GET").description())
            .isEqualTo("Makes a GET request to the specified URL.");
    }

    @Test
    void load_stripsTrailingNewlinesFromDescription() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps.get("rest.request.GET").description()).doesNotEndWith("\n");
    }

    @Test
    void load_parsesExpressions() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps.get("rest.request.GET").expressions())
            .containsEntry("en", "I make a GET request to {endpoint:text}")
            .containsEntry("es", "Hago una petición GET a {endpoint:text}");
    }

    @Test
    void load_parsesSingleParameter() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var params = steps.get("rest.request.GET").parameters();
        assertThat(params).hasSize(1);
        assertThat(params.get(0).name()).isEqualTo("endpoint");
        assertThat(params.get(0).type()).isEqualTo("text");
        assertThat(params.get(0).description()).isEqualTo("The URL to which the GET request is made");
    }

    @Test
    void load_parsesMultipleParameters() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var params = steps.get("rest.request.POST.file").parameters();
        assertThat(params).hasSize(2);
        assertThat(params).extracting(ParameterDoc::name).containsExactly("endpoint", "file");
    }

    @Test
    void load_parsesAdditionalData() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var additionalData = steps.get("rest.request.POST").additionalData();
        assertThat(additionalData).isNotNull();
        assertThat(additionalData)
            .hasToString("The body of the POST request, provided as a multi-line text input");
    }

    @Test
    void load_additionalDataIsNullWhenAbsent() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps.get("rest.request.GET").additionalData()).isNull();
    }

    @Test
    void load_parsesExample() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps.get("rest.request.GET").example())
            .isEqualTo("Given I make a GET request to \"users\"");
    }

    @Test
    void load_stripsTrailingNewlinesFromExample() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        assertThat(steps.get("rest.request.GET").example()).doesNotEndWith("\n");
    }

    @Test
    void load_emptyExpressionsWhenAbsent() throws IOException, URISyntaxException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        // All entries in step-doc.yaml have expressions, so use a hand-crafted minimal YAML
        // via a temp file to verify the default
        var tempFile = java.nio.file.Files.createTempFile("step-doc-minimal", ".yaml");
        java.nio.file.Files.writeString(tempFile, "'my.step':\n  description: A step.\n  example: Given something\n");
        var steps2 = StepDocLoader.load(tempFile);
        assertThat(steps2.get("my.step").expressions()).isEmpty();
        java.nio.file.Files.delete(tempFile);
    }

}