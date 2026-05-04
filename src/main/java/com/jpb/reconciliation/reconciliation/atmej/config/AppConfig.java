package com.jpb.reconciliation.reconciliation.atmej.config;

import java.io.IOException; 
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lightweight, immutable configuration holder.
 *
 * <p>Resolution order (highest priority first):
 * <ol>
 *   <li>JVM system property : {@code -Dkey=value}</li>
 *   <li>Environment variable : {@code ATM_EJ_KEY=value}
 *       (key is uppercased, dots replaced with underscores, prefixed with
 *       {@code ATM_EJ_})</li>
 *   <li>{@code application.properties} on the classpath</li>
 * </ol>
 *
 * <p>This class never logs values - downstream code is responsible for
 * scrubbing secrets from log output.
 */
public final class AppConfig {

    private static final String DEFAULT_RESOURCE = "application.properties";
    private static final String ENV_PREFIX = "ATM_EJ_";

    private final Properties props;

    private AppConfig(Properties props) {
        this.props = props;
    }

    /** Loads configuration from {@code application.properties} on the classpath. */
    public static AppConfig load() {
        return load(DEFAULT_RESOURCE);
    }

    public static AppConfig load(String resourceName) {
        Properties p = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Configuration resource not found: " + resourceName);
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration: " + resourceName, e);
        }
        return new AppConfig(p);
    }

    // ---- accessors -----------------------------------------------------------

    public String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.trim().isEmpty()) return sys;

        String envKey = ENV_PREFIX + key.toUpperCase().replace('.', '_');
        String env = System.getenv(envKey);
        if (env != null && !env.trim().isEmpty()) return env;

        return props.getProperty(key);
    }

    public String getOrDefault(String key, String def) {
        String v = get(key);
        return v != null ? v : def;
    }

    public String getRequired(String key) {
        String v = get(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("Required configuration property is missing: " + key);
        }
        return v;
    }

    public int getInt(String key, int def) {
        String v = get(key);
        if (v == null || v.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid integer for " + key + ": " + v, e);
        }
    }

    public long getLong(String key, long def) {
        String v = get(key);
        if (v == null || v.trim().isEmpty()) return def;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid long for " + key + ": " + v, e);
        }
    }

    public boolean getBoolean(String key, boolean def) {
        String v = get(key);
        if (v == null || v.trim().isEmpty()) return def;
        return Boolean.parseBoolean(v.trim());
    }

    public Path getPath(String key) {
        return Paths.get(getRequired(key));
    }

    public Charset getCharset(String key, Charset def) {
        String v = get(key);
        if (v == null || v.trim().isEmpty()) return def;
        try {
            return Charset.forName(v.trim());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid charset for " + key + ": " + v, e);
        }
    }

    /** Convenience: charset for input files (defaults to ISO-8859-1 - tolerates ESC bytes). */
    public Charset inputCharset() {
        return getCharset("input.charset", StandardCharsets.ISO_8859_1);
    }
}
