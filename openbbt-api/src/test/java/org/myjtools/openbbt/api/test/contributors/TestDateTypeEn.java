/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.api.test.contributors;


import java.time.*;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import static org.myjtools.openbbt.api.contributors.BasicDataTypes.*;

import org.junit.jupiter.api.Test;


class TestDateTypeEn {

    private static final Locale LOCALE = Locale.ENGLISH;


    @Test
    void testLocalizedDate1() {
        assertTrue(DATE.matcher(LOCALE, "5/30/18").matches());
        assertEquals(
            LocalDate.of(2018, 5, 30),
            DATE.parse(LOCALE, "5/30/18")
        );
    }


    @Test
    void testLocalizedDate2() {
        assertTrue(DATE.matcher(LOCALE, "Jan 30, 2018").matches());
        assertEquals(
            LocalDate.of(2018, 1, 30),
            DATE.parse(LOCALE, "Jan 30, 2018")
        );
    }


    @Test
    void testLocalizedDate3() {
        assertTrue(DATE.matcher(LOCALE, "January 30, 2018").matches());
        assertEquals(
            LocalDate.of(2018, 1, 30),
            DATE.parse(LOCALE, "January 30, 2018")
        );
    }


    @Test
    void testLocalizedDate4() {
        assertTrue(DATE.matcher(LOCALE, "Tuesday, January 30, 2018").matches());
        assertEquals(
            LocalDate.of(2018, 1, 30),
            DATE.parse(LOCALE, "Tuesday, January 30, 2018")
        );
    }


    @Test
    void testLocalizedDate5() {
        assertFalse(DATE.matcher(LOCALE, "5999/30/18").matches());
    }


    @Test
    void testLocalizedTime1() {
        assertTrue(TIME.matcher(LOCALE, "5:35 PM").matches());
        assertEquals(
            LocalTime.of(17, 35),
            TIME.parse(LOCALE, "5:35 PM")
        );
    }


    @Test
    void testLocalizedTime2() {
        assertTrue(TIME.matcher(LOCALE, "11:35 PM").matches());
        assertEquals(
            LocalTime.of(23, 35),
            TIME.parse(LOCALE, "11:35 PM")
        );
    }


    @Test
    void testLocalizedTime3() {
        assertFalse(TIME.matcher(LOCALE, "555:66").matches());
    }


    @Test
    void testLocalizedDateTime1() {
        assertTrue(DATE_TIME.matcher(LOCALE, "5/30/18, 5:35 PM").matches());
        assertEquals(
            LocalDateTime.of(2018, 5, 30, 17, 35),
            DATE_TIME.parse(LOCALE, "5/30/18, 5:35 PM")
        );
    }


    @Test
    void testLocalizedDateTime2() {
        assertTrue(DATE_TIME.matcher(LOCALE, "Jan 30, 2018, 5:35 PM").matches());
        assertEquals(
            LocalDateTime.of(2018, 1, 30, 17, 35),
            DATE_TIME.parse(LOCALE, "Jan 30, 2018, 5:35 PM")
        );
    }


    @Test
    void testLocalizedDateTime3() {
        assertTrue(DATE_TIME.matcher(LOCALE, "January 30, 2018, 5:35 PM").matches());
        assertEquals(
            LocalDateTime.of(2018, 1, 30, 17, 35),
            DATE_TIME.parse(LOCALE, "January 30, 2018, 5:35 PM")
        );
    }


    @Test
    void testLocalizedDateTime4() {
        assertTrue(DATE_TIME.matcher(LOCALE, "January 30, 2018, 5:35 PM").matches());
        assertEquals(
            LocalDateTime.of(2018, 1, 30, 17, 35),
            DATE_TIME.parse(LOCALE, "Tuesday, January 30, 2018, 5:35 PM")
        );
    }


    @Test
    void testLocalizedDateTime5() {
        assertFalse(DATE_TIME.matcher(LOCALE, "5999/30/18 555:66").matches());
    }

}
