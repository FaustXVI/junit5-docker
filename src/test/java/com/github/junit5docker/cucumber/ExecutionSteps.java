package com.github.junit5docker.cucumber;

import com.github.junit5docker.DockerExtension;
import cucumber.api.java8.En;
import org.junit.jupiter.api.extension.ContainerExtensionContext;

public class ExecutionSteps implements En {

    public ExecutionSteps(Containers containers, CompiledClass compiledClass) {

        When("^you run your test :$", () -> {
            DockerExtension dockerExtension = new DockerExtension();
            ContainerExtensionContext context = compiledClass.getExtensionContext();
            dockerExtension.beforeAll(context);
            containers.updateStarted();
            dockerExtension.afterAll(context);
            containers.updateRemainings();
        });

    }

}
