package com.github.junit5docker.cucumber;

import cucumber.api.java8.En;

import static org.assertj.core.api.Assertions.assertThat;

public class BeforeAllCheckSteps implements En {

    public BeforeAllCheckSteps(Containers containers, CompiledClass compiledClass) {
        startedStep(containers);
        portStep(containers);
        environmentStep(containers, compiledClass);
        waitForLogStep(containers, compiledClass);
    }

    private void waitForLogStep(Containers containers, CompiledClass compiledClass) {
        When("^the tests are started only after the string \"([^\"]*)\" is found in the container's logs$",
            (String wantedLog) -> {
                assertThat(compiledClass.waitAnnotations())
                    .overridingErrorMessage("Java code and expectation mismatch on waited log {}", wantedLog)
                    .contains(wantedLog);
                assertThat(containers.logs()
                    .anyMatch(s -> s.contains(wantedLog)))
                    .overridingErrorMessage("Logs should contains {}", wantedLog)
                    .isTrue();
            });
    }

    private void environmentStep(Containers containers, CompiledClass compiledClass) {
        When("^the container is started with the given environment variables$", () -> {
            assertThat(containers.environment()).contains(compiledClass.environmentAnnotations());
        });
    }

    private void portStep(Containers containers) {
        When("^the port (\\d+) is bound to the container's port (\\d+) so you can exchange through this port$",
            (Integer outerPort, Integer innerPort) -> {
                assertThat(containers.portMapping()).contains(new Integer[]{outerPort, innerPort});
            });
    }

    private void startedStep(Containers containers) {
        When("^the container \"([^\"]*)\" is started before running your tests using the version \"([^\"]*)\"$",
            (String containerName, String version) -> {
                assertThat(containers.startedImageNames()).contains(containerName + ":" + version);
            });
    }

}
