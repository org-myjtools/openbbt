/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.core.adapters;


import org.myjtools.openbbt.core.DataType;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.util.Patterns;

import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;
import java.util.regex.Pattern;


public class DurationDataTypeAdapter extends DataTypeAdapter<Duration>
implements DataType {

    private static final Pattern PATTERN = Patterns.of("((\\d+)h)?\\s*((\\d+)m)?\\s*((\\d+)s)?\\s*((\\d)+ms)?");
    private static final String HINT = "<hours>h <minutes>m <seconds>s <milliseconds>ms";

    public DurationDataTypeAdapter(String name) {
        super(
            name,
            Duration.class,
            PATTERN.pattern(),
                HINT, // Example hint for period format
            durationParser()
        );
    }

    private static Function<String,Duration> durationParser() {
        return source -> {
            Duration duration = Duration.ZERO;
            var matcher = PATTERN.matcher(source);
            if (matcher.matches()) {
                if (matcher.group(2) != null) {
                    duration = duration.plusHours(Integer.parseInt(matcher.group(2)));
                }
                if (matcher.group(4) != null) {
                    duration = duration.plusMinutes(Integer.parseInt(matcher.group(4)));
                }
                if (matcher.group(6) != null) {
                    duration = duration.plusSeconds(Integer.parseInt(matcher.group(6)));
                }
                if (matcher.group(8) != null) {
                    duration = duration.plusMillis(Integer.parseInt(matcher.group(8)));
                }
                return duration;
            }
            throw new OpenBBTException("Invalid duration format: {}, expected: {}", source, HINT);
        };
    }


}
