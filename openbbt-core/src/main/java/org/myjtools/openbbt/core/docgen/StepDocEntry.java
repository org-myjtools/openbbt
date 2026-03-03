package org.myjtools.openbbt.core.docgen;

import java.util.List;
import java.util.Map;

public record StepDocEntry(
    String description,
    Map<String, String> expressions,
    List<ParameterDoc> parameters,
    String additionalData,
    String example
) {
}