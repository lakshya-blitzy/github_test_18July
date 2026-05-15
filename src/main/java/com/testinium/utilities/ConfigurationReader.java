package com.testinium.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Externalized-configuration accessor for the testinium-qa test harness.
 *
 * <p>
 * This is the foundational utility class consumed by every other class in the
 * {@code com.testinium} package tree:
 * </p>
 * <ul>
 *   <li>{@code com.testinium.utilities.Driver} &mdash; reads {@code "browser"}
 *       to dispatch to the appropriate WebDriver implementation.</li>
 *   <li>{@code com.testinium.pages.LoginPage} &mdash; reads {@code "explicit_wait"}
 *       to parameterize {@code WebDriverWait} timeouts.</li>
 *   <li>{@code com.testinium.step_definitions.LoginSD} &mdash; reads
 *       {@code "base_url"} to navigate to the system under test.</li>
 * </ul>
 *
 * <h2>Configuration Source</h2>
 * <p>
 * The class loads {@code configuration.properties} from the working directory
 * (project root, <strong>not</strong> the classpath) on first reference via a
 * {@code static} initializer block. The file is intentionally
 * {@code .gitignore}-excluded; operators bootstrap it locally by running
 * {@code cp configuration.properties.template configuration.properties}.
 * </p>
 *
 * <h2>Value-Resolution Precedence</h2>
 * <p>
 * The {@link #get(String)} accessor implements a two-tier resolution strategy:
 * </p>
 * <ol>
 *   <li>{@code System.getProperty(key)} &mdash; allows command-line overrides
 *       such as {@code -Dbrowser=firefox} to take precedence over file values.</li>
 *   <li>The in-memory {@link Properties} instance loaded from
 *       {@code configuration.properties} &mdash; used when no system property
 *       is set for the given key.</li>
 * </ol>
 * <p>
 * If neither source provides a value for the requested key, the accessor
 * fails fast by throwing a {@link RuntimeException} whose message identifies
 * the missing key and the {@code -D} override mechanism.
 * </p>
 *
 * <h2>Fail-Fast Initialization</h2>
 * <p>
 * If {@code configuration.properties} is missing or unreadable at class-load
 * time, the {@code static} initializer raises a {@link RuntimeException}
 * whose message enumerates the four required keys ({@code browser},
 * {@code base_url}, {@code implicit_wait}, {@code explicit_wait}) and
 * instructs the operator how to bootstrap the file from the committed
 * template. The original {@link IOException} is preserved as the cause so
 * that diagnostic information (e.g., {@link java.io.FileNotFoundException})
 * is retained.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The {@link Properties} container extends {@link java.util.Hashtable}, so
 * its {@code getProperty(String)} method is synchronized and therefore safe
 * to invoke from multiple threads concurrently. Because Surefire is
 * configured with {@code <parallel>methods</parallel>}, scenario methods may
 * execute concurrently; this accessor's read-only access pattern is
 * inherently thread-safe.
 * </p>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>The class has a {@code private} constructor and exposes only a
 *       {@code public static} accessor; instantiation is intentionally
 *       prevented.</li>
 *   <li>No environment-variable lookup ({@code System.getenv}) is performed;
 *       per the AAP, the only configuration sources are system properties
 *       and the properties file.</li>
 *   <li>Configuration is read once at class load and never re-read; there is
 *       no file-watching or reload mechanism.</li>
 *   <li>This class has no third-party dependencies &mdash; it relies solely
 *       on the JDK standard library so that it can be compiled and exercised
 *       in isolation.</li>
 * </ul>
 */
public final class ConfigurationReader {

    /**
     * In-memory cache of {@code configuration.properties}. Populated by the
     * {@code static} initializer block at class load and consulted as the
     * second-tier fallback after {@link System#getProperty(String)} by the
     * {@link #get(String)} accessor.
     */
    private static Properties properties = new Properties();

    /*
     * Fail-fast static initializer.
     *
     * Loads `configuration.properties` from the project root using a file-system
     * relative path (NOT a classpath resource), because the file is
     * `.gitignore`-excluded and lives at the working-directory root by design.
     * Any IOException during open or load is translated into a RuntimeException
     * whose message lists all four required keys, so the operator immediately
     * understands what to bootstrap.
     */
    static {
        try (FileInputStream input = new FileInputStream("configuration.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(
                "Configuration file 'configuration.properties' not found or unreadable. " +
                "Please copy 'configuration.properties.template' to 'configuration.properties' " +
                "and set values for the four required keys: browser, base_url, implicit_wait, explicit_wait.",
                e
            );
        }
    }

    /**
     * Private constructor &mdash; prevents external instantiation.
     *
     * <p>
     * All access to configuration values is performed through the
     * {@code public static} {@link #get(String)} accessor; this class is
     * intentionally non-instantiable.
     * </p>
     */
    private ConfigurationReader() {
        // Private constructor to prevent instantiation; all access is through static get(String).
    }

    /**
     * Returns the configuration value for the given key, applying
     * system-property precedence with a fall-back to the loaded properties
     * file.
     *
     * <p>
     * Input validation: the {@code key} argument must be non-{@code null} and
     * must contain at least one non-whitespace character. This guard is
     * required because {@link System#getProperty(String)} throws a raw
     * {@link NullPointerException} when invoked with {@code null} and a raw
     * {@link IllegalArgumentException} when invoked with an empty string.
     * Surfacing those JDK-level exceptions to callers would obscure the
     * actual misconfiguration; instead this method raises a
     * {@link RuntimeException} with a clear, actionable message.
     * </p>
     *
     * <p>
     * Resolution order:
     * </p>
     * <ol>
     *   <li>{@code System.getProperty(key)} &mdash; if it returns a value
     *       that contains at least one non-whitespace character, that value
     *       wins. Command-line overrides such as {@code -Dbrowser=firefox}
     *       therefore take precedence over file values.</li>
     *   <li>{@code properties.getProperty(key)} &mdash; the value loaded from
     *       {@code configuration.properties} is consulted only when the
     *       system property is {@code null} or blank. A blank system
     *       property is treated as "not provided" so that, e.g.,
     *       {@code -Dbrowser=} does not silently override the file value
     *       with an empty string.</li>
     * </ol>
     *
     * <p>
     * If neither source provides a non-blank value, a
     * {@link RuntimeException} is thrown whose message identifies the
     * missing key and explains how to supply it (either by setting it in
     * {@code configuration.properties} or by passing {@code -D<key>=<value>}
     * on the Maven command line). The thrown message names only the missing
     * key; it deliberately does not echo any resolved value to avoid
     * leaking potentially sensitive runtime data into logs.
     * </p>
     *
     * @param key the configuration key to look up (e.g., {@code "browser"},
     *            {@code "base_url"}, {@code "implicit_wait"},
     *            {@code "explicit_wait"})
     * @return the resolved value, guaranteed to be non-{@code null} and to
     *         contain at least one non-whitespace character
     * @throws RuntimeException if {@code key} is {@code null} or blank, or
     *                          if neither the system property nor the
     *                          properties file provides a non-blank value
     *                          for {@code key}
     */
    public static String get(String key) {
        // Validate the key BEFORE delegating to System.getProperty(...) so
        // that null/blank inputs produce a clear, actionable error instead
        // of the raw NullPointerException (for null) or
        // IllegalArgumentException (for "") that the JDK would otherwise
        // surface.
        if (key == null || key.trim().isEmpty()) {
            throw new RuntimeException(
                "Configuration key must not be null or blank. " +
                "Provide a non-empty key such as 'browser', 'base_url', " +
                "'implicit_wait', or 'explicit_wait'."
            );
        }
        // System property takes precedence over file value, but only when
        // it is itself non-blank: a blank system property (e.g. -Dbrowser=)
        // is treated as "not provided" so that the file value can still
        // satisfy the lookup.
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            return systemValue;
        }
        String fileValue = properties.getProperty(key);
        if (fileValue != null && !fileValue.trim().isEmpty()) {
            return fileValue;
        }
        throw new RuntimeException(
            "Required configuration key '" + key + "' is not set or is blank. " +
            "Provide it via -D" + key + "=<value> on the command line " +
            "or set it in 'configuration.properties'."
        );
    }
}
