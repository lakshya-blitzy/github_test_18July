package com.testinium.step_definitions;

import com.testinium.pages.LoginPage;
import com.testinium.utilities.ConfigurationReader;
import com.testinium.utilities.Driver;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Cucumber step-definition class for the Login feature.
 *
 * <p>
 * This class binds every Gherkin step in
 * {@code src/main/resources/features/Login.feature} to an executable Java
 * method. It is discovered at runtime by the Cucumber engine via the
 * {@code glue = "com/testinium/step_definitions"} declaration in
 * {@code CukesRunner.java}'s {@code @CucumberOptions} annotation, which in
 * turn derives verbatim from {@code README.md}.
 * </p>
 *
 * <h2>Step-Binding Surface</h2>
 * <p>
 * Seven distinct regex-anchored patterns are bound here, covering all three
 * Jira-tagged scenarios in {@code Login.feature}:
 * </p>
 * <ul>
 *   <li>{@code @UPGN-286} (valid login &mdash; positive case)</li>
 *   <li>{@code @UPGN-287} (invalid credentials &mdash; negative case)</li>
 *   <li>{@code @UPGN-288} (empty-field validation &mdash; French-locale
 *       validation message)</li>
 * </ul>
 *
 * <h2>Lifecycle Hooks</h2>
 * <p>
 * Two Cucumber lifecycle hooks are declared (from
 * {@link io.cucumber.java.Before} and {@link io.cucumber.java.After},
 * <strong>not</strong> from {@code org.junit.Before}/{@code org.junit.After}):
 * </p>
 * <ol>
 *   <li>{@link #setUp()} runs before each scenario and lazily instantiates a
 *       fresh {@link LoginPage}. The instantiation happens here (not at field
 *       initialization time) so that the underlying
 *       {@code PageFactory.initElements(Driver.getDriver(), this)} call
 *       executes after Cucumber has established per-scenario context and
 *       after the {@link Driver#getDriver()} ThreadLocal is ready for this
 *       thread.</li>
 *   <li>{@link #tearDown()} runs after each scenario (regardless of
 *       pass/fail) and calls {@link Driver#quitDriver()}, which both closes
 *       the WebDriver session and clears the ThreadLocal slot. This is
 *       non-negotiable under Surefire's {@code <parallel>methods</parallel>}
 *       configuration because reused executor threads would otherwise
 *       accumulate stale driver references.</li>
 * </ol>
 *
 * <h2>Thread-Locality</h2>
 * <p>
 * Cucumber creates a fresh step-definition instance per scenario; the
 * {@link #loginPage} field is an instance field (NOT {@code static}), and
 * all WebDriver access flows through {@link Driver#getDriver()} which is
 * ThreadLocal-managed. There is no shared mutable state in this class.
 * </p>
 *
 * <h2>Configuration-Driven URLs and Timeouts</h2>
 * <p>
 * The base login URL is read via
 * {@link ConfigurationReader#get(String) ConfigurationReader.get("base_url")},
 * NOT hardcoded. This honours the system-property precedence rule
 * (e.g., {@code -Dbase_url=https://staging.testinium.com} overrides the
 * value in {@code configuration.properties}). All explicit-wait timeouts
 * used by the underlying {@link LoginPage} action methods likewise read
 * from {@code ConfigurationReader} (key {@code "explicit_wait"}); this
 * class itself contains no hardcoded {@code Thread.sleep} calls.
 * </p>
 *
 * <h2>Assertion Library</h2>
 * <p>
 * Verification steps use {@link Assert} from JUnit 4
 * (declared in {@code pom.xml} at version {@code 4.13.2}), <strong>not</strong>
 * JUnit 5's {@code org.junit.jupiter.api.Assertions}. The project
 * intentionally uses the JUnit 4 / cucumber-junit adapter pattern.
 * </p>
 */
public class LoginSD {

    /**
     * Per-scenario {@link LoginPage} instance.
     *
     * <p>
     * Declared as an instance field (NOT {@code static}) so that Cucumber's
     * per-scenario instance-creation model guarantees a fresh
     * {@link LoginPage} for every scenario. Initialization is deferred to
     * {@link #setUp()} (and therefore happens AFTER Cucumber establishes
     * per-scenario context) because the {@link LoginPage} constructor
     * eagerly resolves {@link Driver#getDriver()} via
     * {@code PageFactory.initElements(...)}: instantiating it at field-
     * initialization time could race with WebDriver provisioning.
     * </p>
     */
    private LoginPage loginPage;

    /**
     * Cucumber {@code @Before} hook &mdash; runs before every scenario.
     *
     * <p>
     * Instantiates a fresh {@link LoginPage}, which in turn triggers
     * {@link Driver#getDriver()} (provisioning the WebDriver for the
     * current thread on first call) and wires the {@code @FindBy}-annotated
     * fields via Selenium's {@code PageFactory.initElements(...)}. After
     * this hook returns, every step method in this class can safely
     * dereference {@link #loginPage}.
     * </p>
     */
    @Before
    public void setUp() {
        loginPage = new LoginPage();
    }

    /**
     * Step binding: {@code Given User is on the Testinium login page}.
     *
     * <p>
     * Navigates the WebDriver for the current thread to the configured
     * base URL. The URL is sourced from
     * {@link ConfigurationReader#get(String) ConfigurationReader.get("base_url")},
     * which honours the {@code -Dbase_url=...} system-property override.
     * </p>
     *
     * <p>
     * This step appears in the {@code Background:} block of
     * {@code Login.feature} and therefore executes before every scenario.
     * </p>
     */
    @Given("^User is on the Testinium login page$")
    public void user_is_on_the_Testinium_login_page() {
        Driver.getDriver().get(ConfigurationReader.get("base_url"));
    }

    /**
     * Step binding: {@code When User enters "<value>" username}.
     *
     * <p>
     * The regex capture group {@code ([^"]*)} matches the value substituted
     * from the scenario's {@code Examples:} table at the {@code <username>}
     * placeholder (and at the {@code <password>} placeholder in
     * {@code @UPGN-288}, where the feature deliberately reuses that
     * column name in the username position).
     * </p>
     *
     * @param username the username text captured from the Gherkin step;
     *                 passed through to {@link LoginPage#enterUsername(String)},
     *                 which performs the {@code sendKeys} interaction
     *                 behind an explicit visibility wait
     */
    @When("^User enters \"([^\"]*)\" username$")
    public void user_enters_username(String username) {
        loginPage.enterUsername(username);
    }

    /**
     * Step binding: {@code When User enters "<value>" password}.
     *
     * <p>
     * Bound by the {@code @UPGN-286} and {@code @UPGN-287} scenarios; the
     * {@code @UPGN-288} scenario intentionally omits the password step to
     * exercise the empty-field validation flow.
     * </p>
     *
     * @param password the password text captured from the Gherkin step;
     *                 passed through to {@link LoginPage#enterPassword(String)},
     *                 which performs the {@code sendKeys} interaction
     *                 behind an explicit visibility wait
     */
    @When("^User enters \"([^\"]*)\" password$")
    public void user_enters_password(String password) {
        loginPage.enterPassword(password);
    }

    /**
     * Step binding: {@code And User clicks the login button}.
     *
     * <p>
     * Although written with the {@code And} keyword in {@code Login.feature},
     * Cucumber matches step text against any registered annotation
     * regardless of keyword: {@code And} steps inherit semantic context
     * from the previous {@code When}/{@code Then}, but a single
     * {@code @When}-annotated method is sufficient to bind them.
     * </p>
     *
     * <p>
     * Delegates to {@link LoginPage#clickLogin()}, which waits for the
     * button to become clickable (composite visibility + enabled
     * predicate) before performing the click.
     * </p>
     */
    @When("^User clicks the login button$")
    public void user_clicks_the_login_button() {
        loginPage.clickLogin();
    }

    /**
     * Step binding: {@code Then User should see the dashboard}
     * &mdash; positive-case assertion for {@code @UPGN-286}.
     *
     * <p>
     * Verifies that the WebDriver has navigated to an authenticated
     * post-login URL after submitting valid credentials. The assertion is
     * built on top of a {@link WebDriverWait} (whose timeout is sourced
     * from {@link ConfigurationReader#get(String) ConfigurationReader.get("explicit_wait")})
     * so that it deterministically blocks until the dashboard navigation
     * has occurred &mdash; preventing a brittle race where the assertion
     * runs before the browser has finished navigating. The wait expires
     * with a {@code TimeoutException} (and therefore a scenario failure)
     * if the dashboard navigation never happens within the configured
     * window, which is the correct semantic for an unsuccessful login
     * that leaves the user stranded on the login page.
     * </p>
     *
     * <h3>False-Pass Hardening</h3>
     * <p>
     * The previous implementation of this step compared
     * {@code currentUrl != baseUrl} only and was susceptible to two
     * known false-pass cases that this implementation eliminates:
     * </p>
     * <ol>
     *   <li><em>Trailing-slash normalization.</em> Some browsers
     *       normalize {@code https://app.testinium.com} to
     *       {@code https://app.testinium.com/} immediately on load, so a
     *       naive string inequality would report navigation success even
     *       when the URL has not actually changed. Both the wait
     *       predicate and the post-wait assertion strip trailing slashes
     *       before comparison so a {@code "/" }-only change is correctly
     *       treated as <em>no</em> navigation.</li>
     *   <li><em>Failed-login redirect to {@code /login}.</em> A login
     *       failure that redirects the user to an explicit
     *       {@code /login} path (rather than the base URL) would have
     *       false-passed the previous string inequality. The current
     *       implementation additionally asserts that the post-wait URL
     *       does <em>not</em> contain {@code "/login"}, catching this
     *       redirect class outright.</li>
     * </ol>
     *
     * <h3>Why Not a DOM-Element Assertion?</h3>
     * <p>
     * {@link LoginPage} intentionally does not model dashboard elements
     * (its scope is strictly the login form, per the minimal-change
     * discipline that governs this delivery). A URL-contract assertion
     * is therefore the strongest deterministic check available without
     * introducing a dashboard-specific page object beyond the scope of
     * the AAP. If a separate {@code DashboardPage} POM is introduced in
     * a future delivery, this assertion can be extended to additionally
     * wait for a known dashboard element via
     * {@code ExpectedConditions.visibilityOf(...)}.
     * </p>
     */
    @Then("^User should see the dashboard$")
    public void user_should_see_the_dashboard() {
        // Positive-case dashboard assertion. Implementation strategy:
        //   1. Compute a normalized form of the configured base URL (no
        //      trailing slashes) so that a browser-side "/" append does
        //      not appear as a navigation event.
        //   2. Use a WebDriverWait whose timeout is sourced from the
        //      configurable explicit_wait property to deterministically
        //      block until the URL signals a successful login: the URL
        //      must differ (after normalization) from the base URL AND
        //      must not contain "/login" (which would indicate a failed
        //      login redirected back to the login page).
        //   3. After the wait succeeds, re-check the URL against the
        //      same two predicates with explicit Assert calls so the
        //      failure message identifies the offending URL precisely.
        final String baseUrl = ConfigurationReader.get("base_url");
        final long explicitWaitSeconds = Long.parseLong(ConfigurationReader.get("explicit_wait"));

        // Normalize trailing slashes for safe string comparison. The
        // regex strips one or more trailing slashes; baseUrl values such
        // as "https://app.testinium.com" and "https://app.testinium.com/"
        // both reduce to "https://app.testinium.com".
        final String normalizedBaseUrl = baseUrl.replaceAll("/+$", "");

        // Deterministic wait: block until the current URL satisfies the
        // dashboard contract (changed from base AND not on /login). The
        // wait fails fast with a TimeoutException after explicit_wait
        // seconds if the dashboard navigation never occurs, which is the
        // correct fail-fast semantic for a stalled or failed login.
        new WebDriverWait(
                Driver.getDriver(),
                Duration.ofSeconds(explicitWaitSeconds).getSeconds()
        ).until(driver -> {
            String url = driver.getCurrentUrl();
            if (url == null || url.isEmpty()) {
                return false;
            }
            String normalizedUrl = url.replaceAll("/+$", "");
            // Both conditions must hold:
            //   (a) URL is not the base login URL (post-normalization)
            //   (b) URL does not contain "/login" anywhere in its path/query
            return !normalizedUrl.equals(normalizedBaseUrl)
                    && !url.toLowerCase().contains("/login");
        });

        // Re-check the URL after the wait so the assertion failure
        // message captures the exact offending URL for debugging if
        // a regression ever defeats the wait predicate (defense in
        // depth: the wait should already have raised TimeoutException
        // if these assertions would fail, but explicit assertions make
        // the contract visible in the step body itself).
        final String currentUrl = Driver.getDriver().getCurrentUrl();
        final String normalizedCurrentUrl = currentUrl == null
                ? ""
                : currentUrl.replaceAll("/+$", "");

        Assert.assertNotEquals(
                "Expected to navigate away from login page after valid credentials"
                        + " (current URL was: " + currentUrl + ")",
                normalizedBaseUrl,
                normalizedCurrentUrl
        );
        Assert.assertFalse(
                "Post-login URL should not contain '/login'"
                        + " (current URL was: " + currentUrl + ")",
                currentUrl != null && currentUrl.toLowerCase().contains("/login")
        );
    }

    /**
     * Step binding: {@code Then User sees error message}
     * &mdash; negative-case assertion for {@code @UPGN-287}.
     *
     * <p>
     * Verifies that an error-message element is rendered with non-empty
     * text after invalid credentials are submitted. The assertion is
     * intentionally generic (non-null, non-blank) rather than asserting
     * an exact string because the rendered error text may vary by the
     * system-under-test's locale; a strict equality check on a locale-
     * dependent string would produce false failures in i18n test
     * environments. The presence and non-emptiness of the error element
     * itself are the binding contract for this scenario.
     * </p>
     */
    @Then("^User sees error message$")
    public void user_sees_error_message() {
        // Negative-case assertion: after invalid credentials, an error
        // message must be displayed on the page. LoginPage.getErrorMessage()
        // returns the text of the error element (waiting for visibility via
        // explicit wait).
        String errorText = loginPage.getErrorMessage();
        Assert.assertNotNull("Error message element should be visible", errorText);
        Assert.assertFalse(
                "Error message should not be empty",
                errorText.trim().isEmpty()
        );
    }

    /**
     * Step binding: {@code Then User sees "<value>" message}
     * &mdash; empty-field validation assertion for {@code @UPGN-288}.
     *
     * <p>
     * The regex capture group {@code ([^"]*)} captures the expected
     * validation message verbatim from the Gherkin step. For the
     * {@code @UPGN-288} scenario this captures the French-locale string
     * {@code "Veuillez renseigner ce champ."} (exactly, including the
     * trailing period). The assertion compares this expected value to the
     * actual rendered text returned by
     * {@link LoginPage#getEmptyFieldValidationMessage()} using
     * {@link Assert#assertEquals(String, Object, Object)} for an exact
     * string match.
     * </p>
     *
     * @param expectedMessage the expected validation message captured from
     *                        the Gherkin step text
     */
    @Then("^User sees \"([^\"]*)\" message$")
    public void user_sees_message(String expectedMessage) {
        // Empty-field validation case: after submitting with empty field,
        // the browser (or the page) displays the validation message.
        // LoginPage.getEmptyFieldValidationMessage() returns the rendered
        // text after waiting for visibility.
        String actualMessage = loginPage.getEmptyFieldValidationMessage();
        Assert.assertEquals(
                "Empty-field validation message did not match expected",
                expectedMessage,
                actualMessage
        );
    }

    /**
     * Cucumber {@code @After} hook &mdash; runs after every scenario,
     * regardless of pass/fail outcome.
     *
     * <p>
     * Invokes {@link Driver#quitDriver()}, which performs two operations:
     * </p>
     * <ol>
     *   <li>Calls {@code driver.quit()} to close the browser and release
     *       all WebDriver-server connections.</li>
     *   <li>Calls {@code driverPool.remove()} to clear the ThreadLocal
     *       slot, preventing stale-driver leaks across reused Surefire
     *       executor threads.</li>
     * </ol>
     *
     * <p>
     * This hook is non-negotiable: omitting it would leak browser
     * processes and accumulate dead-driver references on long-lived
     * executor threads under Surefire's parallel-methods configuration.
     * </p>
     */
    @After
    public void tearDown() {
        Driver.quitDriver();
    }
}
