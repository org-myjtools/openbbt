package org.myjtools.openbbt.core.datatypes;

import org.myjtools.openbbt.core.OpenBBTException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataTypes {

    public static final DataTypes CORE = DataTypes.of(new CoreDataTypes().dataTypes().toList());

    private final Map<String, DataType> dataTypesByName;
    private final Map<Class<?>, DataType> dataTypesByJavaType;

    private DataTypes(DataType[] dataTypes) {
        this.dataTypesByName = new HashMap<>();
        this.dataTypesByJavaType = new HashMap<>();
        for (DataType dataType : dataTypes) {
            this.dataTypesByName.put(dataType.name(), dataType);
            this.dataTypesByJavaType.put(dataType.javaType(), dataType);
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

    public DataType byJavaType(Class<?> javaType) {
        DataType dataType = dataTypesByJavaType.get(javaType);
        if (dataType == null) {
            throw new OpenBBTException(
                "Unknown data type for java type {}\n\tAccepted types are: {}",
                javaType,
                dataTypesByJavaType.keySet().stream().map(Class::toString).collect(Collectors.joining(", "))
            );
        }
        return dataType;
    }

}
