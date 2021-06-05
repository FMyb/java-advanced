package info.kgeorgiy.ja.ilyin.bank;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

public class RunTests {
    public static void main(final String[] args) {
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(BankTests.class))
                .build();
        final Launcher launcher = LauncherFactory.create();
        final TestPlan testPlan = launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(testPlan);
        final TestExecutionSummary summary = listener.getSummary();
        try {
            Thread.sleep(2500);
        } catch (final InterruptedException ignored) {
        }
        summary.printTo(new PrintWriter(System.out));
        System.exit(summary.getFailures().isEmpty() ? 0 : 1);
    }
}