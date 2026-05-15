package com.testinium.utilities;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Thread-safe WebDriver singleton factory for the testinium-qa test harness.
 *
 * <p>
 * This class is the single, authoritative entry point for WebDriver
 * acquisition and lifecycle management across the Cucumber/Selenium test
 * codebase. It is consumed by:
 * </p>
 * <ul>
 *   <li>{@code com.testinium.pages.LoginPage} &mdash; obtains the driver in
 *       its constructor for {@code PageFactory.initElements(...)} wiring and
 *       to construct {@code WebDriverWait} instances.</li>
 *   <li>{@code com.testinium.step_definitions.LoginSD} &mdash; obtains the
 *       driver in {@code @Given}/{@code @When}/{@code @Then} step methods
 *       (e.g., to call {@code driver.get(base_url)}) and calls
 *       {@link #quitDriver()} from the {@code @After} hook.</li>
 * </ul>
 *
 * <h2>Thread-Locality (Non-Negotiable)</h2>
 * <p>
 * The Surefire plugin is configured with {@code <parallel>methods</parallel>}
 * and {@code <useUnlimitedThreads>true</useUnlimitedThreads>} in
 * {@code pom.xml}, which means scenario methods may execute concurrently
 * inside a single JVM. To prevent races on a shared WebDriver state, this
 * class stores its driver reference in a {@link ThreadLocal} so that every
 * thread receives an independent {@link WebDriver} instance. A static
 * {@code WebDriver} field would be incorrect under this configuration and is
 * explicitly forbidden by the architectural rules driving this delivery.
 * </p>
 *
 * <h2>Lazy Provisioning</h2>
 * <p>
 * {@link #getDriver()} provisions a {@link WebDriver} on first call per
 * thread and caches it in the {@link ThreadLocal} pool. Subsequent calls on
 * the same thread return the cached instance without re-running the
 * {@link WebDriverManager} setup step or instantiating a new driver. The
 * browser type ({@code chrome}, {@code firefox}, or {@code edge}) is read
 * from {@link ConfigurationReader#get(String) ConfigurationReader.get("browser")},
 * which in turn honours the system-property precedence rule, so
 * {@code -Dbrowser=firefox} overrides the value committed to
 * {@code configuration.properties}.
 * </p>
 *
 * <h2>Driver Binary Provisioning</h2>
 * <p>
 * The {@link WebDriverManager} library (declared in {@code pom.xml} at
 * version {@code 5.1.0}) is responsible for downloading and configuring the
 * appropriate browser-driver binary for the host platform. This eliminates
 * manual driver-binary management and keeps the host-OS prerequisite to a
 * compatible browser binary only (Chrome, Firefox, or Edge).
 * </p>
 *
 * <h2>Cleanup Contract</h2>
 * <p>
 * {@link #quitDriver()} <strong>must</strong> be invoked at the end of each
 * scenario (typically from a Cucumber {@code @After} hook). It performs two
 * actions in this exact order:
 * </p>
 * <ol>
 *   <li>{@code driverPool.get().quit()} &mdash; closes the browser and
 *       releases all associated WebDriver-server connections.</li>
 *   <li>{@code driverPool.remove()} &mdash; clears the {@link ThreadLocal}
 *       slot so that the executor thread, when reused by Surefire for the
 *       next scenario method, no longer holds a stale {@code WebDriver}
 *       reference. Without this {@code remove()} call, long-lived executor
 *       threads accumulate dead-driver references that block class-loader
 *       unloading and leak browser-process handles.</li>
 * </ol>
 *
 * <h2>Browser Value Normalization</h2>
 * <p>
 * The value returned by {@code ConfigurationReader.get("browser")} is
 * normalized via {@link String#toLowerCase()} before dispatch, so any of the
 * values {@code chrome}, {@code Chrome}, or {@code CHROME} all route to the
 * Chrome path. Unrecognized values produce a {@link RuntimeException} with a
 * descriptive message containing the rejected value, which fails the test
 * fast and surfaces the misconfiguration clearly to the operator.
 * </p>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>The class has a {@code private} constructor and exposes only
 *       {@code public static} accessors; instantiation is intentionally
 *       prevented.</li>
 *   <li>Only Chrome, Firefox, and Edge are supported per the AAP scope.
 *       Safari, Internet Explorer, and Opera are explicitly out of scope.</li>
 *   <li>No headless-mode or window-size customization is performed here;
 *       drivers are instantiated with their default no-arg constructors.</li>
 *   <li>No logging framework is used; failures surface as
 *       {@link RuntimeException} with informative messages.</li>
 * </ul>
 */
public class Driver {

    /**
     * Private constructor &mdash; prevents external instantiation.
     *
     * <p>
     * All access to driver instances is performed through the
     * {@code public static} {@link #getDriver()} and {@link #quitDriver()}
     * methods; this class is intentionally non-instantiable.
     * </p>
     */
    private Driver() {
        // Private constructor to prevent instantiation; all access is through static getDriver()/quitDriver().
    }

    /**
     * Per-thread WebDriver storage. A {@link ThreadLocal} ensures that every
     * test thread spawned by Surefire's parallel-methods configuration
     * receives its own browser session, eliminating races on WebDriver state.
     *
     * <p>
     * Notes on the modifiers:
     * </p>
     * <ul>
     *   <li>{@code private} &mdash; the {@link ThreadLocal} container is an
     *       implementation detail and is never exposed beyond this class.</li>
     *   <li>{@code static} &mdash; one {@link ThreadLocal} per JVM; the
     *       per-thread isolation is built into the {@link ThreadLocal}
     *       mechanism itself.</li>
     *   <li>{@code final} &mdash; the {@link ThreadLocal} reference itself
     *       never changes after class load; only the per-thread value stored
     *       inside it changes over time.</li>
     * </ul>
     *
     * <p>
     * {@link ThreadLocal} is deliberately chosen over
     * {@link InheritableThreadLocal} so that child threads do <em>not</em>
     * inherit driver references &mdash; doing so would cause cross-thread
     * driver sharing under any framework-spawned worker threads.
     * </p>
     */
    private static final ThreadLocal<WebDriver> driverPool = new ThreadLocal<>();

    /**
     * Returns the {@link WebDriver} for the current thread, provisioning a
     * new browser session lazily on first invocation.
     *
     * <p>
     * On first call from a given thread, this method:
     * </p>
     * <ol>
     *   <li>Reads the configured browser via
     *       {@link ConfigurationReader#get(String) ConfigurationReader.get("browser")}.</li>
     *   <li>Normalizes the value with {@link String#toLowerCase()} so that
     *       {@code chrome}, {@code Chrome}, and {@code CHROME} are all
     *       accepted.</li>
     *   <li>Invokes the matching {@link WebDriverManager} factory
     *       ({@link WebDriverManager#chromedriver() chromedriver()},
     *       {@link WebDriverManager#firefoxdriver() firefoxdriver()}, or
     *       {@link WebDriverManager#edgedriver() edgedriver()}) followed by
     *       {@code .setup()} to download and configure the appropriate
     *       driver binary for the host platform.</li>
     *   <li>Instantiates the corresponding Selenium driver
     *       ({@link ChromeDriver}, {@link FirefoxDriver}, or
     *       {@link EdgeDriver}) and stores it in the {@link ThreadLocal}
     *       pool.</li>
     * </ol>
     *
     * <p>
     * Subsequent calls on the same thread return the cached instance without
     * re-running setup or re-instantiating the driver.
     * </p>
     *
     * @return the {@link WebDriver} for the current thread, never {@code null}
     * @throws RuntimeException if the configured browser value is not one of
     *                          {@code chrome}, {@code firefox}, or
     *                          {@code edge}; the message includes the
     *                          rejected value so the operator can identify
     *                          the misconfiguration immediately
     */
    public static WebDriver getDriver() {
        if (driverPool.get() == null) {
            // Normalize to lower-case so that `Chrome`, `CHROME`, and `chrome`
            // all dispatch to the same branch, per the case-insensitive
            // contract documented in the AAP and in
            // configuration.properties.template.
            String browser = ConfigurationReader.get("browser").toLowerCase();
            switch (browser) {
                case "chrome":
                    WebDriverManager.chromedriver().setup();
                    driverPool.set(new ChromeDriver());
                    break;
                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    driverPool.set(new FirefoxDriver());
                    break;
                case "edge":
                    WebDriverManager.edgedriver().setup();
                    driverPool.set(new EdgeDriver());
                    break;
                default:
                    // Fail fast with a clear, actionable message that names
                    // the rejected value, rather than surfacing a downstream
                    // NullPointerException from an absent driver binary.
                    throw new RuntimeException("Unsupported browser: " + browser);
            }
        }
        return driverPool.get();
    }

    /**
     * Tears down the WebDriver for the current thread.
     *
     * <p>
     * If a driver instance exists in the {@link ThreadLocal} pool for the
     * calling thread, this method performs two operations in this strict
     * order:
     * </p>
     * <ol>
     *   <li>{@code driverPool.get().quit()} &mdash; closes the browser and
     *       releases all WebDriver-server connections.</li>
     *   <li>{@code driverPool.remove()} &mdash; clears the per-thread
     *       reference so that the executor thread, when reused by Surefire
     *       for a subsequent scenario method, does not retain a stale
     *       {@link WebDriver} reference. This prevents class-loader memory
     *       leaks and unintended re-use of a quit-ed driver.</li>
     * </ol>
     *
     * <p>
     * If no driver has been provisioned on the calling thread (e.g.,
     * {@code quitDriver()} is invoked twice, or before {@code getDriver()}),
     * this method is a no-op &mdash; it does not throw.
     * </p>
     *
     * <p>
     * <strong>Ordering is significant:</strong> calling {@code remove()}
     * before {@code quit()} would discard the {@link WebDriver} reference
     * before closing the browser, leaking the underlying browser process and
     * its driver-server child.
     * </p>
     */
    public static void quitDriver() {
        if (driverPool.get() != null) {
            driverPool.get().quit();
            driverPool.remove();
        }
    }
}
