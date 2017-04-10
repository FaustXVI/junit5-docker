package com.github.junit5docker.cucumber.steps;

import com.github.junit5docker.cucumber.engine.JupiterExecutionListener;
import com.github.junit5docker.cucumber.engine.JupiterTestEngineForTests;
import com.github.junit5docker.cucumber.state.CompiledClass;
import cucumber.api.java.en.When;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ExecutionSteps {

    private final CompiledClass compiledClass;

    private final JupiterTestEngineForTests testEngine;

    public ExecutionSteps(CompiledClass compiledClass, JupiterTestEngineForTests testEngine) {
        this.compiledClass = compiledClass;
        this.testEngine = testEngine;
    }

    @When("^you run your tests? :$")
    public void executeTest() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<JupiterExecutionListener> future = executor.submit(
            () -> testEngine.executeTestsForClass(compiledClass.getCompiledClass())
        );
        try {
            JupiterExecutionListener listener = future.get(5, TimeUnit.MINUTES);
            assertThat(listener.allTestsPassed())
                .overridingErrorMessage("Tests should be green")
                .isTrue();
        } catch (TimeoutException e) {
            fail("Tests should have finished");
        } finally {
            executor.shutdownNow();
        }
    }

}
