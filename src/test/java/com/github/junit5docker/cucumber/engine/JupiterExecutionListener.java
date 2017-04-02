package com.github.junit5docker.cucumber.engine;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;

import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;

public class JupiterExecutionListener implements EngineExecutionListener {

    private boolean allTestsPassed = true;

    @Override
    public void dynamicTestRegistered(TestDescriptor testDescriptor) {

    }

    @Override
    public void executionSkipped(TestDescriptor testDescriptor, String s) {
        allTestsPassed = false;
    }

    @Override
    public void executionStarted(TestDescriptor testDescriptor) {
    }

    @Override
    public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
        allTestsPassed = allTestsPassed && testExecutionResult.getStatus() == SUCCESSFUL;
    }

    @Override
    public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry reportEntry) {

    }

    public boolean allTestsPassed() {
        return allTestsPassed;
    }

    public void reset() {
        allTestsPassed = true;
    }
}
