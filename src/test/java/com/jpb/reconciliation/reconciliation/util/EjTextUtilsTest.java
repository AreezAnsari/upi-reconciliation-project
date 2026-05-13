package com.jpb.reconciliation.reconciliation.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EjTextUtilsTest {

    @Test
    public void testStripLeading() {
        assertEquals("HELLO", EjTextUtils.stripLeading("   HELLO"));
        assertEquals("HELLO", EjTextUtils.stripLeading("\t\tHELLO"));
        assertEquals("", EjTextUtils.stripLeading(null));
    }

    @Test
    public void testNullIfBlank() {
        assertNull(EjTextUtils.nullIfBlank(""));
        assertNull(EjTextUtils.nullIfBlank("   "));
        assertNull(EjTextUtils.nullIfBlank(null));
        assertEquals("hello", EjTextUtils.nullIfBlank("  hello  "));
    }

    @Test
    public void testTruncate() {
        assertEquals("hel...", EjTextUtils.truncate("hello world", 3));
        assertEquals("hi", EjTextUtils.truncate("hi", 10));
        assertNull(EjTextUtils.truncate(null, 5));
    }
}