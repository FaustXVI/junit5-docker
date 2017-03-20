package com.github.junit5docker.cucumber.steps;

import com.github.dockerjava.api.model.Container;
import com.github.junit5docker.cucumber.state.CompiledClass;
import com.github.junit5docker.cucumber.state.Containers;
import cucumber.api.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class BeforeEachCheckSteps {

    private final Containers containers;

    private final CompiledClass compiledClass;

    private String lastContainerStarted;

    public BeforeEachCheckSteps(Containers containers, CompiledClass compiledClass) {
        this.containers = containers;
        this.compiledClass = compiledClass;
    }

    @When("a new container `([^`]*)` is started before running each tests using the version `([^`]*)`")
    public void startedStep(String imageName, String version) {
        assertThat(containers.startedImageNamesPerTest()).contains(imageName + ":" + version);
        assertThat(containers.startedContainersPerTest()).extracting(Container::getId)
            .isNotEqualTo(lastContainerStarted);
        lastContainerStarted = containers.startedContainersPerTest()
            .findFirst()
            .map(Container::getId)
            .orElseThrow(IllegalStateException::new);
    }

    @When("^the port `(\\d+)` is bound to the container's port `(\\d+)` so you can exchange through this port$")
    public void portStep(Integer outerPort, Integer innerPort) {
        assertThat(containers.portMapping()).contains(new Integer[]{outerPort, innerPort});
    }

    @When("^the container is started with the given environment variables$")
    public void environmentStep() {
        assertThat(containers.environment()).contains(compiledClass.environmentAnnotations());
    }

    @When("^the tests are started only after the string `([^`]*)` is found in the container's logs$")
    public void waitForLogStep(String wantedLog) {
        assertThat(compiledClass.waitAnnotations())
            .describedAs("Java code and expectation mismatch on waited log \"%s\"", wantedLog)
            .contains(wantedLog);
        assertThat(containers.logs()
            .anyMatch(s -> s.contains(wantedLog)))
            .describedAs("Logs should contains \"%s\"", wantedLog)
            .isTrue();
    }

}
