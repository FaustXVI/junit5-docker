package com.github.junit5docker.cucumber.steps;

import com.github.junit5docker.DockerExtension;
import com.github.junit5docker.cucumber.state.CompiledClass;
import com.github.junit5docker.cucumber.state.Containers;
import cucumber.api.java.en.When;
import org.junit.jupiter.api.extension.ContainerExtensionContext;

public class ExecutionSteps {

    private final Containers containers;

    private final CompiledClass compiledClass;

    public ExecutionSteps(Containers containers, CompiledClass compiledClass) {
        this.containers = containers;
        this.compiledClass = compiledClass;
    }

    @When("^you run your test :$")
    public void executeTest() {
        DockerExtension dockerExtension = new DockerExtension();
        ContainerExtensionContext context = compiledClass.getExtensionContext();
        dockerExtension.beforeAll(context);
        containers.updateStarted();
        dockerExtension.afterAll(context);
        containers.updateRemainings();
    }

}
