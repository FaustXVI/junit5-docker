package com.github.junit5docker.cucumber;

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
