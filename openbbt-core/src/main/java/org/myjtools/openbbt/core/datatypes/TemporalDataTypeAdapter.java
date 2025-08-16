/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.core.datatypes;


import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.*;
import java.util.function.Function;


public class TemporalDataTypeAdapter<T extends TemporalAccessor> extends DataTypeAdapter<T>
implements DataType {


    public TemporalDataTypeAdapter(
        String name,
        Class<T> javaType,
        boolean withDate,
        boolean withTime,
        TemporalQuery<T> temporalQuery
    ) {
        super(
            name,
            javaType,
            regex(withDate, withTime),
            hint(withDate, withTime),
            dateTimeParser(withDate, withTime, temporalQuery)
        );
    }


    private static String regex(boolean withDate, boolean withTime) {
        if (withDate && withTime) {
            return "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d{3})?)?"; // Match ISO date-time format
        } else if (withDate) {
            return "\\d{4}-\\d{2}-\\d{2}"; // Match ISO date format
        } else {
            return "\\d{2}:\\d{2}(:\\d{2}(\\.\\d{3})?)?"; // Match time format
        }
    }


    private static String hint(boolean withDate, boolean withTime) {
        if (withDate && withTime) {
            return "yyyy-MM-ddTHH:mm:ss"; // Match ISO date-time format
        } else if (withDate) {
            return "yyyy-MM-dd"; // Match ISO date format
        } else {
            return "HH:mm:ss"; // Match time format
        }
    }




    public static <T extends TemporalAccessor> Function<String,T> dateTimeParser(
        boolean withDate,
        boolean withTime,
        TemporalQuery<T> temporalQuery
    ) {
        return (String input) -> {
            List<DateTimeFormatter> formatters = new ArrayList<>();
            if (withDate && withTime) {
                return DateTimeFormatter.ISO_DATE_TIME.parse(input, temporalQuery);
            } else if (withDate) {
                return DateTimeFormatter.ISO_DATE.parse(input, temporalQuery);
            } else {
                return DateTimeFormatter.ISO_TIME.parse(input, temporalQuery);
            }
        };
    }

}
