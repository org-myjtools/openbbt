package org.myjtools.openbbt.docgen;

import java.util.List;
import java.util.Map;

record ConfigDocEntry(
    String description,
    String type,
    boolean required,
    Object defaultValue,
    String constraintPattern,
    Number constraintMin,
    Number constraintMax,
    List<String> constraintValues
) {}