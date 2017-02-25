package com.github.junit5docker.cucumber.engine;

import com.github.junit5docker.cucumber.state.Containers;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;

class ClassTestDescriptorForCucumber extends ClassTestDescriptor {

    private final Containers containers;

    ClassTestDescriptorForCucumber(ClassTestDescriptor testDescriptor, Containers containers) {
        super(testDescriptor.getUniqueId(), testDescriptor.getTestClass());
        this.containers = containers;
    }

    @Override
    public JupiterEngineExecutionContext before(JupiterEngineExecutionContext context) throws Exception {
        JupiterEngineExecutionContext result = super.before(context);
        containers.updateStarted();
        return result;
    }

    @Override
    public void after(JupiterEngineExecutionContext context) throws Exception {
        super.after(context);
        containers.updateRemainings();
    }
}
