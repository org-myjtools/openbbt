package org.myjtools.openbbt.core.docgen;

import java.util.List;

public record ConfigDocEntry(
    String description,
    String type,
    boolean required,
    Object defaultValue,
    String constraintPattern,
    Number constraintMin,
    Number constraintMax,
    List<String> constraintValues
) {}