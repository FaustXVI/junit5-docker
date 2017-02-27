package com.github.junit5docker.cucumber;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "target/test-classes/documentation",
    plugin = {"pretty"},
    strict = true)
public class CucumberTestRunner {

}
