/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.core.adapters;


import org.myjtools.openbbt.core.DataType;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.util.Patterns;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;
import java.util.regex.Pattern;


public class PeriodDataTypeAdapter extends DataTypeAdapter<Period>
implements DataType {

    private static final Pattern PATTERN = Patterns.of(
        "(?=.*\\d+(?:y|m|d))(?:(\\d+)y)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)d)?"
    );
    public static final String HINT = "<years>y <months>m <days>d";

    public PeriodDataTypeAdapter(String name) {
        super(
            name,
            Period.class,
            PATTERN.pattern(),
            HINT, // Example hint for period format
            periodParser()
        );
    }

    private static Function<String,Period> periodParser() {
        return source -> {
            Period period = Period.ZERO;
            var matcher = PATTERN.matcher(source);
            if (matcher.matches()) {
                   if (matcher.group(1) != null) {
                       period = period.plusYears(Integer.parseInt(matcher.group(1)));
                }
                if (matcher.group(2) != null) {
                    period = period.plusMonths(Integer.parseInt(matcher.group(2)));
                }
                if (matcher.group(3) != null) {
                    period = period.plusDays(Integer.parseInt(matcher.group(3)));
                }
                return period;
            }
            throw new OpenBBTException("Invalid period format: {} , expected: {}", source, HINT);
        };
    }


}
