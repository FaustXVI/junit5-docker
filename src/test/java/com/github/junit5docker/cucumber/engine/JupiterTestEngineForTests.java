package com.github.junit5docker.cucumber.engine;

import com.github.junit5docker.cucumber.state.Containers;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

import static com.github.junit5docker.cucumber.engine.TestDescriptorForCucumber.createTestDescriptorForCucumber;
import static org.junit.platform.engine.UniqueId.forEngine;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

public class JupiterTestEngineForTests {

    private final JupiterTestEngine engine;

    private final Containers containers;

    private final JupiterExecutionListener eventRecorder;

    public JupiterTestEngineForTests(Containers containers, JupiterExecutionListener eventRecorder) {
        this.containers = containers;
        this.eventRecorder = eventRecorder;
        this.engine = new JupiterTestEngine();
    }

    public JupiterExecutionListener executeTestsForClass(Class<?> testClass) throws Exception {
        return executeTests(request().selectors(selectClass(testClass)).build());
    }

    private JupiterExecutionListener executeTests(LauncherDiscoveryRequest request) throws Exception {
        TestDescriptor testDescriptor = createTestDescriptorForCucumber(
            engine.discover(request, forEngine(engine.getId())), containers);
        eventRecorder.reset();
        engine.execute(new ExecutionRequest(testDescriptor, eventRecorder, request.getConfigurationParameters()));
        return eventRecorder;
    }

}
