/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.api.contributors;


import org.myjtools.openbbt.api.DataType;
import org.myjtools.openbbt.api.util.DateTimeFormats;

import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.*;



public class TemporalDataTypeAdapter<T extends TemporalAccessor> extends DataTypeAdapter<T>
implements DataType {

    private final boolean withDate;
    private final boolean withTime;


    public TemporalDataTypeAdapter(
        String name,
        Class<T> javaType,
        boolean withDate,
        boolean withTime,
        TemporalQuery<T> temporalQuery
    ) {
        super(
            name, javaType,
            locale -> DateTimeFormats.dateTimeRegex(new DateTimeFormats.Criteria(locale, withDate, withTime)),
            locale -> DateTimeFormats.dateTimePatterns(new DateTimeFormats.Criteria(locale,withDate, withTime)),
            locale -> dateTimeParser(new DateTimeFormats.Criteria(locale, withDate, withTime), temporalQuery)
        );
        this.withDate = withDate;
        this.withTime = withTime;
    }


    public List<String> getDateTimeFormats(Locale locale) {
        return DateTimeFormats.dateTimePatterns(new DateTimeFormats.Criteria(locale,withDate,withTime));
    }


    public static <T extends TemporalAccessor> DataTypeAdapter.TypeParser<T> dateTimeParser(
        DateTimeFormats.Criteria criteria,
        TemporalQuery<T> temporalQuery
    ) {
        List<DateTimeFormatter> formatters = new ArrayList<>();
        for (var formatStyles : DateTimeFormats.formatStyles(criteria.withDate(), criteria.withTime())) {
            formatters.add(DateTimeFormats.formatter(criteria.locale(), formatStyles));
        }
        if (criteria.withDate() && criteria.withTime()) {
            formatters.add(DateTimeFormatter.ISO_DATE_TIME);
        } else if (criteria.withDate()) {
            formatters.add(DateTimeFormatter.ISO_DATE);
        } else {
            formatters.add(DateTimeFormatter.ISO_TIME);
        }
        return (String input) -> DateTimeFormats.parse(formatters, input, temporalQuery);
    }

}
