package com.jpb.reconciliation.reconciliation.atmej.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TextUtilsTest {

    @Test
    public void testStripLeading() {
        assertEquals("HELLO", TextUtils.stripLeading("   HELLO"));
        assertEquals("HELLO", TextUtils.stripLeading("\t\tHELLO"));
        assertEquals("", TextUtils.stripLeading(null));
    }

    @Test
    public void testNullIfBlank() {
        assertNull(TextUtils.nullIfBlank(""));
        assertNull(TextUtils.nullIfBlank("   "));
        assertNull(TextUtils.nullIfBlank(null));
        assertEquals("hello", TextUtils.nullIfBlank("  hello  "));
    }

    @Test
    public void testTruncate() {
        assertEquals("hel...", TextUtils.truncate("hello world", 3));
        assertEquals("hi", TextUtils.truncate("hi", 10));
        assertNull(TextUtils.truncate(null, 5));
    }
}