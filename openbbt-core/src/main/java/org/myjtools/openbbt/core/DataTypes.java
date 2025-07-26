package org.myjtools.openbbt.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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


    public static DataTypes of(List<DataType> dataTypes) {
        return new DataTypes(dataTypes.toArray(new DataType[0]));
    }


    public DataType byName(String value) {
        DataType dataType = dataTypesByName.get(value);
        if (dataType == null) {
            throw new OpenBBTException(
                "Unknown data type {}\n\tAccepted types are: {}",
                value,
                String.join(", ", dataTypesByName.keySet())
            );
        }
        return dataType;
    }
}
