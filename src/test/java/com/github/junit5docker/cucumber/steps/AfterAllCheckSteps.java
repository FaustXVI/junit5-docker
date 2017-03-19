package com.github.junit5docker.cucumber.steps;

import com.github.junit5docker.cucumber.state.Containers;
import cucumber.api.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class AfterAllCheckSteps {

    private Containers containers;

    public AfterAllCheckSteps(Containers containers) {

        this.containers = containers;
    }

    @When("^the container is stopped and removed after your tests$")
    public void checkedAndRemoved() {
        assertThat(containers.remaining()).isEmpty();
    }

}
