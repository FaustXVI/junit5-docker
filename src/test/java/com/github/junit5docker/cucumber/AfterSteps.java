package com.github.junit5docker.cucumber;

import cucumber.api.java8.En;

public class AfterSteps implements En {

    public AfterSteps(Containers containers) {

        After(containers::verifyAllClean);

    }

}
