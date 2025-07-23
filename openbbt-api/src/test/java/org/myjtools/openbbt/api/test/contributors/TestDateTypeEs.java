/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.api.test.contributors;



import java.time.*;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import static org.myjtools.openbbt.api.contributors.BasicDataTypes.*;

import org.junit.jupiter.api.Test;


class TestDateTypeEs {

    private static final Locale LOCALE = Locale.forLanguageTag("es");


    @Test
    void testLocalizedDate1() {
        assertTrue(DATE.matcher(LOCALE, "30/05/18").matches());
        assertEquals(
            LocalDate.of(2018, 5, 30),
            DATE.parse(LOCALE, "30/05/18")
        );
    }


    @Test
    void testLocalizedDate2() {
        assertTrue(DATE.matcher(LOCALE, "30 de mayo de 2018").matches());
        assertEquals(
            LocalDate.of(2018, 5, 30),
            DATE.parse(LOCALE, "30 de mayo de 2018")
        );
    }


    @Test
    void testLocalizedDate3() {
        assertTrue(DATE.matcher(LOCALE, "Miércoles, 30 de mayo de 2018").matches());
        assertEquals(
            LocalDate.of(2018, 5, 30),
            DATE.parse(LOCALE, "Miércoles, 30 de mayo de 2018")
        );
    }


    @Test
    void testLocalizedDate4() {
        assertFalse(DATE.matcher(LOCALE, "5630/18").matches());
    }


    @Test
    void testLocalizedTime1() {
        assertTrue(TIME.matcher(LOCALE, "17:35").matches());
        assertEquals(
            LocalTime.of(17, 35),
            TIME.parse(LOCALE, "17:35")
        );
    }


    @Test
    void testLocalizedTime2() {
        assertTrue(TIME.matcher(LOCALE, "5:35").matches());
        assertEquals(
            LocalTime.of(5, 35),
            TIME.parse(LOCALE, "5:35")
        );
    }


    @Test
    void testLocalizedTime3() {
        assertFalse(TIME.matcher(LOCALE, "555:66").matches());
    }


    @Test
    void testLocalizedDateTime1() {
        assertTrue(DATE_TIME.matcher(LOCALE, "30/05/18 17:35").matches());
        assertEquals(
            LocalDateTime.of(2018, 5, 30, 17, 35),
            DATE_TIME.parse(LOCALE, "30/05/18 17:35")
        );
    }


    @Test
    void testLocalizedDateTime2() {
        assertTrue(DATE_TIME.matcher(LOCALE, "30 de Mayo de 2018 17:35").matches());
        assertEquals(
            LocalDateTime.of(2018, 5, 30, 17, 35),
            DATE_TIME.parse(LOCALE, "30 de Mayo de 2018 17:35")
        );
    }


}
