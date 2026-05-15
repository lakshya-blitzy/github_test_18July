package com.testinium.pages;

import com.testinium.utilities.ConfigurationReader;
import com.testinium.utilities.Driver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model (POM) for the Testinium login page.
 *
 * <p>
 * This class is the abstraction layer between
 * {@code com.testinium.step_definitions.LoginSD} (Cucumber step definitions)
 * and the raw Selenium WebDriver API. It encapsulates the locators for the
 * five logical elements on the login page and exposes five semantic action
 * methods that step definitions invoke.
 * </p>
 *
 * <h2>Wiring</h2>
 * <p>
 * Element wiring is performed by Selenium's {@link PageFactory} in the
 * constructor: each {@code @FindBy}-annotated {@link WebElement} field is
 * configured with a lazy proxy that resolves its underlying DOM element on
 * the first interaction (per the {@link PageFactory} lazy-initialization
 * contract). The first argument to
 * {@link PageFactory#initElements(org.openqa.selenium.WebDriver, Object)} is
 * the {@link org.openqa.selenium.WebDriver} obtained from
 * {@link Driver#getDriver()}, which is the {@link ThreadLocal}-managed
 * singleton; this preserves thread-locality under Surefire's
 * {@code <parallel>methods</parallel>} configuration.
 * </p>
 *
 * <h2>Explicit Waits</h2>
 * <p>
 * Every public action method wraps its WebDriver interaction in a freshly
 * constructed {@link WebDriverWait} whose timeout is sourced from
 * {@link ConfigurationReader#get(String) ConfigurationReader.get("explicit_wait")}.
 * This satisfies the project-wide rule that timeouts must be
 * configuration-driven (overridable via {@code -Dexplicit_wait=...} on the
 * Maven command line) and that hardcoded sleep calls are forbidden
 * anywhere in the page-object layer.
 * </p>
 *
 * <h3>Duration Usage Note</h3>
 * <p>
 * The {@link Duration} type is used as a type-safe wrapper for the seconds
 * value before it is passed to the {@link WebDriverWait} constructor. The
 * Selenium 3.141.59 release of the {@code selenium-support} module exposes
 * the {@code WebDriverWait(WebDriver, long)} constructor (timeout in
 * seconds); the {@link Duration}-accepting overload is provided in Selenium
 * 4.x and later. To keep the timeout expression type-safe and
 * forward-compatible, the value is first wrapped in
 * {@link Duration#ofSeconds(long)} and then unwrapped via
 * {@link Duration#getSeconds()} for the constructor call. This pattern
 * documents the semantic unit (seconds, not millis), compiles cleanly
 * against the pinned Selenium 3.141.59 dependency, and trivially upgrades
 * to the {@link Duration}-accepting constructor if the project later
 * migrates to Selenium 4.
 * </p>
 *
 * <h2>Predicate Choice</h2>
 * <ul>
 *   <li>Input fields ({@code usernameInput}, {@code passwordInput}) and
 *       text-bearing message elements ({@code errorMessage},
 *       {@code emptyFieldValidationMessage}) wait on
 *       {@link ExpectedConditions#visibilityOf(WebElement)} &mdash; the
 *       element must be displayed and have non-zero size before interaction
 *       or text retrieval is attempted.</li>
 *   <li>The login button waits on
 *       {@link ExpectedConditions#elementToBeClickable(WebElement)} &mdash;
 *       this composite predicate verifies both visibility and the
 *       {@code enabled} state, preventing the
 *       {@code ElementClickInterceptedException} or
 *       {@code ElementNotInteractableException} that a bare visibility
 *       check could leave open.</li>
 * </ul>
 *
 * <h2>Scope &amp; Constraints</h2>
 * <ul>
 *   <li>Exactly five {@code @FindBy}-annotated {@link WebElement} fields are
 *       declared. No additional locators (forgot-password link, sign-up
 *       link, dashboard link, remember-me checkbox, etc.) are introduced,
 *       per the minimal-change clause governing this delivery.</li>
 *   <li>Exactly five public action methods are exposed; no fluent-API
 *       method chaining, no convenience {@code login(String, String)}
 *       wrapper, and no {@code BasePage} extraction.</li>
 *   <li>No direct WebDriver instantiation; all WebDriver access flows
 *       through {@link Driver#getDriver()}.</li>
 *   <li>No logging-framework fields, no screenshot capture, and no retry
 *       wrappers &mdash; these are intentionally out of scope.</li>
 * </ul>
 *
 * <h2>Locator Strategy</h2>
 * <p>
 * The five field locators use {@code id} or CSS class selectors as
 * recommended defaults for a typical login form. They are intentionally
 * conservative (matching common conventions such as {@code id="username"},
 * {@code id="password"}, {@code id="login-button"},
 * {@code css=".error-message"}, and {@code css=".validation-message"}) and
 * are designed to be substitutable: if the live Testinium login page uses
 * different attributes, the locator strings can be updated in place
 * without changing any consumer code.
 * </p>
 */
public class LoginPage {

    /**
     * Constructs a new {@code LoginPage} instance and wires its
     * {@code @FindBy}-annotated {@link WebElement} fields against the
     * current thread's {@link org.openqa.selenium.WebDriver}.
     *
     * <p>
     * The driver passed to
     * {@link PageFactory#initElements(org.openqa.selenium.WebDriver, Object)}
     * is obtained from {@link Driver#getDriver()}, which provisions or
     * returns the {@link ThreadLocal}-managed instance for the calling
     * thread. The second argument is {@code this} so that field wiring
     * targets the {@link LoginPage} instance being constructed (and not a
     * fresh proxy instance, which would be the behaviour of the
     * deprecated {@code PageFactory.initElements(WebDriver, Class)}
     * overload).
     * </p>
     *
     * <p>
     * Note that {@link PageFactory} uses lazy initialization &mdash;
     * the actual DOM lookup for each field occurs on the field's first
     * interaction, not during this constructor call. This means the
     * constructor itself does not raise
     * {@code NoSuchElementException} even if the page is not yet
     * loaded; element-not-found errors surface at the call site of the
     * first action method instead, after the explicit wait has elapsed.
     * </p>
     */
    public LoginPage() {
        PageFactory.initElements(Driver.getDriver(), this);
    }

    /**
     * Username input field on the Testinium login page. The locator
     * targets the element by its {@code id} attribute, which is the
     * most stable identifier for standard form inputs.
     */
    @FindBy(id = "username")
    private WebElement usernameInput;

    /**
     * Password input field on the Testinium login page. The locator
     * targets the element by its {@code id} attribute, mirroring the
     * {@link #usernameInput} pattern.
     */
    @FindBy(id = "password")
    private WebElement passwordInput;

    /**
     * Login submission button on the Testinium login page. The locator
     * uses a kebab-case {@code id} attribute, which is a common
     * convention for form-control identifiers.
     */
    @FindBy(id = "login-button")
    private WebElement loginButton;

    /**
     * Error message element displayed after an invalid-credentials
     * login attempt. Consumed by the {@code @UPGN-287} scenario in
     * {@code Login.feature} via {@link #getErrorMessage()}.
     */
    @FindBy(css = ".error-message")
    private WebElement errorMessage;

    /**
     * Empty-field validation message element displayed when a required
     * input is left blank at submit time. Expected to render the
     * French-locale text {@code "Veuillez renseigner ce champ."} per
     * the {@code @UPGN-288} scenario in {@code Login.feature}.
     */
    @FindBy(css = ".validation-message")
    private WebElement emptyFieldValidationMessage;

    /**
     * Types the supplied username into the username input field after
     * waiting for the field to become visible.
     *
     * <p>
     * The wait timeout is sourced from
     * {@link ConfigurationReader#get(String) ConfigurationReader.get("explicit_wait")}
     * and parsed as a {@code long} number of seconds. Using
     * {@link Duration#ofSeconds(long)} as an intermediate value keeps
     * the unit explicit at the call site and makes the migration path
     * to Selenium 4's {@code WebDriverWait(WebDriver, Duration)}
     * constructor trivial.
     * </p>
     *
     * @param username the username text to type into the field; passed
     *                 through verbatim to
     *                 {@link WebElement#sendKeys(CharSequence...)}
     */
    public void enterUsername(String username) {
        new WebDriverWait(
                Driver.getDriver(),
                Duration.ofSeconds(Long.parseLong(ConfigurationReader.get("explicit_wait"))).getSeconds()
        ).until(ExpectedConditions.visibilityOf(usernameInput));
        usernameInput.sendKeys(username);
    }

    /**
     * Types the supplied password into the password input field after
     * waiting for the field to become visible.
     *
     * <p>
     * Mirrors {@link #enterUsername(String)} in wait policy and
     * configuration sourcing; differs only in the target element.
     * </p>
     *
     * @param password the password text to type into the field; passed
     *                 through verbatim to
     *                 {@link WebElement#sendKeys(CharSequence...)}
     */
    public void enterPassword(String password) {
        new WebDriverWait(
                Driver.getDriver(),
                Duration.ofSeconds(Long.parseLong(ConfigurationReader.get("explicit_wait"))).getSeconds()
        ).until(ExpectedConditions.visibilityOf(passwordInput));
        passwordInput.sendKeys(password);
    }

    /**
     * Clicks the login submission button after waiting for the button
     * to become clickable.
     *
     * <p>
     * {@link ExpectedConditions#elementToBeClickable(WebElement)} is a
     * composite predicate that verifies both visibility AND the
     * {@code enabled} state of the element, which is essential for
     * button-style controls: a button can be rendered but disabled
     * (e.g., while a form-validation script runs), and clicking such a
     * button would otherwise raise
     * {@code ElementClickInterceptedException} at the WebDriver layer.
     * </p>
     */
    public void clickLogin() {
        new WebDriverWait(
                Driver.getDriver(),
                Duration.ofSeconds(Long.parseLong(ConfigurationReader.get("explicit_wait"))).getSeconds()
        ).until(ExpectedConditions.elementToBeClickable(loginButton));
        loginButton.click();
    }

    /**
     * Returns the rendered text of the error-message element after
     * waiting for the element to become visible.
     *
     * <p>
     * Consumed by the {@code @UPGN-287} scenario's
     * {@code @Then User sees error message} step. The returned value
     * is the {@code .getText()} result of the underlying
     * {@link WebElement}, which is the displayed text content of the
     * element (whitespace-trimmed per Selenium's
     * {@link WebElement#getText()} contract).
     * </p>
     *
     * @return the visible text of the error-message element
     */
    public String getErrorMessage() {
        new WebDriverWait(
                Driver.getDriver(),
                Duration.ofSeconds(Long.parseLong(ConfigurationReader.get("explicit_wait"))).getSeconds()
        ).until(ExpectedConditions.visibilityOf(errorMessage));
        return errorMessage.getText();
    }

    /**
     * Returns the rendered text of the empty-field validation message
     * element after waiting for the element to become visible.
     *
     * <p>
     * Consumed by the {@code @UPGN-288} scenario's
     * {@code @Then User sees "Veuillez renseigner ce champ." message}
     * step. The expected rendered text is the French-locale validation
     * string {@code "Veuillez renseigner ce champ."} as documented in
     * the project README; the consuming step definition is responsible
     * for asserting equality against the expected literal.
     * </p>
     *
     * @return the visible text of the empty-field validation element
     */
    public String getEmptyFieldValidationMessage() {
        new WebDriverWait(
                Driver.getDriver(),
                Duration.ofSeconds(Long.parseLong(ConfigurationReader.get("explicit_wait"))).getSeconds()
        ).until(ExpectedConditions.visibilityOf(emptyFieldValidationMessage));
        return emptyFieldValidationMessage.getText();
    }
}
