package com.github.junit5docker.cucumber.steps;

import com.github.junit5docker.cucumber.state.CompiledClass;
import com.github.junit5docker.cucumber.state.Containers;
import cucumber.api.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class BeforeAllCheckSteps {

    private final Containers containers;

    private final CompiledClass compiledClass;

    public BeforeAllCheckSteps(Containers containers, CompiledClass compiledClass) {
        this.containers = containers;
        this.compiledClass = compiledClass;
    }

    @When("^the tests are started only after the string `([^`]*)` is found in the container's logs$")
    public void waitForLogStep(String wantedLog) {
        assertThat(compiledClass.waitAnnotations())
                .overridingErrorMessage("Java code and expectation mismatch on waited log {}", wantedLog)
                .contains(wantedLog);
        assertThat(containers.logs()
                .anyMatch(s -> s.contains(wantedLog)))
                .overridingErrorMessage("Logs should contains {}", wantedLog)
                .isTrue();
    }

    @When("^the container is started with the given environment variables$")
    public void environmentStep() {
        assertThat(containers.environment()).contains(compiledClass.environmentAnnotations());
    }

    @When("^the port `(\\d+)` is bound to the container's port `(\\d+)` so you can exchange through this port$")
    public void portStep(Integer outerPort, Integer innerPort) {
        assertThat(containers.portMapping()).contains(new Integer[]{outerPort, innerPort});
    }

    @When("^the container `([^`]*)` is started before running your tests using the version `([^`]*)`$")
    public void startedStep(String containerName, String version) {
        assertThat(containers.startedImageNames()).contains(containerName + ":" + version);
    }

}
