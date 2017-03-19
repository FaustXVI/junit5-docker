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

    @When("^the container `([^`]*)` is started before running your tests using the version `([^`]*)`$")
    public void startedStep(String containerName, String version) {
        assertThat(containers.startedImageNames()).contains(containerName + ":" + version);
    }

}
