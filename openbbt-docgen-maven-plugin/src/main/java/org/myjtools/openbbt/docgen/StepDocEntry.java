package org.myjtools.openbbt.docgen;

import java.util.List;
import java.util.Map;

record StepDocEntry(
    String description,
    Map<String, String> expressions,
    List<ParameterDoc> parameters,
    Map<String, String> additionalData,
    String example
) {
}