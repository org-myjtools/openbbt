package org.myjtools.openbbt.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DataTypes {

    private final Map<String, DataType> dataTypesByName;

    private DataTypes(DataType[] dataTypes) {
        this.dataTypesByName = new HashMap<>();
        for (DataType dataType : dataTypes) {
            this.dataTypesByName.put(dataType.name(), dataType);
        }
    }

    public static DataTypes of(DataType... dataTypes) {
        return new DataTypes(dataTypes);
    }

    public Optional<DataType> byName(String value) {
        return Optional.ofNullable(dataTypesByName.get(value));
    }
}
