package com.github.junit5docker.cucumber.engine;

import com.github.junit5docker.cucumber.state.Containers;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;

class MethodTestDescriptorForCucumber extends TestMethodTestDescriptor {

    private Containers containers;

    MethodTestDescriptorForCucumber(TestMethodTestDescriptor methodTestDescriptor, Containers containers) {
        super(methodTestDescriptor.getUniqueId(),
            methodTestDescriptor.getTestClass(),
            methodTestDescriptor.getTestMethod());
        this.containers = containers;
    }

    @Override
    protected void invokeTestMethod(JupiterEngineExecutionContext context, DynamicTestExecutor dynamicTestExecutor) {
        containers.updateStartedForTest();
        super.invokeTestMethod(context, dynamicTestExecutor);
    }

    @Override
    public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
                                                 DynamicTestExecutor dynamicTestExecutor) throws Exception {
        try {
            return super.execute(context, dynamicTestExecutor);
        } finally {
            containers.updateRemainingsForTest();
        }
    }
}
