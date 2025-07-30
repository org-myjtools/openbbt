/**
 * @author Luis Iñesta Gelabert - linesta@iti.es | luiinge@gmail.com
 */
package org.myjtools.openbbt.core.test.contributors;


import java.math.BigDecimal;

import org.junit.jupiter.api.*;
import org.myjtools.openbbt.core.OpenBBTException;

import static org.myjtools.openbbt.core.adapters.CoreDataTypes.DECIMAL;
import static org.myjtools.openbbt.core.adapters.CoreDataTypes.NUMBER;


class TestNumberType {


    @Test
    void testAttemptParseWithWrongValue() {
        Assertions.assertThrowsExactly(
            OpenBBTException.class,
            () -> NUMBER.parse("xxxx"),
            "Error parsing type int using language en: 'xxxxx'"
        );
    }


    @Test
    void testInteger() {
        var type = NUMBER;
        Assertions.assertTrue(type.matcher("12345").matches());
        Assertions.assertEquals(12345, type.parse("12345"));
        Assertions.assertTrue(type.matcher("12,345").matches());
        Assertions.assertEquals(12345, type.parse("12,345"));
        Assertions.assertFalse(type.matcher("12,345.54").matches());
        Assertions.assertFalse(type.matcher("xxxxx").matches());
    }


    @Test
    void testDecimal() {
        var type = DECIMAL;
        Assertions.assertFalse(type.matcher("12345").matches());
        Assertions.assertTrue(type.matcher("12345.0").matches());
        Assertions.assertEquals(BigDecimal.valueOf(12345.0),type.parse("12345.0"));
        Assertions.assertFalse(type.matcher("12,345").matches());
        Assertions.assertTrue(type.matcher("12,345.0").matches());
        Assertions.assertEquals(BigDecimal.valueOf(12345.0), type.parse("12,345.0"));
        Assertions.assertTrue(type.matcher("12,345.54").matches());
        Assertions.assertEquals(BigDecimal.valueOf(12345.54),type.parse("12,345.54"));
        Assertions.assertFalse(type.matcher("xxxxx").matches());
    }

}
