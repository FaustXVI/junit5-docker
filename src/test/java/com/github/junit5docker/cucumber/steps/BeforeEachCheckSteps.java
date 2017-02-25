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

}
