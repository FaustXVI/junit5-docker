package com.github.junit5docker.cucumber.steps;

import com.github.junit5docker.cucumber.state.Containers;
import cucumber.api.java.After;

public class AfterSteps {

    private Containers containers;

    public AfterSteps(Containers containers) {
        this.containers = containers;
    }

    @After
    public void verifyContainersAreCleared() {
        containers.verifyAllClean();
    }

}
