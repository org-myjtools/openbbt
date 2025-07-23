/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.api.test.contributors;



import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.myjtools.openbbt.api.contributors.BasicDataTypes.*;

import org.junit.jupiter.api.Test;

class TestDateTypeISO {


    private static final List<Locale> testLocales = List.of(
        Locale.CANADA, Locale.CHINESE, Locale.ENGLISH, Locale.JAPANESE, Locale.FRENCH, Locale.GERMAN
    );

    @Test
    void testISODate() {
        // ISO date should be accepted by any locale
        for (Locale locale : testLocales) {
            assertTrue(DATE.matcher(locale, "2018-05-30").matches());
            assertEquals(DATE.parse(locale, "2018-05-30"),LocalDate.of(2018, 5, 30));
        }
    }


    @Test
    void testISOTime() {
        // ISO time should be accepted by any locale
        for (Locale locale : testLocales) {
            assertTrue(TIME.matcher(locale, "17:35").matches());
            assertEquals(TIME.parse(locale, "17:35"),LocalTime.of(17, 35));
            assertTrue(TIME.matcher(locale, "17:35:29").matches());
            assertEquals(TIME.parse(locale, "17:35:29"),LocalTime.of(17, 35, 29));
            assertTrue(TIME.matcher(locale, "17:35:29.743").matches());
            assertEquals(TIME.parse(locale, "17:35:29.743"),LocalTime.of(17, 35, 29, 743000000));
        }
    }


    @Test
    void testISODateTime() {
        // ISO time should be accepted by any locale
        for (Locale locale : testLocales) {
            assertTrue(DATE_TIME.matcher(locale, "2018-05-30T17:35").matches());
            assertEquals(DATE_TIME.parse(locale, "2018-05-30T17:35"),LocalDateTime.of(2018, 5, 30, 17, 35));
            assertTrue(DATE_TIME.matcher(locale, "2018-05-30T17:35:29").matches());
            assertEquals(DATE_TIME.parse(locale, "2018-05-30T17:35:29"),LocalDateTime.of(2018, 5, 30, 17, 35, 29));
            assertTrue(DATE_TIME.matcher(locale, "2018-05-30T17:35:29.743").matches());
            assertEquals(DATE_TIME.parse(locale, "2018-05-30T17:35:29.743"),LocalDateTime.of(2018, 5, 30, 17, 35, 29, 743000000));
        }
    }

}
