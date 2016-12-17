package com.github.junit5docker.cucumber;

import cucumber.api.PendingException;
import cucumber.api.java8.En;

public class Steps implements En {
    public Steps() {
        Given("^that you have a test like :$", (String arg1) -> {
            System.err.println(arg1);
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });
    }
}
