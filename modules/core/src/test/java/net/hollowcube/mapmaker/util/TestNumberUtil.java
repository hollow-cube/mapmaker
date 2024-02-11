package net.hollowcube.mapmaker.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestNumberUtil {

    @Test
    public void testConversions() {
        Assertions.assertEquals("1k", NumberUtil.formatCurrency(1001));
        Assertions.assertEquals("1.42k", NumberUtil.formatCurrency(1423));
        Assertions.assertEquals("17", NumberUtil.formatCurrency(17));
        Assertions.assertEquals("14.7k", NumberUtil.formatCurrency(14730));
        Assertions.assertEquals("1k", NumberUtil.formatCurrency(1001));
        Assertions.assertEquals("50m", NumberUtil.formatCurrency(50001000));
    }
}
