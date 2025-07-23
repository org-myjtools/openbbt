package org.myjtools.openbbt.api.contributors;


import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.api.DataType;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

@Extension(scope = Scope.SINGLETON)
public class BasicDataTypes implements DataTypeProvider {


    public static final DataType WORD = new RegexDataTypeAdapter<>(
            "word", "[\\w-]+", String.class, x -> x, List.of("<word>")
    );

    public static final DataType URL = new RegexDataTypeAdapter<> (
            "url",
            "\\w+:(\\/?\\/?)[^\\s]+",
            URI.class,
            URI::new,
            List.of("<protocol://host/path>")
    );

    public static final DataType ID = new RegexDataTypeAdapter<>(
            "id", "\\w[\\w_-\\.]+", String.class, x -> x, List.of("<id>")
    );

    public static final DataType FILE = new RegexDataTypeAdapter<>(
            "file",
            "\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"|'([^'\\\\]*(\\\\.[^'\\\\]*)*)'",
            Path.class,
            Path::of,
            List.of("local path to file/dir")
    );

    public static final DataType TEXT = new RegexDataTypeAdapter<>(
            "text",
            "\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"|'([^'\\\\]*(\\\\.[^'\\\\]*)*)'",
            String.class,
            x -> x,
            List.of("'text'","\"text\"")
    );

    public static final DataType NUMBER = new NumberDataTypeAdapter<>(
            "number",
            Integer.class,
            false,
            false,
            Number::intValue
    );

    public static final DataType BIG_NUMBER = new NumberDataTypeAdapter<>(
            "big-number",
            Long.class,
            false,
            false,
            Number::longValue
    );

    public static final DataType DECIMAL = new NumberDataTypeAdapter<>(
            "decimal",
            BigDecimal.class,
            true,
            true,
            BigDecimal.class::cast
    );

    public static final DataType DATE = new TemporalDataTypeAdapter<>(
            "date", LocalDate.class, true, false, LocalDate::from
    );

    public static final DataType TIME = new TemporalDataTypeAdapter<>(
            "time", LocalTime.class, false, true, LocalTime::from
    );

    public static final DataType DATE_TIME = new TemporalDataTypeAdapter<>(
            "date-time", LocalDateTime.class, true, true, LocalDateTime::from
    );


    @Override
    public Stream<DataType> dataTypes() {
        return Stream.of(
            WORD,
            URL,
            ID,
            FILE,
            TEXT,
            NUMBER,
            BIG_NUMBER,
            DECIMAL,
            DATE,
            TIME,
            DATE_TIME
        );
    }

}
