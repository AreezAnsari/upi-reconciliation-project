package com.jpb.reconciliation.reconciliation.util;
/**
 * Tiny collection of string helpers shared across parser and reader.
 *
 * <p>Each method is null-tolerant and never throws. Kept separate so reader
 * and parser can both use them without one depending on the other.
 */
public final class EjTextUtils {

    private EjTextUtils() {}

    /**
     * Strip leading tab and space characters from {@code s}. Used only for
     * marker matching - the original line is preserved verbatim in the
     * emitted block so the audit copy is byte-faithful.
     */
    public static String stripLeading(String s) {
        if (s == null) return "";
        int i = 0;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c != '\t' && c != ' ') break;
            i++;
        }
        return i == 0 ? s : s.substring(i);
    }

    /** Trim {@code s} and return null if blank. */
    public static String nullIfBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /** Truncate {@code s} to at most {@code max} chars, suffixing "..." if cut. */
    public static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
    
    /** Java-8-safe replacement for String#isBlank() */
    public static boolean isBlank(String s) {
        if (s == null) return true;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) return false;
        }
        return true;
    }
}
