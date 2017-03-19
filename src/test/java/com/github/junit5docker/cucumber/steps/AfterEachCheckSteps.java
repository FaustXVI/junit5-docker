package com.github.junit5docker.cucumber.steps;

import com.github.junit5docker.cucumber.state.Containers;
import cucumber.api.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class AfterEachCheckSteps {

    private Containers containers;

    public AfterEachCheckSteps(Containers containers) {

        this.containers = containers;
    }

    @When("^this container is stopped and removed after usage$")
    public void checkedAndRemoved() {
        assertThat(containers.remainingForTest()).isEmpty();
    }

}
