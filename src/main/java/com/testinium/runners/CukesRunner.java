package com.testinium.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * JUnit 4 + Cucumber 7.x test runner that serves as the single entry point for
 * the Testinium login-feature test suite.
 *
 * <p>
 * This class has <strong>no body</strong> by design. Cucumber drives all
 * execution through the two type-annotations applied below
 * ({@link RunWith} and {@link CucumberOptions}). The Surefire plugin
 * declared in {@code pom.xml} at lines L17&ndash;L30 discovers this class
 * via the {@code <include>**&#47;CukesRunner*.java</include>} pattern at
 * {@code pom.xml:L27}, instantiates it as a JUnit test class, and JUnit
 * 4 delegates execution to the Cucumber JUnit runner (via
 * {@code @RunWith(Cucumber.class)}), which in turn parses Gherkin feature
 * files under {@code src/main/resources/features/}, binds steps against
 * glue code under {@code com/testinium/step_definitions}, and emits the
 * four documented report artifacts under {@code target/}.
 * </p>
 *
 * <h2>Contract: Surefire Runner Discovery</h2>
 * <p>
 * The class name {@code CukesRunner} matches the Surefire include pattern
 * {@code **&#47;CukesRunner*.java} at {@code pom.xml:L27} (the trailing
 * {@code *} allows zero or more characters after {@code CukesRunner},
 * which means the literal filename {@code CukesRunner.java} is a valid
 * match). Renaming this class to anything else (e.g.,
 * {@code LoginRunner}, {@code TestRunner}, {@code CukesRunnerTest}) would
 * cause Surefire to silently skip the class and no Cucumber scenarios
 * would execute, even though the JUnit/Cucumber wiring would still
 * compile.
 * </p>
 *
 * <h2>Contract: {@code @CucumberOptions} Exactness</h2>
 * <p>
 * The annotation values below replicate the documented contract in
 * {@code README.md} at lines L77&ndash;L88 verbatim, with one explicit
 * exception called out in the AAP: the {@code tags} value is the literal
 * disjunction {@code "@UPGN-286 or @UPGN-287 or @UPGN-288"} that selects
 * the three login scenarios in {@code Login.feature}, replacing the
 * {@code "@LogOut"} placeholder that appears in the README's documentation
 * snippet (the {@code @LogOut} value is illustrative only per AAP
 * &sect;0.1.2). All other values &mdash; the four {@code plugin} entries,
 * the {@code features} path, the {@code glue} path, and the
 * {@code dryRun = false} setting &mdash; match the README contract
 * exactly so that the Jenkins pipeline's
 * {@code fileIncludePattern: '**&#47;*.json'} report aggregator at
 * {@code Jenkins:L15} continues to find {@code target/cucumber.json}
 * without any modification to the {@code Jenkins} file (which AAP
 * &sect;0.7.1 forbids).
 * </p>
 *
 * <h2>Contract: Tag Expression Syntax</h2>
 * <p>
 * The {@code tags} value uses Cucumber 5+ tag-expression syntax with the
 * {@code or} keyword. The deprecated Cucumber 4-and-earlier
 * comma-separated tag syntax would throw an exception in Cucumber 7.x
 * and is therefore avoided. The three tags themselves are reproduced
 * exactly as they appear in {@code README.md} at lines L115, L123, and
 * L131 (uppercase, hyphenated, with the {@code UPGN-} Jira project
 * prefix).
 * </p>
 *
 * <h2>Contract: Empty Class Body</h2>
 * <p>
 * No fields, no methods, no inner classes. Cucumber dispatches execution
 * entirely through the two annotations above; scenario-level lifecycle
 * hooks ({@code @Before}/{@code @After}) belong in the step-definition
 * class {@code com.testinium.step_definitions.LoginSD} (which is
 * discovered via the {@code glue} path at runtime, not via a Java
 * {@code import} statement in this file).
 * </p>
 *
 * <h2>Thread Safety Note</h2>
 * <p>
 * The Surefire configuration at {@code pom.xml:L22-L23} enables
 * {@code <parallel>methods</parallel>} with
 * {@code <useUnlimitedThreads>true</useUnlimitedThreads>}, which means
 * concurrent execution of Cucumber scenario methods inside a single JVM.
 * This runner itself is stateless (empty class body) and therefore
 * trivially thread-safe; the thread-locality contract for the WebDriver
 * session is enforced inside {@code com.testinium.utilities.Driver}
 * via {@code ThreadLocal<WebDriver>}.
 * </p>
 */
// NOTE: pom.xml declares io.cucumber:cucumber-junit twice (versions 7.2.3 at L60-L65
// and 7.3.4 at L76-L80). Per AAP §0.7.5 "Note-But-Do-Not-Fix" discipline, this duplicate
// is acknowledged but not repaired in this delivery. The API surface for Cucumber and
// CucumberOptions is identical across both versions, so the duplicate has no functional
// impact on this runner.
@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "html:target/cucumber-reports.html",
                "json:target/cucumber.json",
                "rerun:target/rerun.txt",
                "me.jvt.cucumber.report.PrettyReports:target/cucumber"
        },
        features = "src/main/resources/features",
        glue = "com/testinium/step_definitions",
        dryRun = false,
        tags = "@UPGN-286 or @UPGN-287 or @UPGN-288"
)
public class CukesRunner {

}
