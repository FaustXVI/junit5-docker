package com.github.junit5docker.cucumber;

import cucumber.api.java8.En;

import static org.assertj.core.api.Assertions.assertThat;

public class AfterAllCheckSteps implements En {

    public AfterAllCheckSteps(Containers containers) {

        When("^the container is stopped and removed after your tests$", () -> {
            assertThat(containers.remainings()).isEmpty();
        });

    }

}
