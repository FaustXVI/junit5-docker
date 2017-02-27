package com.github.junit5docker.cucumber.steps;

import com.github.junit5docker.cucumber.engine.JupiterExecutionListener;
import com.github.junit5docker.cucumber.engine.JupiterTestEngineForTests;
import com.github.junit5docker.cucumber.state.CompiledClass;
import cucumber.api.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionSteps {

    private final CompiledClass compiledClass;

    private final JupiterTestEngineForTests testEngine;

    public ExecutionSteps(CompiledClass compiledClass, JupiterTestEngineForTests testEngine) {
        this.compiledClass = compiledClass;
        this.testEngine = testEngine;
    }

    @When("^you run your tests? :$")
    public void executeTest() throws Exception {
        JupiterExecutionListener listener = testEngine.executeTestsForClass(compiledClass.getCompiledClass());
        assertThat(listener.allTestsPassed())
            .overridingErrorMessage("Tests should be green")
            .isTrue();
    }

}
