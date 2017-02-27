package com.github.junit5docker.cucumber.engine;

import com.github.junit5docker.cucumber.state.Containers;
import org.junit.jupiter.engine.descriptor.MethodTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;

class MethodTestDescriptorForCucumber extends MethodTestDescriptor {

    private Containers containers;

    MethodTestDescriptorForCucumber(MethodTestDescriptor methodTestDescriptor, Containers containers) {
        super(methodTestDescriptor.getUniqueId(),
            methodTestDescriptor.getTestClass(),
            methodTestDescriptor.getTestMethod());
        this.containers = containers;
    }

    @Override
    protected void invokeTestMethod(JupiterEngineExecutionContext context) {
        containers.updateStartedForTest();
        super.invokeTestMethod(context);
        containers.updateRemainingsForTest();
    }
}
