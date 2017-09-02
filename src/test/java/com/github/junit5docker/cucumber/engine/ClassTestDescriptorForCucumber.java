package com.github.junit5docker.cucumber.engine;

import com.github.junit5docker.cucumber.state.Containers;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;

class ClassTestDescriptorForCucumber extends ClassTestDescriptor {

    private final Containers containers;

    private final ClassTestDescriptor testDescriptor;

    ClassTestDescriptorForCucumber(ClassTestDescriptor testDescriptor, Containers containers) {
        super(testDescriptor.getUniqueId(), testDescriptor.getTestClass());
        this.testDescriptor = testDescriptor;
        this.containers = containers;
    }

    @Override
    public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
        return testDescriptor.prepare(context);
    }

    @Override
    public JupiterEngineExecutionContext before(JupiterEngineExecutionContext context) throws Exception {
        JupiterEngineExecutionContext result = testDescriptor.before(context);
        containers.updateStarted();
        return result;
    }

    @Override
    public void after(JupiterEngineExecutionContext context) throws Exception {
        testDescriptor.after(context);
        containers.updateRemainings();
    }
}
